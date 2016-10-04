/*
 * Copyright (c) 2010-2015 Pivotal Software, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */

package com.pivotal.gemfire.tools.pulse.internal.data;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.pivotal.gemfire.tools.pulse.internal.log.PulseLogWriter;

/**
 * Class Cluster This class is the Data Model for the data used for the Pulse
 * Web UI.
 *
 * @since version 7.0.Beta 2012-09-23
 */
public class Cluster extends Thread {
  private static final int POLL_INTERVAL = 5000;
  public static final int MAX_SAMPLE_SIZE = 180;
  public static final int ALERTS_MAX_SIZE = 1000;
  public static final int PAGE_ALERTS_MAX_SIZE = 100;

  private final PulseLogWriter LOGGER = PulseLogWriter.getLogger();
  private final ResourceBundle resourceBundle = Repository.get()
      .getResourceBundle();

  private String jmxUserName;
  private String jmxUserPassword;
  private String serverName;
  private String port;
  private int stale = 0;
  private float loadPerSec;

  // start: fields defined in System MBean
  private IClusterUpdater updater = null;
  private DataBrowser dataBrowser = null;
  private int memberCount;
  private long clientConnectionCount;
  private int locatorCount;
  private int totalRegionCount;
  private Long totalHeapSize = 0L;
  private Long totalRegionEntryCount;
  private int currentQueryCount;
  private Long totalBytesOnDisk;
  private float diskReadsRate;
  private float diskWritesRate;
  private float writePerSec;
  private float readPerSec;
  private float queriesPerSec;
  private int avgDiskStorage;
  private int avgDiskWritesRate;
  private int runningFunctionCount;
  private Long registeredCQCount;
  private int subscriptionCount;
  private int serverCount;
  private int leadCount;
  private int txnCommittedCount;
  private int txnRollbackCount;
  private Long usedHeapSize = 0L;
  private Long garbageCollectionCount = 0L;
  private int clusterId;
  private int notificationPageNumber = 1;
  private boolean connectedFlag;
  private String connectionErrorMsg = "";

  private Set<String> deletedMembers = new HashSet<String>();

  private Map<String, List<Cluster.Member>> physicalToMember = new ConcurrentHashMap<String, List<Cluster.Member>>();

  private Map<String, Cluster.Member> membersHMap = new ConcurrentHashMap<String, Cluster.Member>();

  private Set<String> deletedRegions = new HashSet<String>();

  private Map<String, Cluster.Region> clusterRegionMap = new ConcurrentHashMap<String, Cluster.Region>();
  private List<Cluster.Alert> alertsList = new CopyOnWriteArrayList<Cluster.Alert>();

  private CircularFifoBuffer totalBytesOnDiskTrend = new CircularFifoBuffer(
      MAX_SAMPLE_SIZE);
  private CircularFifoBuffer throughoutWritesTrend = new CircularFifoBuffer(
      MAX_SAMPLE_SIZE);
  private CircularFifoBuffer throughoutReadsTrend = new CircularFifoBuffer(
      MAX_SAMPLE_SIZE);
  private CircularFifoBuffer writePerSecTrend = new CircularFifoBuffer(
      MAX_SAMPLE_SIZE);
  private CircularFifoBuffer readPerSecTrend = new CircularFifoBuffer(
      MAX_SAMPLE_SIZE);
  private CircularFifoBuffer queriesPerSecTrend = new CircularFifoBuffer(
      MAX_SAMPLE_SIZE);
  private CircularFifoBuffer memoryUsageTrend = new CircularFifoBuffer(
      MAX_SAMPLE_SIZE);
  private CircularFifoBuffer garbageCollectionTrend = new CircularFifoBuffer(
      MAX_SAMPLE_SIZE);

  private Map<String, Boolean> wanInformation = new ConcurrentHashMap<String, Boolean>();
  private Map<String, Cluster.Statement> clusterStatementMap = new ConcurrentHashMap<String, Cluster.Statement>();

  public static final int CLUSTER_STAT_TOTAL_BYTES_ON_DISK = 0;
  public static final int CLUSTER_STAT_THROUGHPUT_WRITES = 1;
  public static final int CLUSTER_STAT_THROUGHPUT_READS = 2;
  public static final int CLUSTER_STAT_WRITES_PER_SECOND = 3;
  public static final int CLUSTER_STAT_READ_PER_SECOND = 4;
  public static final int CLUSTER_STAT_QUERIES_PER_SECOND = 5;
  public static final int CLUSTER_STAT_MEMORY_USAGE = 6;
  public static final int CLUSTER_STAT_GARBAGE_COLLECTION = 7;

  // end: fields defined in System MBean

  // used for updating member's client data
  public static long LAST_UPDATE_TIME = 0;

  public int getStaleStatus() {
    return this.stale;
  }

  private boolean stopUpdates = false;

  private static final int MAX_HOSTS = 40;
  private final List<String> hostNames = new ArrayList<String>();

  public Object[] getStatisticTrend(int trendId) {

    Object[] returnArray = null;
    switch (trendId) {
    case CLUSTER_STAT_TOTAL_BYTES_ON_DISK:
      synchronized (this.totalBytesOnDiskTrend) {
        returnArray = this.totalBytesOnDiskTrend.toArray();
      }

      break;

    case CLUSTER_STAT_THROUGHPUT_READS:
      synchronized (this.throughoutReadsTrend) {
        returnArray = this.throughoutReadsTrend.toArray();
      }
      break;

    case CLUSTER_STAT_THROUGHPUT_WRITES:
      synchronized (this.throughoutWritesTrend) {
        returnArray = this.throughoutWritesTrend.toArray();
      }
      break;

    case CLUSTER_STAT_WRITES_PER_SECOND:
      synchronized (this.writePerSecTrend) {
        returnArray = this.writePerSecTrend.toArray();
      }
      break;

    case CLUSTER_STAT_READ_PER_SECOND:
      synchronized (this.readPerSecTrend) {
        returnArray = this.readPerSecTrend.toArray();
      }
      break;

    case CLUSTER_STAT_QUERIES_PER_SECOND:
      synchronized (this.queriesPerSecTrend) {
        returnArray = this.queriesPerSecTrend.toArray();
      }
      break;

    case CLUSTER_STAT_MEMORY_USAGE:
      synchronized (this.memoryUsageTrend) {
        returnArray = this.memoryUsageTrend.toArray();
      }
      break;

    case CLUSTER_STAT_GARBAGE_COLLECTION:
      synchronized (this.garbageCollectionTrend) {
        returnArray = this.garbageCollectionTrend.toArray();
      }
      break;

    }

    return returnArray;
  }

  /**
   * Member Inner Class
   */
  public static class Member {

    // start: fields defined in MBean
    private boolean manager;
    private int totalRegionCount;
    private String host;
    private long currentHeapSize;
    private long maxHeapSize;
    private int avgHeapUsage;
    private long OffHeapFreeSize;
    private long OffHeapUsedSize;
    private long totalBytesOnDisk;
    private String memberPort;

    private Float cpuUsage = 0.0f;
    private Float hostCpuUsage = 0.0f;
    private Long uptime;
    private String name;
    private float getsRate;
    private float putsRate;
    private boolean isCache;
    private boolean isGateway;
    private boolean isLocator;
    private boolean isServer;
    private boolean isLead;
    private Double loadAverage;
    private int numThreads;
    private Long totalFileDescriptorOpen;
    private Long garbageCollectionCount = 0L;
    private float throughputWrites;
    private float throughputReads;
    private long totalDiskUsage;
    private String queueBacklog;
    private String id;
    private long numSnappyDataClients = 0;

    private CircularFifoBuffer cpuUsageSamples = new CircularFifoBuffer(
        MAX_SAMPLE_SIZE);
    private CircularFifoBuffer heapUsageSamples = new CircularFifoBuffer(
        MAX_SAMPLE_SIZE);
    private Map<String, Cluster.Region> memberRegions = new ConcurrentHashMap<String, Cluster.Region>();
    private Map<String, Cluster.Client> memberClientsHMap = new ConcurrentHashMap<String, Cluster.Client>();
    private CircularFifoBuffer totalBytesOnDiskSamples = new CircularFifoBuffer(
        MAX_SAMPLE_SIZE);
    private CircularFifoBuffer getsPerSecond = new CircularFifoBuffer(
        MAX_SAMPLE_SIZE);
    private CircularFifoBuffer putsPerSecond = new CircularFifoBuffer(
        MAX_SAMPLE_SIZE);
    private CircularFifoBuffer throughputWritesTrend = new CircularFifoBuffer(
        MAX_SAMPLE_SIZE);
    private CircularFifoBuffer throughputReadsTrend = new CircularFifoBuffer(
        MAX_SAMPLE_SIZE);
    private CircularFifoBuffer garbageCollectionSamples = new CircularFifoBuffer(
        MAX_SAMPLE_SIZE);
    private Cluster.GatewayReceiver gatewayReceiver = null;
    private List<Cluster.GatewaySender> gatewaySenderList = new CopyOnWriteArrayList<Cluster.GatewaySender>();
    // end: fields defined in MBean

    public static final int MEMBER_STAT_GARBAGE_COLLECTION = 0;
    public static final int MEMBER_STAT_HEAP_USAGE_SAMPLE = 1;
    public static final int MEMBER_STAT_CPU_USAGE_SAMPLE = 2;
    public static final int MEMBER_STAT_GETS_PER_SECOND = 3;
    public static final int MEMBER_STAT_PUTS_PER_SECOND = 4;
    public static final int MEMBER_STAT_THROUGHPUT_WRITES = 5;
    public static final int MEMBER_STAT_THROUGHPUT_READS = 6;

    public Cluster.Region[] getMemberRegionsList() {
      Cluster.Region[] memberReg = null;
      memberReg = new Cluster.Region[memberRegions.size()];
      memberReg = memberRegions.values().toArray(memberReg);

      return memberReg;
    }

    public Cluster.Client[] getMemberClients() {
      Cluster.Client[] memberClients = null;
      memberClients = new Cluster.Client[memberClientsHMap.size()];
      memberClients = memberClientsHMap.values().toArray(memberClients);

      return memberClients;
    }

    public Cluster.GatewaySender[] getMemberGatewaySenders() {
      Cluster.GatewaySender[] memberGWS = null;
      memberGWS = new Cluster.GatewaySender[gatewaySenderList.size()];
      memberGWS = gatewaySenderList.toArray(memberGWS);
      return memberGWS;
    }

    public Object[] getMemberStatisticTrend(int trendId) {
      Object[] returnArray = null;
      switch (trendId) {
      case MEMBER_STAT_GARBAGE_COLLECTION:
        synchronized (this.garbageCollectionSamples) {
          returnArray = this.garbageCollectionSamples.toArray();
        }

        break;

      case MEMBER_STAT_HEAP_USAGE_SAMPLE:
        synchronized (this.heapUsageSamples) {
          returnArray = this.heapUsageSamples.toArray();
        }
        break;

      case MEMBER_STAT_CPU_USAGE_SAMPLE:
        synchronized (this.cpuUsageSamples) {
          returnArray = this.cpuUsageSamples.toArray();
        }
        break;

      case MEMBER_STAT_GETS_PER_SECOND:
        synchronized (this.getsPerSecond) {
          returnArray = this.getsPerSecond.toArray();
        }
        break;

      case MEMBER_STAT_PUTS_PER_SECOND:
        synchronized (this.putsPerSecond) {
          returnArray = this.putsPerSecond.toArray();
        }
        break;

      case MEMBER_STAT_THROUGHPUT_WRITES:
        synchronized (this.throughputWritesTrend) {
          returnArray = this.throughputWritesTrend.toArray();
        }
        break;

      case MEMBER_STAT_THROUGHPUT_READS:
        synchronized (this.throughputReadsTrend) {
          returnArray = this.throughputReadsTrend.toArray();
        }
        break;
      }

      return returnArray;
    }

