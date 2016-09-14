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

package com.pivotal.gemfire.tools.pulse.internal.service;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.pivotal.gemfire.tools.pulse.internal.controllers.PulseController;
import com.pivotal.gemfire.tools.pulse.internal.data.Cluster;
import com.pivotal.gemfire.tools.pulse.internal.data.PulseConstants;
import com.pivotal.gemfire.tools.pulse.internal.data.Repository;
import com.pivotal.gemfire.tools.pulse.internal.util.StringUtils;

/**
 * Class ClusterRegionService
 * 
 * This class contains implementations of getting Cluster's regions details
 * 
 * @since version 7.5
 */
@Component
@Service("ClusterRegion")
@Scope("singleton")
public class ClusterRegionService implements PulseService {

  // String constants used for forming a json response
  private final String ENTRY_SIZE = "entrySize";

  // Comparator based upon regions entry count
  private static Comparator<Cluster.Region> regionEntryCountComparator = new Comparator<Cluster.Region>() {
    @Override
    public int compare(Cluster.Region r1, Cluster.Region r2) {
      long r1Cnt = r1.getSystemRegionEntryCount();
      long r2Cnt = r2.getSystemRegionEntryCount();
      if (r1Cnt < r2Cnt) {
        return -1;
      } else if (r1Cnt > r2Cnt) {
        return 1;
      } else {
        return 0;
      }
    }
  };

  public JSONObject execute(final HttpServletRequest request) throws Exception {

    String userName = request.getUserPrincipal().getName();

    // get cluster object
    Cluster cluster = Repository.get().getCluster();

    // json object to be sent as response
    JSONObject responseJSON = new JSONObject();

    try {
      // getting cluster's Regions
      responseJSON.put("clusterName", cluster.getServerName());
      responseJSON.put("userName", userName);
      responseJSON.put("region", getRegionJson(cluster));
      responseJSON.put("connectedFlag", cluster.isConnectedFlag());
      responseJSON.put("connectedErrorMsg", cluster.getConnectionErrorMsg());
      // Send json response
      return responseJSON;
    } catch (JSONException e) {
      throw new Exception(e);
    }
  }