    public String getMemberPort() {
      return this.memberPort;
    }

    public void setMemberPort(String memberPort) {
      this.memberPort = memberPort;
    }

    public float getThroughputWrites() {
      return this.throughputWrites;
    }

    public void setThroughputWrites(float throughputWrites) {
      this.throughputWrites = throughputWrites;
    }

    public float getThroughputReads() {
      return this.throughputReads;
    }

    public void setThroughputReads(float throughputReads) {
      this.throughputReads = throughputReads;
    }

    public long getTotalDiskUsage() {
      return this.totalDiskUsage;
    }

    public void setTotalDiskUsage(long totalDiskUsage) {
      this.totalDiskUsage = totalDiskUsage;
    }

    public String getId() {
      return this.id;
    }

    public String getName() {
      return this.name;
    }

    public Double getLoadAverage() {
      return this.loadAverage;
    }

    public void setLoadAverage(Double loadAverage) {
      this.loadAverage = loadAverage;
    }

    public String getHost() {
      return this.host;
    }

    public Long getUptime() {
      return this.uptime;
    }

    public String getQueueBacklog() {
      return this.queueBacklog;
    }

    public Map<String, Cluster.Region> getMemberRegions() {
      return this.memberRegions;
    }

    public void setMemberRegions(Map<String, Cluster.Region> memberRegions) {
      this.memberRegions = memberRegions;
    }

    public long getCurrentHeapSize() {
      return this.currentHeapSize;
    }

    public void setCurrentHeapSize(long currentHeapSize) {
      this.currentHeapSize = currentHeapSize;
    }

    public long getMaxHeapSize() {
      return this.maxHeapSize;
    }

    public void setMaxHeapSize(long maxHeapSize) {
      this.maxHeapSize = maxHeapSize;
    }

    public boolean isManager() {
      return this.manager;
    }

    public void setManager(boolean manager) {
      this.manager = manager;
    }

    public int getAvgHeapUsage() {
      return this.avgHeapUsage;
    }

    public void setAvgHeapUsage(int avgHeapUsage) {
      this.avgHeapUsage = avgHeapUsage;
    }

    public long getOffHeapFreeSize() {
      return OffHeapFreeSize;
    }

    public void setOffHeapFreeSize(long offHeapFreeSize) {
      this.OffHeapFreeSize = offHeapFreeSize;
    }

    public long getOffHeapUsedSize() {
      return OffHeapUsedSize;
    }

    public void setOffHeapUsedSize(long offHeapUsedSize) {
      this.OffHeapUsedSize = offHeapUsedSize;
    }

    public void setId(String id) {
      this.id = id;
    }

    public void setName(String name) {
      this.name = name;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public void setUptime(Long uptime) {
      this.uptime = uptime;
    }

    public void setQueueBacklog(String queueBacklog) {
      this.queueBacklog = queueBacklog;
    }

    public int getTotalRegionCount() {
      return this.totalRegionCount;
    }

    public void setTotalRegionCount(int totalRegionCount) {
      this.totalRegionCount = totalRegionCount;
    }

    public long getTotalBytesOnDisk() {
      return this.totalBytesOnDisk;
    }

    public void setTotalBytesOnDisk(long totalBytesOnDisk) {
      this.totalBytesOnDisk = totalBytesOnDisk;
    }

    public Float getCpuUsage() {
      return this.cpuUsage;
    }

    public void setCpuUsage(Float cpuUsage) {
      this.cpuUsage = cpuUsage;
    }

    public Float getHostCpuUsage() {
      return this.hostCpuUsage;
    }

    public void setHostCpuUsage(Float hostCpuUsage) {
      this.hostCpuUsage = hostCpuUsage;
    }

    public float getGetsRate() {
      return this.getsRate;
    }

    public void setGetsRate(float getsRate) {
      this.getsRate = getsRate;
    }

    public float getPutsRate() {
      return this.putsRate;
    }

    public void setPutsRate(float putsRate) {
      this.putsRate = putsRate;
    }

    public Map<String, Cluster.Client> getMemberClientsHMap() {
      return this.memberClientsHMap;
    }

    public void setMemberClientsHMap(
        Map<String, Cluster.Client> memberClientsHMap) {
      this.memberClientsHMap = memberClientsHMap;
    }

    public boolean isCache() {
      return this.isCache;
    }

    public void setCache(boolean isCache) {
      this.isCache = isCache;
    }

    public boolean isGateway() {
      return this.isGateway;
    }

    public void setGateway(boolean isGateway) {
      this.isGateway = isGateway;
    }

    public int getNumThreads() {
      return this.numThreads;
    }

    public void setNumThreads(int numThreads) {
      this.numThreads = numThreads;
    }

    public Long getTotalFileDescriptorOpen() {
      return this.totalFileDescriptorOpen;
    }

    public void setTotalFileDescriptorOpen(Long totalFileDescriptorOpen) {
      this.totalFileDescriptorOpen = totalFileDescriptorOpen;
    }

    public Long getGarbageCollectionCount() {
      return this.garbageCollectionCount;
    }

    public void setGarbageCollectionCount(Long garbageCollectionCount) {
      this.garbageCollectionCount = garbageCollectionCount;
    }

    public boolean isLocator() {
      return this.isLocator;
    }

    public void setLocator(boolean isLocator) {
      this.isLocator = isLocator;
    }

    public Cluster.GatewayReceiver getGatewayReceiver() {
      return this.gatewayReceiver;
    }

    public void setGatewayReceiver(Cluster.GatewayReceiver gatewayReceiver) {
      this.gatewayReceiver = gatewayReceiver;
    }

    public List<Cluster.GatewaySender> getGatewaySenderList() {
      return this.gatewaySenderList;
    }

    public void setGatewaySenderList(
        List<Cluster.GatewaySender> gatewaySenderList) {
      this.gatewaySenderList = gatewaySenderList;
    }

    public boolean isServer() {
      return this.isServer;
    }

    public void setServer(boolean isServer) {
      this.isServer = isServer;
    }

    public boolean isLead() {
      return isLead;
    }

    public void setLead(boolean isLead) {
      this.isLead = isLead;
    }

    public CircularFifoBuffer getCpuUsageSamples() {
      return this.cpuUsageSamples;
    }

    public void setCpuUsageSamples(CircularFifoBuffer cpuUsageSamples) {
      this.cpuUsageSamples = cpuUsageSamples;
    }

    public CircularFifoBuffer getHeapUsageSamples() {
      return this.heapUsageSamples;
    }

    public void setHeapUsageSamples(CircularFifoBuffer heapUsageSamples) {
      this.heapUsageSamples = heapUsageSamples;
    }

    public CircularFifoBuffer getTotalBytesOnDiskSamples() {
      return this.totalBytesOnDiskSamples;
    }

    public void setTotalBytesOnDiskSamples(
        CircularFifoBuffer totalBytesOnDiskSamples) {
      this.totalBytesOnDiskSamples = totalBytesOnDiskSamples;
    }

    public CircularFifoBuffer getGetsPerSecond() {
      return this.getsPerSecond;
    }

    public void setGetsPerSecond(CircularFifoBuffer getsPerSecond) {
      this.getsPerSecond = getsPerSecond;
    }

    public CircularFifoBuffer getPutsPerSecond() {
      return this.putsPerSecond;
    }

    public void setPutsPerSecond(CircularFifoBuffer putsPerSecond) {
      this.putsPerSecond = putsPerSecond;
    }

    public CircularFifoBuffer getThroughputWritesTrend() {
      return this.throughputWritesTrend;
    }

    public void setThroughputWritesTrend(
        CircularFifoBuffer throughputWritesTrend) {
      this.throughputWritesTrend = throughputWritesTrend;
    }

    public CircularFifoBuffer getThroughputReadsTrend() {
      return this.throughputReadsTrend;
    }

    public void setThroughputReadsTrend(CircularFifoBuffer throughputReadsTrend) {
      this.throughputReadsTrend = throughputReadsTrend;
    }

    public CircularFifoBuffer getGarbageCollectionSamples() {
      return this.garbageCollectionSamples;
    }

    public void setGarbageCollectionSamples(
        CircularFifoBuffer garbageCollectionSamples) {
      this.garbageCollectionSamples = garbageCollectionSamples;
    }

    public long getNumSnappyDataClients() {
      return numSnappyDataClients;
    }

    public void setNumSnappyDataClients(long numSnappyDataClients) {
      this.numSnappyDataClients = numSnappyDataClients;
    }

    public void updateMemberClientsHMap(
        HashMap<String, Cluster.Client> memberClientsHM) {

      if (Cluster.LAST_UPDATE_TIME == 0) {
        Cluster.LAST_UPDATE_TIME = System.nanoTime();
      }

      long systemNanoTime = System.nanoTime();

      for (Map.Entry<String, Cluster.Client> entry : memberClientsHM.entrySet()) {
        String clientId = entry.getKey();
        Cluster.Client client = entry.getValue();

        if (memberClientsHMap.get(clientId) != null) {
          Client existingClient = memberClientsHMap.get(clientId);
          Client updatedClient = memberClientsHM.get(clientId);

          existingClient.setGets(updatedClient.getGets());
          existingClient.setPuts(updatedClient.getPuts());
          existingClient.setCpus(updatedClient.getCpus());
          existingClient.setQueueSize(updatedClient.getQueueSize());
          existingClient.setStatus(updatedClient.getStatus());
          existingClient.setThreads(updatedClient.getThreads());
          long elapsedTime = updatedClient.getUptime()
              - existingClient.getUptime();
          existingClient.setUptime(updatedClient.getUptime());

          // set cpu usage
          long lastCPUTime = 0;
          lastCPUTime = existingClient.getProcessCpuTime();
          long currCPUTime = 0;
          currCPUTime = updatedClient.getProcessCpuTime();

          float newCPUTime = (float) (currCPUTime - lastCPUTime)
              / (elapsedTime * 1000000000);

          float newCPUUsage = 0;
          int availableCpus = updatedClient.getCpus();
          if (availableCpus > 0) {
            newCPUUsage = newCPUTime / availableCpus;
          }

          existingClient.setCpuUsage(newCPUUsage);
          existingClient.setProcessCpuTime(currCPUTime);

        } else {
          // Add client to clients list
          memberClientsHMap.put(clientId, client);
        }

      }

      // Remove unwanted entries from clients list
      HashMap<String, Cluster.Client> memberClientsHMapNew = new HashMap<String, Cluster.Client>();
      for (Map.Entry<String, Cluster.Client> entry : memberClientsHMap
          .entrySet()) {
        String clientId = entry.getKey();
        if (memberClientsHM.get(clientId) != null) {
          memberClientsHMapNew.put(clientId, memberClientsHMap.get(clientId));
        }
      }
      // replace existing memberClientsHMap by memberClientsHMapNew
      this.setMemberClientsHMap(memberClientsHMapNew);

      // update last update time
      Cluster.LAST_UPDATE_TIME = systemNanoTime;

    }

  }

  /**
   * Member Inner Class
   */
  public static class Statement {

    private String queryDefn;
    private long numTimesCompiled;
    private long numExecution;
    private long numExecutionsInProgress;
    private long numTimesGlobalIndexLookup;
    private long numRowsModified;
    private long parseTime;
    private long bindTime;
    private long optimizeTime;
    private long routingInfoTime;
    private long generateTime;
    private long totalCompilationTime;
    private long executionTime;
    private long projectionTime;
    private long totalExecutionTime;
    private long rowsModificationTime;
    private long qNNumRowsSeen;
    private long qNMsgSendTime;
    private long qNMsgSerTime;
    private long qNRespDeSerTime;

    public static String[] getGridColumnNames() {
      String[] colNames = new String[] {
          PulseConstants.MBEAN_COLNAME_QUERYDEFINITION,
          PulseConstants.MBEAN_COLNAME_NUMEXECUTION,
          PulseConstants.MBEAN_COLNAME_TOTALEXECUTIONTIME,
          PulseConstants.MBEAN_COLNAME_NUMEXECUTIONSINPROGRESS,
          PulseConstants.MBEAN_COLNAME_NUMTIMESCOMPILED,
          PulseConstants.MBEAN_COLNAME_NUMTIMESGLOBALINDEXLOOKUP,
          PulseConstants.MBEAN_COLNAME_NUMROWSMODIFIED,
          PulseConstants.MBEAN_COLNAME_PARSETIME,
          PulseConstants.MBEAN_COLNAME_BINDTIME,
          PulseConstants.MBEAN_COLNAME_OPTIMIZETIME,
          PulseConstants.MBEAN_COLNAME_ROUTINGINFOTIME,
          PulseConstants.MBEAN_COLNAME_GENERATETIME,
          PulseConstants.MBEAN_COLNAME_TOTALCOMPILATIONTIME,
          PulseConstants.MBEAN_COLNAME_EXECUTIONTIME,
          PulseConstants.MBEAN_COLNAME_PROJECTIONTIME,
          PulseConstants.MBEAN_COLNAME_ROWSMODIFICATIONTIME,
          PulseConstants.MBEAN_COLNAME_QNNUMROWSSEEN,
          PulseConstants.MBEAN_COLNAME_QNMSGSENDTIME,
          PulseConstants.MBEAN_COLNAME_QNMSGSERTIME,
          PulseConstants.MBEAN_COLNAME_QNRESPDESERTIME };
      return colNames;
    }

    public static String[] getGridColumnAttributes() {
      String[] colAttributes = new String[] {
          PulseConstants.MBEAN_ATTRIBUTE_QUERYDEFINITION,
          PulseConstants.MBEAN_ATTRIBUTE_NUMEXECUTION,
          PulseConstants.MBEAN_ATTRIBUTE_TOTALEXECUTIONTIME,
          PulseConstants.MBEAN_ATTRIBUTE_NUMEXECUTIONSINPROGRESS,
          PulseConstants.MBEAN_ATTRIBUTE_NUMTIMESCOMPILED,
          PulseConstants.MBEAN_ATTRIBUTE_NUMTIMESGLOBALINDEXLOOKUP,
          PulseConstants.MBEAN_ATTRIBUTE_NUMROWSMODIFIED,
          PulseConstants.MBEAN_ATTRIBUTE_PARSETIME,
          PulseConstants.MBEAN_ATTRIBUTE_BINDTIME,
          PulseConstants.MBEAN_ATTRIBUTE_OPTIMIZETIME,
          PulseConstants.MBEAN_ATTRIBUTE_ROUTINGINFOTIME,
          PulseConstants.MBEAN_ATTRIBUTE_GENERATETIME,
          PulseConstants.MBEAN_ATTRIBUTE_TOTALCOMPILATIONTIME,
          PulseConstants.MBEAN_ATTRIBUTE_EXECUTIONTIME,
          PulseConstants.MBEAN_ATTRIBUTE_PROJECTIONTIME,
          
          PulseConstants.MBEAN_ATTRIBUTE_ROWSMODIFICATIONTIME,
          PulseConstants.MBEAN_ATTRIBUTE_QNNUMROWSSEEN,
          PulseConstants.MBEAN_ATTRIBUTE_QNMSGSENDTIME,
          PulseConstants.MBEAN_ATTRIBUTE_QNMSGSERTIME,
          PulseConstants.MBEAN_ATTRIBUTE_QNRESPDESERTIME };
      return colAttributes;
    }

    public static int[] getGridColumnWidths() {
      int[] colWidths = new int[] { 300, 150, 160, 180, 150, 200, 150, 130, 130,
          160, 140, 180, 170, 160, 130,  190, 170, 170, 170, 200 };
      return colWidths;
    }

    /**
     * @return the numTimesCompiled
     */
    public String getQueryDefinition() {
      return queryDefn;
    }

    /**
     * @param numTimesCompiled
     *          the numTimesCompiled to set
     */
    public void setQueryDefinition(String queryDefn) {
      this.queryDefn = queryDefn;
    }

    /**
     * @return the numTimesCompiled
     */
    public long getNumTimesCompiled() {
      return numTimesCompiled;
    }

    /**
     * @param numTimesCompiled
     *          the numTimesCompiled to set
     */
    public void setNumTimesCompiled(long numTimesCompiled) {
      this.numTimesCompiled = numTimesCompiled;
    }

    /**
     * @return the numExecution
     */
    public long getNumExecution() {
      return numExecution;
    }

    /**
     * @param numExecution
     *          the numExecution to set
     */
    public void setNumExecution(long numExecution) {
      this.numExecution = numExecution;
    }

    /**
     * @return the numExecutionsInProgress
     */
    public long getNumExecutionsInProgress() {
      return numExecutionsInProgress;
    }

    /**
     * @param numExecutionsInProgress
     *          the numExecutionsInProgress to set
     */
    public void setNumExecutionsInProgress(long numExecutionsInProgress) {
      this.numExecutionsInProgress = numExecutionsInProgress;
    }

    /**
     * @return the numTimesGlobalIndexLookup
     */
    public long getNumTimesGlobalIndexLookup() {
      return numTimesGlobalIndexLookup;
    }

    /**
     * @param numTimesGlobalIndexLookup
     *          the numTimesGlobalIndexLookup to set
     */
    public void setNumTimesGlobalIndexLookup(long numTimesGlobalIndexLookup) {
      this.numTimesGlobalIndexLookup = numTimesGlobalIndexLookup;
    }

    /**
     * @return the numRowsModified
     */
    public long getNumRowsModified() {
      return numRowsModified;
    }

    /**
     * @param numRowsModified
     *          the numRowsModified to set
     */
    public void setNumRowsModified(long numRowsModified) {
      this.numRowsModified = numRowsModified;
    }

    /**
     * @return the parseTime
     */
    public long getParseTime() {
      return parseTime;
    }

    /**
     * @param parseTime
     *          the parseTime to set
     */
    public void setParseTime(long parseTime) {
      this.parseTime = parseTime;
    }

    /**
     * @return the bindTime
     */
    public long getBindTime() {
      return bindTime;
    }

    /**
     * @param bindTime
     *          the bindTime to set
     */
    public void setBindTime(long bindTime) {
      this.bindTime = bindTime;
    }

    /**
     * @return the optimizeTime
     */
    public long getOptimizeTime() {
      return optimizeTime;
    }

    /**
     * @param optimizeTime
     *          the optimizeTime to set
     */
    public void setOptimizeTime(long optimizeTime) {
      this.optimizeTime = optimizeTime;
    }

    /**
     * @return the routingInfoTime
     */
    public long getRoutingInfoTime() {
      return routingInfoTime;
    }

    /**
     * @param routingInfoTime
     *          the routingInfoTime to set
     */
    public void setRoutingInfoTime(long routingInfoTime) {
      this.routingInfoTime = routingInfoTime;
    }

    /**
     * @return the generateTime
     */
    public long getGenerateTime() {
      return generateTime;
    }

    /**
     * @param generateTime
     *          the generateTime to set
     */
    public void setGenerateTime(long generateTime) {
      this.generateTime = generateTime;
    }

    /**
     * @return the totalCompilationTime
     */
    public long getTotalCompilationTime() {
      return totalCompilationTime;
    }

    /**
     * @param totalCompilationTime
     *          the totalCompilationTime to set
     */
    public void setTotalCompilationTime(long totalCompilationTime) {
      this.totalCompilationTime = totalCompilationTime;
    }

    /**
     * @return the executionTime
     */
    public long getExecutionTime() {
      return executionTime;
    }

    /**
     * @param executionTime
     *          the executionTime to set
     */
    public void setExecutionTime(long executionTime) {
      this.executionTime = executionTime;
    }

    /**
     * @return the projectionTime
     */
    public long getProjectionTime() {
      return projectionTime;
    }

    /**
     * @param projectionTime
     *          the projectionTime to set
     */
    public void setProjectionTime(long projectionTime) {
      this.projectionTime = projectionTime;
    }

    /**
     * @return the totalExecutionTime
     */
    public long getTotalExecutionTime() {
      return totalExecutionTime;
    }

    /**
     * @param totalExecutionTime
     *          the totalExecutionTime to set
     */
    public void setTotalExecutionTime(long totalExecutionTime) {
      this.totalExecutionTime = totalExecutionTime;
    }

    /**
     * @return the rowsModificationTime
     */
    public long getRowsModificationTime() {
      return rowsModificationTime;
    }

    /**
     * @param rowsModificationTime
     *          the rowsModificationTime to set
     */
    public void setRowsModificationTime(long rowsModificationTime) {
      this.rowsModificationTime = rowsModificationTime;
    }

    /**
     * @return the qNNumRowsSeen
     */
    public long getqNNumRowsSeen() {
      return qNNumRowsSeen;
    }

    /**
     * @param qNNumRowsSeen
     *          the qNNumRowsSeen to set
     */
    public void setqNNumRowsSeen(long qNNumRowsSeen) {
      this.qNNumRowsSeen = qNNumRowsSeen;
    }

    /**
     * @return the qNMsgSendTime
     */
    public long getqNMsgSendTime() {
      return qNMsgSendTime;
    }

    /**
     * @param qNMsgSendTime
     *          the qNMsgSendTime to set
     */
    public void setqNMsgSendTime(long qNMsgSendTime) {
      this.qNMsgSendTime = qNMsgSendTime;
    }

    /**
     * @return the qNMsgSerTime
     */
    public long getqNMsgSerTime() {
      return qNMsgSerTime;
    }

    /**
     * @param qNMsgSerTime
     *          the qNMsgSerTime to set
     */
    public void setqNMsgSerTime(long qNMsgSerTime) {
      this.qNMsgSerTime = qNMsgSerTime;
    }

    /**
     * @return the qNRespDeSerTime
     */
    public long getqNRespDeSerTime() {
      return qNRespDeSerTime;
    }

    /**
     * @param qNRespDeSerTime
     *          the qNRespDeSerTime to set
     */
    public void setqNRespDeSerTime(long qNRespDeSerTime) {
      this.qNRespDeSerTime = qNRespDeSerTime;
    }
  }

  /**
   * Region Inner Class
   */
  public static class Region {
    // start: fields defined in MBean
    private String fullPath;
    private float diskReadsRate;
    private float diskWritesRate;
    private float getsRate;
    private float putsRate;
    private float averageReads;
    private float averageWrites;
    private float lruEvictionRate;
    private String regionType;
    private long systemRegionEntryCount;
    private int memberCount;
    private String name;
    private boolean persistentEnabled;
    private long entrySize;
    private boolean wanEnabled;
    private int emptyNode;
    private long diskUsage;
    private String scope;
    private String diskStoreName;
    private boolean diskSynchronous;
    private boolean enableOffHeapMemory;
    private String compressionCodec = "";
    private boolean hdfsWriteOnly;

    // fields defined in related column store region MBean
    private boolean isColocatedWith = false;
    private String regionColcatedWith;
    private long entrySize_columnStore; 
    private long SystemRegionEntryCount_columnStore;
    private long RowsInCachedBatches_columnStore;
    private float diskReadsRate_columnStore;
    private float diskWritesRate_columnStore;
    private float getsRate_columnStore;
    private float putsRate_columnStore;
    private float averageReads_columnStore;
    private float averageWrites_columnStore;
    private long diskUsage_columnStore;