  /**
   * This method is used to get various regions associated with the given
   * cluster and create json for each region fields and returns Array List for
   * all the regions associated with given cluster
   * 
   * @param cluster
   * @return JSONObject Array List
   */
  private List<JSONObject> getRegionJson(Cluster cluster) throws JSONException {

    Long totalHeapSize = cluster.getTotalHeapSize();
    Long totalDiskUsage = cluster.getTotalBytesOnDisk();

    Map<String, Cluster.Region> clusterRegions = cluster.getClusterRegions();

    List<Cluster.Region> clusterRegionsList = new ArrayList<Cluster.Region>();
    clusterRegionsList.addAll(clusterRegions.values());

    Collections.sort(clusterRegionsList, regionEntryCountComparator);

    List<JSONObject> regionListJson = new ArrayList<JSONObject>();
    for (int count = 0; count < clusterRegionsList.size(); count++) {
      Cluster.Region reg = clusterRegionsList.get(count);
      JSONObject regionJSON = new JSONObject();

      regionJSON.put("name", reg.getName());
      regionJSON.put("totalMemory", totalHeapSize);

      regionJSON.put("memberCount", reg.getMemberCount());

      final String regionType = reg.getRegionType();
      regionJSON.put("type", regionType);

      // collective rates from region and its column store region
      regionJSON.put("getsRate", (reg.getGetsRate() + reg.getGetsRate_columnStore()));
      regionJSON.put("putsRate", (reg.getPutsRate() + reg.getPutsRate_columnStore()));

      Cluster.Member[] clusterMembersList = cluster.getMembers();

      JSONArray memberNameArray = new JSONArray();
      for (String memberName : reg.getMemberName()) {
        for (Cluster.Member member : clusterMembersList) {
          String name = member.getName();
          name = name.replace(":", "-");
          String id = member.getId();
          id = id.replace(":", "-");

          if ((memberName.equals(id)) || (memberName.equals(name))) {
            JSONObject regionMember = new JSONObject();
            regionMember.put("id", member.getId());
            regionMember.put("name", member.getName());
            memberNameArray.put(regionMember);
            break;
          }
        }
      }

      regionJSON.put("memberNames", memberNameArray);

      // collective entry count from region and its column store region
      regionJSON.put("entryCount", (reg.getSystemRegionEntryCount() + reg.getSystemRegionEntryCount_columnStore()));
      regionJSON.put("systemRegionEntryCount", (reg.getSystemRegionEntryCount() + reg.getRowsInCachedBatches_columnStore()));

      Boolean persistent = reg.getPersistentEnabled();
      if (persistent) {
        regionJSON.put("persistence", this.VALUE_ON);
      } else {
        regionJSON.put("persistence", this.VALUE_OFF);
      }

      Boolean isEnableOffHeapMemory = reg.isEnableOffHeapMemory();
      if (isEnableOffHeapMemory) {
        regionJSON.put("isEnableOffHeapMemory", this.VALUE_ON);
      } else {
        regionJSON.put("isEnableOffHeapMemory", this.VALUE_OFF);
      }

      Boolean isHDFSWriteOnly = reg.isHdfsWriteOnly();
      if (regionType.startsWith("HDFS")) {
        if (isHDFSWriteOnly) {
          regionJSON.put("isHDFSWriteOnly", this.VALUE_ON);
        } else {
          regionJSON.put("isHDFSWriteOnly", this.VALUE_OFF);
        }
      } else {
        regionJSON.put("isHDFSWriteOnly", this.VALUE_NA);
      }

      String regCompCodec = reg.getCompressionCodec();
      if (StringUtils.isNotNullNotEmptyNotWhiteSpace(regCompCodec)) {
        regionJSON.put("compressionCodec", reg.getCompressionCodec());
      } else {
        regionJSON.put("compressionCodec", this.VALUE_NA);
      }

      if (PulseConstants.PRODUCT_NAME_GEMFIREXD.equalsIgnoreCase(PulseController
          .getPulseProductSupport())) {
        // Convert region path to dot separated region path
        regionJSON.put("regionPath",
            StringUtils.getTableNameFromRegionName(reg.getFullPath()));
      } else {
        regionJSON.put("regionPath", reg.getFullPath());
      }

      regionJSON
          .put(
              "memoryReadsTrend",
              new JSONArray(
                  reg.getRegionStatisticTrend(Cluster.Region.REGION_STAT_GETS_PER_SEC_TREND)));
      regionJSON
          .put(
              "memoryWritesTrend",
              new JSONArray(
                  reg.getRegionStatisticTrend(Cluster.Region.REGION_STAT_PUTS_PER_SEC_TREND)));
      regionJSON
          .put(
              "diskReadsTrend",
              new JSONArray(
                  reg.getRegionStatisticTrend(Cluster.Region.REGION_STAT_DISK_READS_PER_SEC_TREND)));
      regionJSON
          .put(
              "diskWritesTrend",
              new JSONArray(
                  reg.getRegionStatisticTrend(Cluster.Region.REGION_STAT_DISK_WRITES_PER_SEC_TREND)));
      regionJSON
          .put(
              "averageReadsTrend",
              new JSONArray(
                  reg.getRegionStatisticTrend(Cluster.Region.REGION_STAT_AVERAGE_READS_TREND)));
      regionJSON
          .put(
              "averageWritesTrend",
              new JSONArray(
                  reg.getRegionStatisticTrend(Cluster.Region.REGION_STAT_AVERAGE_WRITES_TREND)));
      
      regionJSON.put("emptyNodes", reg.getEmptyNode());

      // collective entry size from region and its column store region 
      Long entrySize = reg.getEntrySize() + reg.getEntrySize_columnStore();
      DecimalFormat form = new DecimalFormat(
          PulseConstants.DECIMAL_FORMAT_PATTERN_2);
      String entrySizeInMB = form.format(entrySize / (1024f * 1024f));

      if (entrySize < 0) {
        regionJSON.put(this.ENTRY_SIZE, this.VALUE_NA);
      } else {
        regionJSON.put(this.ENTRY_SIZE, entrySizeInMB);
      }

      // collective Disk Usage from region and its column store region
      regionJSON.put("dataUsage", (reg.getDiskUsage() + reg.getDiskUsage_columnStore()));

      regionJSON.put("wanEnabled", reg.getWanEnabled());
      regionJSON.put("totalDataUsage", totalDiskUsage);

      regionJSON.put("memoryUsage", entrySizeInMB);

      regionListJson.add(regionJSON);
    }

    return regionListJson;
  }
}