    private List<String> memberName = new ArrayList<String>();
    private CircularFifoBuffer getsPerSecTrend = new CircularFifoBuffer(
        MAX_SAMPLE_SIZE);
    private CircularFifoBuffer putsPerSecTrend = new CircularFifoBuffer(
        MAX_SAMPLE_SIZE);
    private CircularFifoBuffer diskReadsPerSecTrend = new CircularFifoBuffer(
        MAX_SAMPLE_SIZE);
    private CircularFifoBuffer diskWritesPerSecTrend = new CircularFifoBuffer(
        MAX_SAMPLE_SIZE);
    private CircularFifoBuffer averageReadsTrend = new CircularFifoBuffer(
        MAX_SAMPLE_SIZE);
    private CircularFifoBuffer averageWritesTrend = new CircularFifoBuffer(
        MAX_SAMPLE_SIZE);

    public static final int REGION_STAT_GETS_PER_SEC_TREND = 0;
    public static final int REGION_STAT_PUTS_PER_SEC_TREND = 1;
    public static final int REGION_STAT_DISK_READS_PER_SEC_TREND = 3;
    public static final int REGION_STAT_DISK_WRITES_PER_SEC_TREND = 4;
    public static final int REGION_STAT_AVERAGE_READS_TREND = 5;
    public static final int REGION_STAT_AVERAGE_WRITES_TREND = 6;

    // end: fields defined in MBean

    public Object[] getRegionStatisticTrend(int trendId) {

      Object[] returnArray = null;
      switch (trendId) {
      case REGION_STAT_GETS_PER_SEC_TREND:
        synchronized (this.getsPerSecTrend) {
          returnArray = this.getsPerSecTrend.toArray();
        }
        break;

      case REGION_STAT_PUTS_PER_SEC_TREND:
        synchronized (this.putsPerSecTrend) {
          returnArray = this.putsPerSecTrend.toArray();
        }
        break;

      case REGION_STAT_DISK_READS_PER_SEC_TREND:
        synchronized (this.diskReadsPerSecTrend) {
          returnArray = this.diskReadsPerSecTrend.toArray();
        }
        break;

      case REGION_STAT_DISK_WRITES_PER_SEC_TREND:
        synchronized (this.diskWritesPerSecTrend) {
          returnArray = this.diskWritesPerSecTrend.toArray();
        }
        break;

      case REGION_STAT_AVERAGE_READS_TREND:
        synchronized (this.averageReadsTrend) {
          returnArray = this.averageReadsTrend.toArray();
        }
        break;

      case REGION_STAT_AVERAGE_WRITES_TREND:
        synchronized (this.averageWritesTrend) {
          returnArray = this.averageWritesTrend.toArray();
        }
        break;
      }

      return returnArray;
    }

    public boolean isDiskSynchronous() {
      return this.diskSynchronous;
    }

    public void setDiskSynchronous(boolean diskSynchronous) {
      this.diskSynchronous = diskSynchronous;
    }

    public String getDiskStoreName() {
      return this.diskStoreName;
    }

    public void setDiskStoreName(String diskStoreName) {
      this.diskStoreName = diskStoreName;
    }

    public String getScope() {
      return this.scope;
    }

    public void setScope(String scope) {
      this.scope = scope;
    }

    public int getEmptyNode() {
      return this.emptyNode;
    }

    public void setEmptyNode(int emptyNode) {
      this.emptyNode = emptyNode;
    }

    public long getDiskUsage() {
      return this.diskUsage;
    }

    public void setDiskUsage(long diskUsage) {
      this.diskUsage = diskUsage;
    }

    public void setEntrySize(long entrySize) {
      this.entrySize = entrySize;
    }

    public boolean getWanEnabled() {
      return this.wanEnabled;
    }

    public void setWanEnabled(boolean wanEnabled) {
      this.wanEnabled = wanEnabled;
    }

    public boolean getPersistentEnabled() {
      return this.persistentEnabled;
    }

    public void setPersistentEnabled(boolean persistentEnabled) {
      this.persistentEnabled = persistentEnabled;
    }

    public String getName() {
      return this.name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public long getEntrySize() {
      return this.entrySize;
    }

    public List<String> getMemberName() {
      return this.memberName;
    }

    public void setMemberName(List<String> memberName) {
      this.memberName = memberName;
    }

    public String getFullPath() {
      return this.fullPath;
    }

    public void setFullPath(String fullPath) {
      this.fullPath = fullPath;
    }

    public float getDiskReadsRate() {
      return this.diskReadsRate;
    }

    public void setDiskReadsRate(float diskReadsRate) {
      this.diskReadsRate = diskReadsRate;
    }

    public float getDiskWritesRate() {
      return this.diskWritesRate;
    }

    public void setDiskWritesRate(float diskWritesRate) {
      this.diskWritesRate = diskWritesRate;
    }

    public CircularFifoBuffer getDiskReadsPerSecTrend() {
      return this.diskReadsPerSecTrend;
    }

    public void setDiskReadsPerSecTrend(CircularFifoBuffer diskReadsPerSecTrend) {
      this.diskReadsPerSecTrend = diskReadsPerSecTrend;
    }

    public CircularFifoBuffer getDiskWritesPerSecTrend() {
      return this.diskWritesPerSecTrend;
    }

    public void setDiskWritesPerSecTrend(
        CircularFifoBuffer diskWritesPerSecTrend) {
      this.diskWritesPerSecTrend = diskWritesPerSecTrend;
    }

    public float getGetsRate() {
      return this.getsRate;
    }

    public void setGetsRate(float getsRate) {
      this.getsRate = getsRate;
    }

    public float getLruEvictionRate() {
      return this.lruEvictionRate;
    }

    public void setLruEvictionRate(float lruEvictionRate) {
      this.lruEvictionRate = lruEvictionRate;
    }

    public String getRegionType() {
      return this.regionType;
    }

    public void setRegionType(String regionType) {
      this.regionType = regionType;
    }

    public long getSystemRegionEntryCount() {
      return this.systemRegionEntryCount;
    }

    public void setSystemRegionEntryCount(long systemRegionEntryCount) {
      this.systemRegionEntryCount = systemRegionEntryCount;
    }

    public int getMemberCount() {
      return this.memberCount;
    }

    public void setMemberCount(int memberCount) {
      this.memberCount = memberCount;
    }

    public float getPutsRate() {
      return this.putsRate;
    }

    public void setPutsRate(float putsRate) {
      this.putsRate = putsRate;
    }

    public float getAverageReads() {
      return this.averageReads;
    }

    public void setAverageReads(float averageReads) {
      this.averageReads = averageReads;
    }

    public float getAverageWrites() {
      return this.averageWrites;
    }

    public void setAverageWrites(float averageWrites) {
      this.averageWrites = averageWrites;
    }

    public CircularFifoBuffer getGetsPerSecTrend() {
      return this.getsPerSecTrend;
    }

    public void setGetsPerSecTrend(CircularFifoBuffer getsPerSecTrend) {
      this.getsPerSecTrend = getsPerSecTrend;
    }

    public CircularFifoBuffer getPutsPerSecTrend() {
      return this.putsPerSecTrend;
    }

    public void setPutsPerSecTrend(CircularFifoBuffer putsPerSecTrend) {
      this.putsPerSecTrend = putsPerSecTrend;
    }

    public CircularFifoBuffer getAverageReadsTrend() {
      return this.averageReadsTrend;
    }

    public void setAverageReadsTrend(CircularFifoBuffer averageReadsTrend) {
      this.averageReadsTrend = averageReadsTrend;
    }

    public CircularFifoBuffer getAverageWritesTrend() {
      return this.averageWritesTrend;
    }

    public void setAverageWritesTrend(CircularFifoBuffer averageWritesTrend) {
      this.averageWritesTrend = averageWritesTrend;
    }

    public boolean isEnableOffHeapMemory() {
      return this.enableOffHeapMemory;
    }

    public void setEnableOffHeapMemory(boolean enableOffHeapMemory) {
      this.enableOffHeapMemory = enableOffHeapMemory;
    }

    public String getCompressionCodec() {
      return this.compressionCodec;
    }

    public void setCompressionCodec(String compressionCodec) {
      this.compressionCodec = compressionCodec;
    }

    public boolean isHdfsWriteOnly() {
      return hdfsWriteOnly;
    }

    public void setHdfsWriteOnly(boolean hdfsWriteOnly) {
      this.hdfsWriteOnly = hdfsWriteOnly;
    }

    public boolean isColocatedWith() {
      return isColocatedWith;
    }

    public void setColocatedWith(boolean isColocatedWith) {
      this.isColocatedWith = isColocatedWith;
    }

    public String getRegionColcatedWith() {
      return regionColcatedWith;
    }

    public void setRegionColcatedWith(String regionColcatedWith) {
      this.regionColcatedWith = regionColcatedWith;
    }

    public long getEntrySize_columnStore() {
      return entrySize_columnStore;
    }

    public void setEntrySize_columnStore(long entrySize_columnStore) {
      this.entrySize_columnStore = entrySize_columnStore;
    }

    public long getSystemRegionEntryCount_columnStore() {
      return SystemRegionEntryCount_columnStore;
    }

    public void setSystemRegionEntryCount_columnStore(long systemRegionEntryCount_columnStore) {
      SystemRegionEntryCount_columnStore = systemRegionEntryCount_columnStore;
    }

    public long getRowsInCachedBatches_columnStore() {
      return RowsInCachedBatches_columnStore;
    }

    public void setRowsInCachedBatches_columnStore(long rowsInCachedBatches_columnStore) {
      RowsInCachedBatches_columnStore = rowsInCachedBatches_columnStore;
    }

    public float getDiskReadsRate_columnStore() {
      return diskReadsRate_columnStore;
    }

    public void setDiskReadsRate_columnStore(float diskReadsRate_columnStore) {
      this.diskReadsRate_columnStore = diskReadsRate_columnStore;
    }

    public float getDiskWritesRate_columnStore() {
      return diskWritesRate_columnStore;
    }

    public void setDiskWritesRate_columnStore(float diskWritesRate_columnStore) {
      this.diskWritesRate_columnStore = diskWritesRate_columnStore;
    }

    public float getGetsRate_columnStore() {
      return getsRate_columnStore;
    }

    public void setGetsRate_columnStore(float getsRate_columnStore) {
      this.getsRate_columnStore = getsRate_columnStore;
    }

    public float getPutsRate_columnStore() {
      return putsRate_columnStore;
    }

    public void setPutsRate_columnStore(float putsRate_columnStore) {
      this.putsRate_columnStore = putsRate_columnStore;
    }

    public float getAverageReads_columnStore() {
      return averageReads_columnStore;
    }

    public void setAverageReads_columnStore(float averageReads_columnStore) {
      this.averageReads_columnStore = averageReads_columnStore;
    }

    public float getAverageWrites_columnStore() {
      return averageWrites_columnStore;
    }

    public void setAverageWrites_columnStore(float averageWrites_columnStore) {
      this.averageWrites_columnStore = averageWrites_columnStore;
    }

    public long getDiskUsage_columnStore() {
      return diskUsage_columnStore;
    }

    public void setDiskUsage_columnStore(long diskUsage_columnStore) {
      this.diskUsage_columnStore = diskUsage_columnStore;
    }

  }

  /**
   * Alert Inner Class
   */
  public static class Alert {
    public static final int SEVERE = 0;
    public static final int ERROR = 1;
    public static final int WARNING = 2;
    public static final int INFO = 3;

    public static AtomicInteger ALERT_ID_CTR = new AtomicInteger();

    private int id;
    private Date timestamp;
    private int severity;
    private String memberName;
    private String description;
    private boolean isAcknowledged;
    private String iso8601Ts;

    public String getIso8601Ts() {
      return iso8601Ts;
    }

    public void setIso8601Ts(String iso8601Ts) {
      this.iso8601Ts = iso8601Ts;
    }

    public boolean isAcknowledged() {
      return this.isAcknowledged;
    }

    public void setAcknowledged(boolean isAcknowledged) {
      this.isAcknowledged = isAcknowledged;
    }

    public Date getTimestamp() {
      return this.timestamp;
    }

    public void setTimestamp(Date timestamp) {
      this.timestamp = timestamp;
      this.iso8601Ts = formatToISOTimestamp(timestamp);
    }

    public int getSeverity() {
      return this.severity;
    }

    public void setSeverity(int severity) {
      this.severity = severity;
    }

    public String getMemberName() {
      return this.memberName;
    }

    public void setMemberName(String memberName) {
      this.memberName = memberName;
    }

    public String getDescription() {
      return this.description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public int getId() {
      return this.id;
    }

    public void setId(int id) {
      this.id = id;
    }

    public static int nextID() {
      /*
       * int id = -1; synchronized (Alert.class) { ALERT_ID_CTR = ALERT_ID_CTR +
       * 1; id = ALERT_ID_CTR; }
       */
      return ALERT_ID_CTR.incrementAndGet();
    }

    private static DateFormat df = new SimpleDateFormat(PulseConstants.PULSE_NOTIFICATION_ALERT_DATE_PATTERN);

    public static String formatToISOTimestamp(Date date) {
      TimeZone tz = TimeZone.getTimeZone("UTC");
      df.setTimeZone(tz);
      return df.format(date);
    }

  }

  /**
   * Client Inner Class
   */
  public static class Client {

    private String id;
    private String name;
    private String host;
    private int queueSize;
    private float cpuUsage;
    private long uptime;
    private int threads;
    private int gets;
    private int puts;
    private int cpus;
    private long processCpuTime;
    private String status;

    public String getId() {
      return this.id;
    }

    public int getGets() {
      return this.gets;
    }

    public int getPuts() {
      return this.puts;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getName() {
      return this.name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getHost() {
      return this.host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public int getQueueSize() {
      return this.queueSize;
    }

    public void setQueueSize(int queueSize) {
      this.queueSize = queueSize;
    }

    public float getCpuUsage() {
      return this.cpuUsage;
    }

    public void setCpuUsage(float cpuUsage) {
      this.cpuUsage = cpuUsage;
    }

    public void setGets(int gets) {
      this.gets = gets;
    }

    public void setPuts(int puts) {
      this.puts = puts;
    }

    public long getUptime() {
      return this.uptime;
    }

    public void setUptime(long uptime) {
      this.uptime = uptime;
    }

    public int getThreads() {
      return this.threads;
    }

    public void setThreads(int threads) {
      this.threads = threads;
    }

    public String getStatus() {
      return this.status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public int getCpus() {
      return this.cpus;
    }

    public void setCpus(int cpus) {
      this.cpus = cpus;
    }

    public long getProcessCpuTime() {
      return this.processCpuTime;
    }

    public void setProcessCpuTime(long processCpuTime) {
      this.processCpuTime = processCpuTime;
    }

  }

  /**
   * Gateway Receiver Inner Class
   */
  public static class GatewayReceiver {

    private int listeningPort;
    private Float linkThroughput;
    private Long avgBatchProcessingTime;
    private String id;
    private int queueSize;
    private Boolean status;
    private int batchSize;

    public int getListeningPort() {
      return this.listeningPort;
    }

    public void setListeningPort(int listeningPort) {
      this.listeningPort = listeningPort;
    }

    public Float getLinkThroughput() {
      return this.linkThroughput;
    }

    public void setLinkThroughput(Float linkThroughput) {
      this.linkThroughput = linkThroughput;
    }

    public Long getAvgBatchProcessingTime() {
      return this.avgBatchProcessingTime;
    }

    public void setAvgBatchProcessingTime(Long avgBatchProcessingTime) {
      this.avgBatchProcessingTime = avgBatchProcessingTime;
    }

    public String getId() {
      return this.id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public int getQueueSize() {
      return this.queueSize;
    }

    public void setQueueSize(int queueSize) {
      this.queueSize = queueSize;
    }

    public Boolean getStatus() {
      return this.status;
    }

    public void setStatus(Boolean status) {
      this.status = status;
    }

    public int getBatchSize() {
      return this.batchSize;
    }

    public void setBatchSize(int batchSize) {
      this.batchSize = batchSize;
    }
  }

  /**
   * Gateway Sender Inner class
   */
  public static class GatewaySender {

    private Float linkThroughput;
    private String id;
    private int queueSize;
    private Boolean status;
    private boolean primary;
    private boolean senderType;
    private int batchSize;
    private boolean persistenceEnabled;

    public Float getLinkThroughput() {
      return this.linkThroughput;
    }

    public void setLinkThroughput(Float linkThroughput) {
      this.linkThroughput = linkThroughput;
    }

    public String getId() {
      return this.id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public int getQueueSize() {
      return this.queueSize;
    }

    public void setQueueSize(int queueSize) {
      this.queueSize = queueSize;
    }

    public Boolean getStatus() {
      return this.status;
    }

    public void setStatus(Boolean status) {
      this.status = status;
    }

    public boolean getPrimary() {
      return this.primary;
    }

    public void setPrimary(boolean primary) {
      this.primary = primary;
    }

    public boolean getSenderType() {
      return this.senderType;
    }

    public void setSenderType(boolean senderType) {
      this.senderType = senderType;
    }

    public int getBatchSize() {
      return this.batchSize;
    }

    public void setBatchSize(int batchSize) {
      this.batchSize = batchSize;
    }

    public boolean getPersistenceEnabled() {
      return this.persistenceEnabled;
    }

    public void setPersistenceEnabled(boolean persistenceEnabled) {
      this.persistenceEnabled = persistenceEnabled;
    }

  }

  /**
   * This function is used for calling getUpdator function of ClusterDataFactory
   * and starting the thread for updating the Cluster details.
   *
   * @param host
   *          host name
   * @param port
   *          port
   * @param userName
   *          pulse user name
   * @param userPassword
   *          pulse user password
   * @throws ConnectException
   */
  public Cluster(String host, String port, String userName, String userPassword)
      throws ConnectException {
    this.serverName = host;
    this.port = port;
    this.jmxUserName = userName;
    this.jmxUserPassword = userPassword;

    this.updater = ClusterDataFactory.getUpdater(this, host, port);
    // start();
  }

  /**
   * thread run method for updating the cluster data
   */
  @Override
  public void run() {
    while (!this.stopUpdates) {
      try {
        if (!this.updateData()) {
          this.stale++;
        } else {
          this.stale = 0;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }

      try {
        Thread.sleep(POLL_INTERVAL);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    if (LOGGER.infoEnabled()) {
      LOGGER.info(resourceBundle.getString("LOG_MSG_STOP_THREAD_UPDATES")
          + " :: " + this.serverName + ":" + this.port);
    }
  }

  /**
   * calling updateData
   *
   * @return true if update was successful. false if it failed.
   */
  private boolean updateData() {
    // This will eventually call JMX. Currently we will update this with
    // some dummy data.
    // Connect if required or hold a connection. If unable to connect,
    // return false
    if (LOGGER.finerEnabled()) {
      LOGGER.finer(resourceBundle.getString("LOG_MSG_CLUSTER_DATA_IS_UPDATING")
          + "::" + this.serverName + ":" + this.port);
    }
    return this.updater.updateData();
  }

  /**
   * for stopping the update thread
   */
  public void stopThread() {
    this.stopUpdates = true;

    try {
      join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public Map<String, Cluster.Member> getMembersHMap() {
    return this.membersHMap;
  }

  public void setMembersHMap(Map<String, Cluster.Member> membersHMap) {
    this.membersHMap = membersHMap;
  }

  public Map<String, Boolean> getWanInformation() {
    Map<String, Boolean> wanMap = null;
    wanMap = this.wanInformation;

    return wanMap;
  }

  // Returns actual wanInformation object reference
  public Map<String, Boolean> getWanInformationObject() {
    return this.wanInformation;
  }

  public void setWanInformation(Map<String, Boolean> wanInformation) {
    this.wanInformation = wanInformation;
  }

  public String getJmxUserName() {
    return this.jmxUserName;
  }

  public void setJmxUserName(String jmxUserName) {
    this.jmxUserName = jmxUserName;
  }

  public String getJmxUserPassword() {
    return this.jmxUserPassword;
  }

  public void setJmxUserPassword(String jmxUserPassword) {
    this.jmxUserPassword = jmxUserPassword;
  }

  public String getConnectionErrorMsg() {
    return this.connectionErrorMsg;
  }

  public void setConnectionErrorMsg(String connectionErrorMsg) {
    this.connectionErrorMsg = connectionErrorMsg;
  }

  public String getServerName() {
    return this.serverName;
  }

  public boolean isConnectedFlag() {
    return this.connectedFlag;
  }

  public void setConnectedFlag(boolean connectedFlag) {
    this.connectedFlag = connectedFlag;
  }

  public String getPort() {
    return this.port;
  }

  public int getStale() {
    return this.stale;
  }

  public float getWritePerSec() {
    return this.writePerSec;
  }

  public void setWritePerSec(float writePerSec) {
    this.writePerSec = writePerSec;
  }

  public float getReadPerSec() {
    return this.readPerSec;
  }

  public void setReadPerSec(float readPerSec) {
    this.readPerSec = readPerSec;
  }

  public float getQueriesPerSec() {
    return this.queriesPerSec;
  }

  public void setQueriesPerSec(float queriesPerSec) {
    this.queriesPerSec = queriesPerSec;
  }

  public float getLoadPerSec() {
    return this.loadPerSec;
  }

  public void setLoadPerSec(float loadPerSec) {
    this.loadPerSec = loadPerSec;
  }

  public int getNotificationPageNumber() {
    return this.notificationPageNumber;
  }

  public void setNotificationPageNumber(int notificationPageNumber) {
    this.notificationPageNumber = notificationPageNumber;
  }

  public void setStale(int stale) {
    this.stale = stale;
  }

  public boolean isStopUpdates() {
    return this.stopUpdates;
  }

  public void setStopUpdates(boolean stopUpdates) {
    this.stopUpdates = stopUpdates;
  }

  public Long getUsedHeapSize() {
    return this.usedHeapSize;
  }

  public void setUsedHeapSize(Long usedHeapSize) {
    this.usedHeapSize = usedHeapSize;
  }

  public int getServerCount() {
    return this.serverCount;
  }

  public void setServerCount(int serverCount) {
    this.serverCount = serverCount;
  }

  public int getLeadCount() {
    return leadCount;
  }

  public void setLeadCount(int leadCount) {
    this.leadCount = leadCount;
  }

  public int getTxnCommittedCount() {
    return txnCommittedCount;
  }

  public void setTxnCommittedCount(int txnCommittedCount) {
    this.txnCommittedCount = txnCommittedCount;
  }

  public int getTxnRollbackCount() {
    return txnRollbackCount;
  }

  public void setTxnRollbackCount(int txnRollbackCount) {
    this.txnRollbackCount = txnRollbackCount;
  }

  public int getRunningFunctionCount() {
    return this.runningFunctionCount;
  }

  public Long getRegisteredCQCount() {
    return this.registeredCQCount;
  }

  public int getSubscriptionCount() {
    return this.subscriptionCount;
  }

  public void setSubscriptionCount(int subscriptionCount) {
    this.subscriptionCount = subscriptionCount;
  }

  public void setRegisteredCQCount(Long registeredCQCount) {
    this.registeredCQCount = registeredCQCount;
  }

  public void setRunningFunctionCount(int runningFunctionCount) {
    this.runningFunctionCount = runningFunctionCount;
  }

  public Map<String, Cluster.Region> getClusterRegions() {
    return this.clusterRegionMap;
  }

  public void setClusterRegions(Map<String, Region> clusterRegionMap) {
    this.clusterRegionMap = clusterRegionMap;
  }

  public Map<String, Cluster.Statement> getClusterStatements() {
    return this.clusterStatementMap;
  }

  public void setClusterStatements(Map<String, Statement> clusterStatementMap) {
    this.clusterStatementMap = clusterStatementMap;
  }

  public Alert[] getAlertsList() {
    Alert[] list = null;
    list = new Alert[this.alertsList.size()];
    list = this.alertsList.toArray(list);

    return list;
  }

  public void setAlertsList(List<Alert> alertsList) {
    this.alertsList = alertsList;
  }

  public void setServerName(String serverName) {
    this.serverName = serverName;
  }

  public void setPort(String port) {
    this.port = port;
  }

  public Set<String> getDeletedMembers() {
    return this.deletedMembers;
  }

  public void setDeletedMembers(Set<String> deletedMembers) {
    this.deletedMembers = deletedMembers;
  }

  public Set<String> getDeletedRegions() {
    return this.deletedRegions;
  }

  public void setDeletedRegions(Set<String> deletedRegions) {
    this.deletedRegions = deletedRegions;
  }

  public Map<String, List<Member>> getPhysicalToMember() {
    Map<String, List<Member>> ptom = null;
    ptom = this.physicalToMember;
    return ptom;
  }

  public void setPhysicalToMember(Map<String, List<Member>> physicalToMember) {
    this.physicalToMember = physicalToMember;
  }

  public int getMemberCount() {
    return this.memberCount;
  }

  public void setMemberCount(int memberCount) {
    this.memberCount = memberCount;
  }

  public long getClientConnectionCount() {
    return this.clientConnectionCount;
  }

  public void setClientConnectionCount(long clientConnectionCount) {
    this.clientConnectionCount = clientConnectionCount;
  }

  public int getClusterId() {
    return this.clusterId;
  }

  public void setClusterId(int clusterId) {
    this.clusterId = clusterId;
  }

  public int getLocatorCount() {
    return this.locatorCount;
  }

  public void setLocatorCount(int locatorCount) {
    this.locatorCount = locatorCount;
  }

  public int getTotalRegionCount() {
    return this.totalRegionCount;
  }

  public void setTotalRegionCount(int totalRegionCount) {
    this.totalRegionCount = totalRegionCount;
  }

  public Long getTotalHeapSize() {
    return this.totalHeapSize;
  }

  public void setTotalHeapSize(Long totalHeapSize) {
    this.totalHeapSize = totalHeapSize;
  }

  public Long getTotalRegionEntryCount() {
    return this.totalRegionEntryCount;
  }

  public void setTotalRegionEntryCount(Long totalRegionEntryCount) {
    this.totalRegionEntryCount = totalRegionEntryCount;
  }

  public int getCurrentQueryCount() {
    return this.currentQueryCount;
  }

  public void setCurrentQueryCount(int currentQueryCount) {
    this.currentQueryCount = currentQueryCount;
  }

  public Long getTotalBytesOnDisk() {
    return this.totalBytesOnDisk;
  }

  public void setTotalBytesOnDisk(Long totalBytesOnDisk) {
    this.totalBytesOnDisk = totalBytesOnDisk;
  }

  public float getDiskReadsRate() {
    return this.diskReadsRate;
  }

  public void setDiskReadsRate(float diskReadsRate) {
    this.diskReadsRate = diskReadsRate;
  }

  public float getDiskWritesRate() {
    return this.diskWritesRate;
  }

  public void setDiskWritesRate(float diskWritesRate) {
    this.diskWritesRate = diskWritesRate;
  }

  public int getAvgDiskStorage() {
    return this.avgDiskStorage;
  }

  public void setAvgDiskStorage(int avgDiskStorage) {
    this.avgDiskStorage = avgDiskStorage;
  }

  public int getAvgDiskWritesRate() {
    return this.avgDiskWritesRate;
  }

  public void setAvgDiskWritesRate(int avgDiskWritesRate) {
    this.avgDiskWritesRate = avgDiskWritesRate;
  }

  public CircularFifoBuffer getWritePerSecTrend() {
    return this.writePerSecTrend;
  }

  public void setWritePerSecTrend(CircularFifoBuffer writePerSecTrend) {
    this.writePerSecTrend = writePerSecTrend;
  }

  public Long getGarbageCollectionCount() {
    return this.garbageCollectionCount;
  }

  public void setGarbageCollectionCount(Long garbageCollectionCount) {
    this.garbageCollectionCount = garbageCollectionCount;
  }

  public CircularFifoBuffer getTotalBytesOnDiskTrend() {
    return this.totalBytesOnDiskTrend;
  }

  public void setTotalBytesOnDiskTrend(CircularFifoBuffer totalBytesOnDiskTrend) {
    this.totalBytesOnDiskTrend = totalBytesOnDiskTrend;
  }

  public CircularFifoBuffer getThroughoutWritesTrend() {
    return this.throughoutWritesTrend;
  }

  public void setThroughoutWritesTrend(CircularFifoBuffer throughoutWritesTrend) {
    this.throughoutWritesTrend = throughoutWritesTrend;
  }

  public CircularFifoBuffer getThroughoutReadsTrend() {
    return this.throughoutReadsTrend;
  }

  public void setThroughoutReadsTrend(CircularFifoBuffer throughoutReadsTrend) {
    this.throughoutReadsTrend = throughoutReadsTrend;
  }

  public CircularFifoBuffer getReadPerSecTrend() {
    return this.readPerSecTrend;
  }

  public void setReadPerSecTrend(CircularFifoBuffer readPerSecTrend) {
    this.readPerSecTrend = readPerSecTrend;
  }

  public CircularFifoBuffer getQueriesPerSecTrend() {
    return this.queriesPerSecTrend;
  }

  public void setQueriesPerSecTrend(CircularFifoBuffer queriesPerSecTrend) {
    this.queriesPerSecTrend = queriesPerSecTrend;
  }

  public CircularFifoBuffer getMemoryUsageTrend() {
    return this.memoryUsageTrend;
  }

  public void setMemoryUsageTrend(CircularFifoBuffer memoryUsageTrend) {
    this.memoryUsageTrend = memoryUsageTrend;
  }

  public CircularFifoBuffer getGarbageCollectionTrend() {
    return this.garbageCollectionTrend;
  }

  public void setGarbageCollectionTrend(
      CircularFifoBuffer garbageCollectionSamples) {
    this.garbageCollectionTrend = garbageCollectionSamples;
  }

  public DataBrowser getDataBrowser() {
    // Initialize dataBrowser if null
    if (this.dataBrowser == null) {
      this.dataBrowser = new DataBrowser();
    }
    return this.dataBrowser;
  }

  public void setDataBrowser(DataBrowser dataBrowser) {
    this.dataBrowser = dataBrowser;
  }

  public JSONObject executeQuery(String queryText, String members, int limit)
      throws JSONException {
    // Execute data browser query
    return this.updater.executeQuery(queryText, members, limit);
  }

  public JSONArray getQueryHistoryByUserId(String userId) throws JSONException {
    return this.getDataBrowser().getQueryHistoryByUserId(userId);
  }

  public boolean addQueryInHistory(String queryText, String userName) {
    return this.getDataBrowser().addQueryInHistory(queryText, userName);
  }

  public boolean deleteQueryById(String queryId) {
    return this.getDataBrowser().deleteQueryById(queryId);
  }

  /**
   * inner class for creating Mock Data
   */
  public class MockDataUpdater implements IClusterUpdater {
    public MockDataUpdater() {
    }

    /**
     * function used for updating Cluster data for Mock
     */
    @Override
    public boolean updateData() {
      setConnectedFlag(true);
      Random r = new Random(System.currentTimeMillis());
      totalHeapSize = (long) Math.abs(r.nextInt(3200 - 2048) + 2048);
      usedHeapSize = (long) Math.abs(r.nextInt(2048));
      writePerSec = Math.abs(r.nextInt(100));
      subscriptionCount = Math.abs(r.nextInt(100));
      registeredCQCount = (long) Math.abs(r.nextInt(100));
      txnCommittedCount = Math.abs(r.nextInt(100));
      txnRollbackCount = Math.abs(r.nextInt(100));
      runningFunctionCount = Math.abs(r.nextInt(100));
      clusterId = Math.abs(r.nextInt(100));
      writePerSecTrend.add(writePerSec);
      diskWritesRate = writePerSec;
      garbageCollectionCount = (long) Math.abs(r.nextInt(100));
      garbageCollectionTrend.add(garbageCollectionCount);

      readPerSec = Math.abs(r.nextInt(100));
      readPerSecTrend.add(readPerSec);

      diskReadsRate = readPerSec;
      queriesPerSec = Math.abs(r.nextInt(100));
      queriesPerSecTrend.add(queriesPerSec);

      loadPerSec = Math.abs(r.nextInt(100));
      totalHeapSize = (long) totalHeapSize;
      totalBytesOnDisk = (long) totalHeapSize;

      totalBytesOnDiskTrend.add(totalBytesOnDisk);

      memoryUsageTrend.add((long) usedHeapSize);
      throughoutWritesTrend.add(writePerSec);
      throughoutReadsTrend.add(readPerSec);

      memberCount = 0;

      // Create 3 members first time around
      if (membersHMap.size() == 0) {

        membersHMap.put(
            "pnq-visitor1",
            initializeMember(
                "pnq-visitor1(Launcher_Manager-1099-13-40-24-5368)-24357",
                "pnq-visitor1", true, true, true, true));

        for (int i = 2; i <= 8; i++) {
          if ((i % 2) == 0) {
            membersHMap.put(
                "pnq-visitor" + i,
                initializeMember("pnq-visitor" + i
                    + "(Launcher_Server-1099-13-40-24-5368)-24357",
                    "pnq-visitor" + i, false, false, true, false));
          } else {
            if ((i % 3) == 0) {
              membersHMap.put(
                  "pnq-visitor" + i,
                  initializeMember("pnq-visitor" + i
                      + "(Launcher_Server-1099-13-40-24-5368)-24357",
                      "pnq-visitor" + i, false, false, false, false));
            } else {
              membersHMap.put(
                  "pnq-visitor" + i,
                  initializeMember("pnq-visitor" + i
                      + "(Launcher_Server-1099-13-40-24-5368)-24357",
                      "pnq-visitor" + i, false, true, true, true));
            }
          }
        }

        for (Entry<String, Member> memberSet : membersHMap.entrySet()) {
          HashMap<String, Cluster.Region> memberRegions = new HashMap<String, Cluster.Region>();
          HashMap<String, Cluster.Client> memberClientsHM = new HashMap<String, Cluster.Client>();

          Random randomGenerator = new Random();
          int randomInt = (randomGenerator.nextInt(15)) + 10;
          int regionExists = 0;
          for (int y = 0; y < randomInt; y++) {
            Region region = initMemberRegion(y, memberSet.getValue().getName());
            if (clusterRegionMap.entrySet().size() > 0) {
              for (Region clusterRegion : clusterRegionMap.values()) {
                if ((region.name).equals(clusterRegion.name)) {
                  clusterRegion.memberName.add(memberSet.getValue().getName());
                  clusterRegion.memberCount = clusterRegion.memberCount + 1;
                  regionExists = 1;
                  break;
                }
              }
              if (regionExists == 0) {
                addClusterRegion(region.getFullPath(), region);
              }
            } else {
              addClusterRegion(region.getFullPath(), region);
            }
            memberRegions.put(region.getFullPath(), region);
            totalRegionCount = clusterRegionMap.values().size();
          }
          membersHMap.get(memberSet.getKey()).setMemberRegions(memberRegions);

          if (memberSet.getValue().isCache) {
            Client client = initMemberClient(0, memberSet.getValue().getHost());
            memberClientsHM.put(client.getId(), client);
            randomInt = randomGenerator.nextInt(10);
            for (int y = 1; y < randomInt; y++) {
              Client newClient = initMemberClient(y, memberSet.getValue()
                  .getHost());
              memberClientsHM.put(newClient.getId(), newClient);
            }
            membersHMap.get(memberSet.getKey()).updateMemberClientsHMap(
                memberClientsHM);
            clientConnectionCount = clientConnectionCount
                + membersHMap.get(memberSet.getKey()).getMemberClientsHMap()
                    .size();
          }

        }
      }
      wanInformation.clear();
      int wanInfoSize = Math.abs(r.nextInt(10));
      wanInfoSize++;
      for (int i = 0; i < wanInfoSize; i++) {
        String name = "Mock Cluster" + i;
        Boolean value = false;
        if (i % 2 == 0) {
          value = true;
        }
        wanInformation.put(name, value);
      }
      memberCount = membersHMap.size();

      totalHeapSize = (long) 0;
      for (Entry<String, Member> memberSet : membersHMap.entrySet()) {
        refresh(membersHMap.get(memberSet.getKey()));
        Member member = membersHMap.get(memberSet.getKey());
        totalHeapSize += member.currentHeapSize;
      }

      for (Region region : clusterRegionMap.values()) {
        // Memory reads and writes
        region.getsRate = (Math.abs(r.nextInt(100))) + 1;
        region.putsRate = (Math.abs(r.nextInt(100))) + 1;
        region.getsPerSecTrend.add(region.getsRate);
        region.putsPerSecTrend.add(region.putsRate);

        // Disk reads and writes
        region.diskReadsRate = (Math.abs(r.nextInt(100))) + 1;
        region.diskWritesRate = (Math.abs(r.nextInt(100))) + 1;
        region.diskReadsPerSecTrend.add(region.diskReadsRate);
        region.diskWritesPerSecTrend.add(region.diskWritesRate);
      }

      if(clusterStatementMap.size() < 500){
        for(int i = 1; i <= 500; ++i) {
          if (LOGGER.infoEnabled()) {
            LOGGER.info("Adding statement = " + i);
          }

          updateClusterStatement(i);
        }
      } else if(clusterStatementMap.size() == 510){
        for(Iterator itSt = clusterStatementMap.values().iterator(); itSt.hasNext(); ){
          Cluster.Statement statement = (Cluster.Statement)itSt.next();
          statement.setNumTimesCompiled((statement.getNumTimesCompiled()+5));
          statement.setNumExecution((statement.getNumExecution()+5));
          statement.setNumExecutionsInProgress((statement.getNumExecutionsInProgress()+5));
          statement.setNumTimesGlobalIndexLookup((statement.getNumTimesGlobalIndexLookup()+5));
          statement.setNumRowsModified((statement.getNumRowsModified()+5));
        }  
      } else if(clusterStatementMap.size() < 510){
        Cluster.Statement statement = new Cluster.Statement();
        Random randomGenerator = new Random();
        String statementDefinition = "select * from member where member_name = member-510"
            + " and lastUpdatedTime = '" + new Date().toString() + "'";
        Integer intVal = randomGenerator.nextInt(5);
        statement.setQueryDefinition(statementDefinition);
        statement.setNumTimesCompiled(intVal.longValue());
        statement.setNumExecution(intVal.longValue());
        statement.setNumExecutionsInProgress(intVal.longValue());
        statement.setNumTimesGlobalIndexLookup(intVal.longValue());
        statement.setNumRowsModified(intVal.longValue());
        statement.setParseTime(randomGenerator.nextLong());
        statement.setBindTime(randomGenerator.nextLong());
        statement.setOptimizeTime(randomGenerator.nextLong());
        statement.setRoutingInfoTime(randomGenerator.nextLong());
        statement.setGenerateTime(randomGenerator.nextLong());
        statement.setTotalCompilationTime(randomGenerator.nextLong());
        statement.setExecutionTime(randomGenerator.nextLong());
        statement.setProjectionTime(randomGenerator.nextLong());
        statement.setTotalExecutionTime(randomGenerator.nextLong());
        statement.setRowsModificationTime(randomGenerator.nextLong());
        statement.setqNNumRowsSeen(intVal.longValue());
        statement.setqNMsgSendTime(randomGenerator.nextLong());
        statement.setqNMsgSerTime(randomGenerator.nextLong());
        statement.setqNRespDeSerTime(randomGenerator.nextLong());
        addClusterStatement(statementDefinition, statement);
      }

      return true;
    }

    private void updateClusterStatement(int iNum) {

      Cluster.Statement statement = new Cluster.Statement();
      Random randomGenerator = new Random();
      String statementDefinition = "select * from member where member_name = member-"
          + iNum + " and lastUpdatedTime = '" + new Date().toString() + "'";
      Integer intVal = randomGenerator.nextInt(5);
      statement.setQueryDefinition(statementDefinition);
      statement.setNumTimesCompiled(intVal.longValue());
      statement.setNumExecution(intVal.longValue());
      statement.setNumExecutionsInProgress(intVal.longValue());
      statement.setNumTimesGlobalIndexLookup(intVal.longValue());
      statement.setNumRowsModified(intVal.longValue());
      statement.setParseTime(randomGenerator.nextLong());
      statement.setBindTime(randomGenerator.nextLong());
      statement.setOptimizeTime(randomGenerator.nextLong());
      statement.setRoutingInfoTime(randomGenerator.nextLong());
      statement.setGenerateTime(randomGenerator.nextLong());
      statement.setTotalCompilationTime(randomGenerator.nextLong());
      statement.setExecutionTime(randomGenerator.nextLong());
      statement.setProjectionTime(randomGenerator.nextLong());
      statement.setTotalExecutionTime(randomGenerator.nextLong());
      statement.setRowsModificationTime(randomGenerator.nextLong());
      statement.setqNNumRowsSeen(intVal.longValue());
      statement.setqNMsgSendTime(randomGenerator.nextLong());
      statement.setqNMsgSerTime(randomGenerator.nextLong());
      statement.setqNRespDeSerTime(randomGenerator.nextLong());
      addClusterStatement(statementDefinition, statement);

      if (LOGGER.infoEnabled()) {
        LOGGER.info("statementDefinition [" + iNum + "]" + statementDefinition);
      }
    }

    private Region initMemberRegion(int count, String memName) {

      Region memberRegion = new Region();
      memberRegion.setName("GlobalVilage_" + count);
      // region and subrgions path
      if (count < 5) {
        memberRegion.setFullPath("/GlobalVilage_" + count);
      } else if (count >= 5 && count < 8) {
        memberRegion.setFullPath("/GlobalVilage_1/GlobalVilage_" + count);
      } else if (count >= 8 && count < 10) {
        memberRegion.setFullPath("/GlobalVilage_2/GlobalVilage_" + count);
      } else if (count >= 10 && count < 14) {
        memberRegion.setFullPath("/GlobalVilage_3/GlobalVilage_" + count);
      } else {
        memberRegion
            .setFullPath("/GlobalVilage_3/GlobalVilage_11/GlobalVilage_"
                + count);
      }

      Random randomGenerator = new Random();
      int randomInt = Math.abs(randomGenerator.nextInt(100));
      memberRegion.setSystemRegionEntryCount(randomInt);
      // memberRegion.setEntrySize("N/A");
      memberRegion.setEntrySize(Math.abs(randomGenerator.nextInt(10)));
      memberRegion.setDiskStoreName("ABC");
      memberRegion.setScope("DISTRIBUTED_NO_ACK");
      memberRegion.setDiskSynchronous(true);

      memberRegion.regionType = "REPLICATE_PARTITIONED_NORMAL";
      memberRegion.persistentEnabled = true;
      if (count % 2 == 0) {
        memberRegion.wanEnabled = true;
      } else {
        memberRegion.wanEnabled = false;
      }
      memberRegion.wanEnabled = true;
      memberRegion.setSystemRegionEntryCount(Long.valueOf(String.valueOf(Math
          .abs(randomGenerator.nextInt(100)))));
      memberRegion.memberName.add(memName);
      memberRegion.memberCount = 1;
      return memberRegion;
    }

    private Client initMemberClient(int count, String host) {

      Client memberClient = new Client();
      Random r = new Random(System.currentTimeMillis());
      memberClient.setName("Name_" + count);
      long processCpuTime = (long) (r.nextDouble() * 100);
      memberClient.setProcessCpuTime(processCpuTime);
      memberClient.setCpuUsage(0);
      memberClient.setGets(Math.abs(r.nextInt(100)));
      memberClient.setHost(host);
      memberClient.setId(String.valueOf(1000 + count));
      memberClient.setPuts(Math.abs(r.nextInt(100)));
      memberClient.setCpus(Math.abs(r.nextInt(20)));
      memberClient.setQueueSize(Math.abs(r.nextInt(100)));
      if ((count % 2) == 0) {
        memberClient.setStatus("up");
      } else {
        memberClient.setStatus("down");
      }
      memberClient.setThreads(Math.abs(r.nextInt(100)));
      memberClient
          .setUptime(Math.abs(System.currentTimeMillis() - r.nextLong()));

      return memberClient;
    }

    private Member initializeMember(String id, String name, boolean manager,
        boolean isCache, boolean isLocator, boolean isServer) {
      Member m = new Member();
      m.manager = manager;
      m.id = id;
      m.name = name;

      m.host = getHostName(System.currentTimeMillis());

      m.maxHeapSize = 247;

      Random r = new Random(System.currentTimeMillis());

      m.isCache = isCache;

      m.loadAverage = (double) Math.abs(r.nextInt(100));
      m.numThreads = Math.abs(r.nextInt(100));
      m.garbageCollectionCount = (long) Math.abs(r.nextInt(100));
      m.garbageCollectionSamples.add(m.garbageCollectionCount);

      m.totalFileDescriptorOpen = (long) Math.abs(r.nextInt(100));
      m.totalDiskUsage = Math.abs(r.nextInt(100));

      m.throughputWrites = Math.abs(r.nextInt(10));
      m.throughputWritesTrend.add(m.throughputWrites);

      m.throughputReads = Math.abs(r.nextInt(10));
      m.throughputReadsTrend.add(m.throughputReads);

      if (port == null || "".equals(port.trim())) {
        port = "1089";
      }

      if (m.gatewayReceiver == null) {
        m.gatewayReceiver = new Cluster.GatewayReceiver();
      }
      m.gatewayReceiver.listeningPort = Integer.parseInt(port);
      m.gatewayReceiver.linkThroughput = (float) Math.abs(r.nextInt(10));
      m.gatewayReceiver.avgBatchProcessingTime = (long) Math.abs(r.nextInt(10));
      m.gatewayReceiver.id = String.valueOf(Math.abs(r.nextInt(10)));
      m.gatewayReceiver.queueSize = Math.abs(r.nextInt(10));
      m.gatewayReceiver.status = true;
      m.gatewayReceiver.batchSize = Math.abs(r.nextInt(10));

      int gatewaySenderCount = Math.abs(r.nextInt(10));

      for (int i = 0; i < gatewaySenderCount; i++) {
        m.gatewaySenderList.add(createGatewaySenderCount(r));
      }
      m.isLocator = isLocator;
      m.isServer = isServer;

      List<Cluster.Member> memberArrList = physicalToMember.get(m.host);
      if (memberArrList != null) {
        memberArrList.add(m);
      } else {
        List<Cluster.Member> memberList = new ArrayList<Cluster.Member>();
        memberList.add(m);
        physicalToMember.put(m.host, memberList);
      }
      memberCount++;
      return m;
    }

    private GatewaySender createGatewaySenderCount(Random r) {

      GatewaySender gatewaySender = new GatewaySender();

      gatewaySender.batchSize = Math.abs(r.nextInt(10));
      gatewaySender.id = String.valueOf(Math.abs(r.nextInt(10)));
      gatewaySender.linkThroughput = (float) Math.abs(r.nextInt(10));
      gatewaySender.persistenceEnabled = true;
      gatewaySender.primary = true;
      gatewaySender.queueSize = Math.abs(r.nextInt(10));
      gatewaySender.senderType = false;
      gatewaySender.status = true;

      return gatewaySender;
    }

    private String getHostName(long rndSeed) {
      Random rnd = new Random(rndSeed);
      String hName = null;

      int index = Math.abs(rnd.nextInt(MAX_HOSTS));

      if (hostNames.size() <= index) {
        hName = "host" + hostNames.size();
        hostNames.add(hName);
      } else {
        hName = hostNames.get(index);
      }

      List<Member> memberArrList = physicalToMember.get(hName);
      if (memberArrList != null) {
        if (memberArrList.size() > 4) {
          hName = getHostName(rndSeed + rnd.nextLong());
        }
      }
      return hName;
    }

    private void refresh(Member m) {
      if (LOGGER.infoEnabled()) {
        LOGGER.info(resourceBundle.getString("LOG_MSG_REFRESHING_MEMBER_DATA")
            + " : " + m.name);
      }

      Random r = new Random(System.currentTimeMillis());

      m.uptime = System.currentTimeMillis();
      m.queueBacklog = "" + Math.abs(r.nextInt(500));
      m.currentHeapSize = Math.abs(r.nextInt(Math.abs((int) m.maxHeapSize)));
      m.OffHeapFreeSize = Math.abs(r.nextInt(Math.abs((int) m.maxHeapSize)));
      m.OffHeapUsedSize = Math.abs(r.nextInt(Math.abs((int) m.maxHeapSize)));
      m.totalDiskUsage = Math.abs(r.nextInt(100));

      Float cpuUsage = r.nextFloat() * 100;
      m.cpuUsageSamples.add(cpuUsage);
      m.cpuUsage = cpuUsage;
      m.hostCpuUsage = r.nextFloat() * 200;

      m.heapUsageSamples.add(m.currentHeapSize);
      m.loadAverage = (double) Math.abs(r.nextInt(100));
      m.numThreads = Math.abs(r.nextInt(100));
      m.garbageCollectionCount = (long) Math.abs(r.nextInt(100));
      m.garbageCollectionSamples.add(m.garbageCollectionCount);

      m.totalFileDescriptorOpen = (long) Math.abs(r.nextInt(100));

      m.throughputWrites = Math.abs(r.nextInt(10));
      m.throughputWritesTrend.add(m.throughputWrites);

      m.throughputReads = Math.abs(r.nextInt(10));
      m.throughputReadsTrend.add(m.throughputReads);

      m.getsRate = Math.abs(r.nextInt(5000));
      m.getsPerSecond.add(m.getsRate);

      m.putsRate = Math.abs(r.nextInt(5000));
      m.putsPerSecond.add(m.putsRate);

      if (r.nextBoolean()) {
        // Generate alerts
        if (r.nextBoolean()) {
          if (r.nextInt(10) > 5) {
            alertsList
                .add(createAlert(Alert.SEVERE, m.name, alertsList.size()));
            if (alertsList.size() > ALERTS_MAX_SIZE) {
              alertsList.remove(0);
            }
          }
        }

        if (r.nextBoolean()) {
          if (r.nextInt(10) > 5) {
            alertsList.add(createAlert(Alert.ERROR, m.name, alertsList.size()));
            if (alertsList.size() > ALERTS_MAX_SIZE) {
              alertsList.remove(0);
            }
          }
        }

        if (r.nextBoolean()) {
          if (r.nextInt(10) > 5) {
            alertsList
                .add(createAlert(Alert.WARNING, m.name, alertsList.size()));
            if (alertsList.size() > ALERTS_MAX_SIZE) {
              alertsList.remove(0);
            }
          }
        }
      }
    }

    private Alert createAlert(int sev, String memberName, int index) {

      Alert alert = new Alert();
      alert.setSeverity(sev);
      alert.setId(Cluster.Alert.nextID());
      alert.setMemberName(memberName);
      alert.setTimestamp(new Date());

      switch (sev) {
      case Alert.SEVERE:
        alert.setDescription(PulseConstants.ALERT_DESC_SEVERE);
        break;
      case Alert.ERROR:
        alert.setDescription(PulseConstants.ALERT_DESC_ERROR);
        break;
      case Alert.WARNING:
        alert.setDescription(PulseConstants.ALERT_DESC_WARNING);
        break;
      }
      return alert;
    }

    @Override
    public JSONObject executeQuery(String queryText, String members, int limit)
        throws JSONException {

      BufferedReader streamReader = null;
      JSONObject jsonObject = new JSONObject();
      Random rand = new Random();
      int min = 1, max = 5;
      int randomNum = rand.nextInt(max - min + 1) + min;
      InputStream is = null;
      URL url = null;
      String inputStr = null;

      try {
        ClassLoader classLoader = Thread.currentThread()
            .getContextClassLoader();

        switch (randomNum) {
        case 1:
          url = classLoader.getResource("testQueryResultClusterSmall.txt");
          break;

        case 2:
          url = classLoader.getResource("testQueryResultSmall.txt");
          break;

        case 3:
          url = classLoader.getResource("testQueryResult.txt");
          break;

        case 4:
          url = classLoader.getResource("testQueryResultWithStructSmall.txt");
          break;

        case 5:
          url = classLoader.getResource("testQueryResultClusterWithStruct.txt");
          break;

        case 6:
          url = classLoader.getResource("testQueryResultHashMapSmall.txt");
          break;

        case 7:
          url = classLoader.getResource("testQueryResult1000.txt");
          break;

        default:
          url = classLoader.getResource("testQueryResult1000.txt");
        }

        File testQueryResultClusterSmall = new File(url.getPath());
        is = new FileInputStream(testQueryResultClusterSmall);
        streamReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder testQueryResultClusterSmallresponseStrBuilder = new StringBuilder();

        while ((inputStr = streamReader.readLine()) != null) {
          testQueryResultClusterSmallresponseStrBuilder.append(inputStr);
        }

        jsonObject = new JSONObject(
            testQueryResultClusterSmallresponseStrBuilder.toString());

        // close stream reader
        streamReader.close();

      } catch (JSONException ex) {
        LOGGER.severe(ex.getMessage());
        ex.printStackTrace();
      } catch (IOException ex) {
        LOGGER.severe(ex.getMessage());
        ex.printStackTrace();
      }

      return jsonObject;
    }
  }

  public void clearAlerts(int alertType, boolean isClearAll) {

    List<Alert> toDelete = new ArrayList<Alert>();

    if (alertType == -1) {
      if (isClearAll) {
        // Remove all alerts
        this.alertsList.clear();
        this.setNotificationPageNumber(1);
      } else {
        // Remove only acknowledged alerts
        for (int i = 0; i < this.alertsList.size(); i++) {
          Cluster.Alert alert = this.alertsList.get(i);
          if (alert.isAcknowledged()) {
            toDelete.add(alert);
          }
        }
        this.alertsList.removeAll(toDelete);
        toDelete.clear();
      }
    } else {
      for (int i = 0; i < this.alertsList.size(); i++) {
        Cluster.Alert alert = this.alertsList.get(i);
        if (alert.getSeverity() == alertType) {
          if (isClearAll) {
            // Remove all alerts of alertType
            toDelete.add(alert);
          } else if (alert.isAcknowledged()) {
            // Remove only acknowledged alerts of alertType
            toDelete.add(alert);
          }
        }
      }
      this.alertsList.removeAll(toDelete);
      toDelete.clear();
    }
  }

  public void acknowledgeAlert(int alertId) {
    for (Cluster.Alert alert : this.alertsList) {
      if (alert.getId() == alertId) {
        alert.setAcknowledged(true);
        break;
      }
    }
  }

  public void addAlert(Alert alert) {
    this.alertsList.add(alert);
    if (this.alertsList.size() > Cluster.ALERTS_MAX_SIZE)
      this.alertsList.remove(0);
  }

  /**
   * Key should be memberId + memberName Where the ':' in member id and member
   * names are replaced by '-'
   *
   *
   * @param memberKey
   * @return
   */
  public Cluster.Member getMember(String memberKey) {
    Cluster.Member member = null;
    member = this.membersHMap.get(memberKey);
    return member;
  }

  public Cluster.Member[] getMembers() {
    Cluster.Member[] members = null;
    members = new Cluster.Member[this.membersHMap.size()];
    members = this.membersHMap.values().toArray(members);
    return members;
  }

  public Cluster.Statement[] getStatements() {
    Cluster.Statement[] statements = null;
    statements = new Cluster.Statement[this.clusterStatementMap.size()];
    statements = this.clusterStatementMap.values().toArray(statements);
    return statements;
  }

  public void removeClusterRegion(String regionFullPath) {
    this.clusterRegionMap.remove(regionFullPath);
  }

  public void addClusterRegion(String regionFullPath, Region region) {
    this.clusterRegionMap.put(regionFullPath, region);
  }

  public void removeClusterStatement(String name) {
    this.clusterStatementMap.remove(name);
  }

  public void addClusterStatement(String name, Statement stmt) {
    this.clusterStatementMap.put(name, stmt);
  }

  /**
   * This class is used for switching between production(JMX) and Mock Data
   */
  public static class ClusterDataFactory {
    public static final int JMX = 0;
    public static final int MOCK = 1;

    public static int UPDATER_TYPE = JMX;

    private ClusterDataFactory() {
    }

    public static IClusterUpdater getUpdater(Cluster cluster,
        String serverName, String port) {

      String prop = System.getProperty("pulse.propMockDataUpdaterClass");
      if (prop != null) {
        Class klass;
        try {
          klass = Class.forName(prop);
          @SuppressWarnings("unchecked")
          Constructor constructor = klass.getConstructor(Cluster.class);
          Object updaterObject = constructor.newInstance(cluster);
          IClusterUpdater updater = (IClusterUpdater) updaterObject;
          return updater;
        } catch (ClassNotFoundException e) {
          cluster.LOGGER.severe(e);
        } catch (SecurityException e) {
          cluster.LOGGER.severe(e);
        } catch (NoSuchMethodException e) {
          cluster.LOGGER.severe(e);
        } catch (IllegalArgumentException e) {
          cluster.LOGGER.severe(e);
        } catch (InstantiationException e) {
          cluster.LOGGER.severe(e);
        } catch (IllegalAccessException e) {
          cluster.LOGGER.severe(e);
        } catch (InvocationTargetException e) {
          cluster.LOGGER.severe(e);
        }
        return null;
      } else {
        prop = System.getProperty("pulse.updater", "JMX");
        if ("MOCK".equalsIgnoreCase(prop)) {
          UPDATER_TYPE = MOCK;
          return cluster.new MockDataUpdater();
        } else {
          UPDATER_TYPE = JMX;
          return new JMXDataUpdater(serverName, port, cluster);
        }
      }
    }

  }
}
