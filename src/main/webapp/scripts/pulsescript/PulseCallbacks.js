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

/**
 *  @overview
 * This is the actual response handling and processing of the web calls
 * that are made by the PulseFunctions class. It will also process the 
 * JSON response.
 *
 * @name PulseCallbacks.js
 * @version 1.0
 */

/**
 * @function
 * 
 * @param r
 *                This is the JSON object that is returned by the AJAX call made
 *                by the PulseFunctions.
 */
var getPulseVersionBack = function(data) {

  if (data.pulseVersion != undefined && data.pulseVersion != "") {
    // Set Pulse version details
    $('#pulseVersion').html(data.pulseVersion);
    $('#pulseVer').html(data.pulseVersion);
    $('#buildId').html(data.buildId);
    $('#buildDate').html(data.buildDate);
    $('#sourceDate').html(data.sourceDate);
    $('#sourceRevision').html(data.sourceRevision);
    $('#sourceRepository').html(data.sourceRepository);

    // Display version details link
    $('#pulseVersionDetailsLink').show();
  } else {
    // Hide version details link
    $('#pulseVersionDetailsLink').hide();
  }
};


/**
 * Default handler for ClusterDetails service,
 * function used for getting the response of Cluster Basic Details and wired
 * them with HTML representing tags.
 */
var getClusterDetailsBack = null;

/**
 * For GemFire, handler for ClusterDetails service
 * function used for getting the response of Cluster Basic Details and wired
 * them with HTML representing tags.
 */
var getClusterDetailsGemFireBack = function(data) {

  if (data.connectedFlag) {
    $('#connectionStatusDiv').hide();
  } else {
    $('#connectionStatusDiv').show();
    $('#connectionErrorMsgDiv').html(data.connectedErrorMsg);
  }

  if (severAlerts.length > 0) { // Severe
    $('#clusterStatusText').html("Severe");
    $("#clusterStatusIcon").addClass("severeStatus");
  } else if (errorAlerts.length > 0) { // Error
    $('#clusterStatusText').html("Error");
    $("#clusterStatusIcon").addClass("errorStatus");
    $("#clusterStatusIcon").removeClass("severeStatus");
  } else if (warningAlerts.length > 0) { // Warning
    $('#clusterStatusText').html("Warning");
    $("#clusterStatusIcon").addClass("warningStatus");
    $("#clusterStatusIcon").removeClass("severeStatus");
    $("#clusterStatusIcon").removeClass("errorStatus");
  } else { // Normal
    $('#clusterStatusText').html("Normal");
    $("#clusterStatusIcon").addClass("normalStatus");
    $("#clusterStatusIcon").removeClass("severeStatus");
    $("#clusterStatusIcon").removeClass("errorStatus");
    $("#clusterStatusIcon").removeClass("warningStatus");
  }

  $('#clusterName').html(data.clusterName);
  $('#clusterTotalMembersText').html(data.totalMembers);
  $('#clusterServersText').html(data.servers);
  $('#clusterClientsText').html(data.clients);
  $('#clusterLocatorsText').html(data.locators);
  $('#clusterTotalRegionsText').html(data.totalRegions);
  $('#clusterTotalHeap').html(data.totalHeap);
  $('#clusterFunctions').html(data.functions);
  $('#clusterUniqueCQs').html(data.uniqueCQs);
  $('#clusterSubscriptionsText').html(data.subscriptions);
  $('#userName').html(data.userName);

};

/**
 * For SnappyData, handler for ClusterDetails service
 * function used for getting the response of Cluster Basic Details and wired
 * them with HTML representing tags.
 */
var getClusterDetailsSnappyDataBack = function(data) {

  if (data.connectedFlag) {
    $('#connectionStatusDiv').hide();
  } else {
    $('#connectionStatusDiv').show();
    $('#connectionErrorMsgDiv').html(data.connectedErrorMsg);
  }

  if (severAlerts.length > 0) { // Severe
    $('#clusterStatusText').html("Severe");
    $("#clusterStatusIcon").addClass("severeStatus");
  } else if (errorAlerts.length > 0) { // Error
    $('#clusterStatusText').html("Error");
    $("#clusterStatusIcon").addClass("errorStatus");
    $("#clusterStatusIcon").removeClass("severeStatus");
  } else if (warningAlerts.length > 0) { // Warning
    $('#clusterStatusText').html("Warning");
    $("#clusterStatusIcon").addClass("warningStatus");
    $("#clusterStatusIcon").removeClass("severeStatus");
    $("#clusterStatusIcon").removeClass("errorStatus");
  } else { // Normal
    $('#clusterStatusText').html("Normal");
    $("#clusterStatusIcon").addClass("normalStatus");
    $("#clusterStatusIcon").removeClass("severeStatus");
    $("#clusterStatusIcon").removeClass("errorStatus");
    $("#clusterStatusIcon").removeClass("warningStatus");
  }

  $('#clusterName').html(data.clusterName);
  $('#clusterTotalMembersText').html(data.totalMembers);
  $('#clusterServersText').html(data.servers);
  $('#clusterClientsText').html(data.clients);
  $('#clusterLocatorsText').html(data.locators);
  $('#clusterTotalRegionsText').html(data.totalRegions);
  $('#clusterTotalHeap').html(data.totalHeap);
  $('#clusterFunctions').html(data.functions);
  $('#clusterTxnCommittedText').html(data.txnCommitted);
  $('#clusterTxnRollbackText').html(data.txnRollback);
  $('#userName').html(data.userName);

};

/**
 * Default handler for ClusterKeyStatistics service,
 * function used for getting the response of Cluster Key Statistics widget and wired
 * them with HTML representing tags.
 */
var getClusterKeyStatisticsBack = null;

/**
 * For GemFire, handler for ClusterKeyStatistics service 
 * function used for getting the response of Cluster Key Statistics widget and
 * wired them with HTML representing tags.
 */
var getClusterKeyStatisticsGemFireBack = function(data) {

  // sparkline graph options
  var sparklineOptions = {
    width : '300px',
    height : '20px',
    lineColor : '#00CCFF',
    fillColor : false,
    spotColor : false,
    minSpotColor : false,
    maxSpotColor : false
  };

  var writePerSecTrend = data.writePerSecTrend;
  var readPerSecTrend = data.readPerSecTrend;
  var queriesPerSecTrend = data.queriesPerSecTrend;

  var sumWrites = 0;
  for ( var i = 0; i < writePerSecTrend.length; i++) {
    sumWrites += writePerSecTrend[i];
  }

  var avgWrites = 0;
  if (writePerSecTrend.length > 0) {
    avgWrites = sumWrites / writePerSecTrend.length;
  }

  var sumReads = 0;
  for ( var i = 0; i < readPerSecTrend.length; i++) {
    sumReads += readPerSecTrend[i];
  }

  var avgReads = 0;
  if (readPerSecTrend.length > 0) {
    avgReads = sumReads / readPerSecTrend.length;
  }

  var sumQueries = 0;
  for ( var i = 0; i < queriesPerSecTrend.length; i++) {
    sumQueries += queriesPerSecTrend[i];
  }

  var avgQueries = 0;
  if (queriesPerSecTrend.length > 0) {
    avgQueries = sumQueries / queriesPerSecTrend.length;
  }
  // write per sec
  $('#writePerSec').html(applyNotApplicableCheck(avgWrites.toFixed(2)));
  $('#writePerSecSparkLine').sparkline(writePerSecTrend, sparklineOptions);

  // read per second
  $('#readPerSec').html(applyNotApplicableCheck(avgReads.toFixed(2)));
  $('#readPerSecSparkLine').sparkline(readPerSecTrend, sparklineOptions);

  // queries per sec
  $('#queriesPerSec').html(avgQueries.toFixed(2));
  $('#queriesPerSecSparkLine').sparkline(queriesPerSecTrend, sparklineOptions);
};

/**
 * For SnappyData, handler for ClusterKeyStatistics service
 * function used for getting the response of Cluster Key Statistics widget and
 * wired them with HTML representing tags.
 */
var getClusterKeyStatisticsSnappyDataBack = function(data) {

  // sparkline graph options
  var sparklineOptions = {
    width : '300px',
    height : '20px',
    lineColor : '#00CCFF',
    fillColor : false,
    spotColor : false,
    minSpotColor : false,
    maxSpotColor : false
  };

  var writePerSecTrend = data.writePerSecTrend;
  var readPerSecTrend = data.readPerSecTrend;
  var queriesPerSecTrend = data.queriesPerSecTrend;

  var sumWrites = 0;
  for ( var i = 0; i < writePerSecTrend.length; i++) {
    sumWrites += writePerSecTrend[i];
  }

  var avgWrites = 0;
  if (writePerSecTrend.length > 0) {
    avgWrites = sumWrites / writePerSecTrend.length;
  }

  var sumReads = 0;
  for ( var i = 0; i < readPerSecTrend.length; i++) {
    sumReads += readPerSecTrend[i];
  }

  var avgReads = 0;
  if (readPerSecTrend.length > 0) {
    avgReads = sumReads / readPerSecTrend.length;
  }

  var sumQueries = 0;
  for ( var i = 0; i < queriesPerSecTrend.length; i++) {
    sumQueries += queriesPerSecTrend[i];
  }

  var avgQueries = 0;
  if (queriesPerSecTrend.length > 0) {
    avgQueries = sumQueries / queriesPerSecTrend.length;
  }
  // write per sec
  $('#writePerSec').html(applyNotApplicableCheck(avgWrites.toFixed(2)));
  $('#writePerSecSparkLine').sparkline(writePerSecTrend, sparklineOptions);

  // read per second
  $('#readPerSec').html(applyNotApplicableCheck(avgReads.toFixed(2)));
  $('#readPerSecSparkLine').sparkline(readPerSecTrend, sparklineOptions);
};

/**
 * function used for getting the response of Cluster WAN Info widget and wired
 * them with HTML representing tags.
 */
var getClusterWANInfoBack = function(data) {
  var clusterList = data.connectedClusters;
  var htmlList = "";
  if (clusterList.length > 0) {
    $('#wanInfoWidget').show();
    for ( var i = 0; i < clusterList.length; i++) {
      if (clusterList[i].status == true) {
        htmlList += '<div class="statusUpDowmBlock statusUp">' + "Cluster_"
            + clusterList[i].name + '</div>';
      } else {
        htmlList += '<div class="statusUpDowmBlock statusDown">' + "Cluster_"
            + clusterList[i].name + '</div>';
      }
    }
    $('#wanInfoList').html(htmlList);
    $('.scroll-pane_1').jScrollPane();
  } else {
    $('#wanInfoWidget').hide();
  }
};

/**
 * function used for getting the response of Cluster Memory Usage widget and
 * wired them with HTML representing tags.
 */
var getClusterMemoryUsageBack = function(data) {

  var memoryUsageTrend = [];
  memoryUsageTrend = data.memoryUsageTrend;

  var sumMemory = 0;
  for ( var i = 0; i < memoryUsageTrend.length; i++) {
    sumMemory += memoryUsageTrend[i];
  }

  var avgMemory = 0;
  if (memoryUsageTrend.length > 0) {
    avgMemory = sumMemory / memoryUsageTrend.length;
  }

  var sparklineOptions = {
    width : '230px',
    height : '72px',
    lineColor : '#FAB948',
    fillColor : false,
    spotRadius : 2.5,
    labelPosition : 'left',
    spotColor : false,
    minSpotColor : false,
    maxSpotColor : false,
    lineWidth : 2
  };

  $('#memoryUsage').sparkline(memoryUsageTrend, sparklineOptions);
  // checking if currentMemoryUsage > 1024 then show the result in GB else
  // show the result in MB
  if (data.currentMemoryUsage > 1024) {
    var memoryUsageGB = (avgMemory / 1024).toFixed(2);
    $('#currentMemoryUsage').html(memoryUsageGB);
    $('#memoryUsageMB').html("GB");

  } else {
    $('#currentMemoryUsage').html(applyNotApplicableCheck(avgMemory.toFixed(2)));
    $('#memoryUsageMB').html("MB");
  }

};

/**
 * function used for getting the response of Cluster GCPauses widget and wired
 * them with HTML representing tags.
 */
var getClusterJVMPausesBack = function(data) {

  var gCPausesTrend = [];
  gCPausesTrend = data.gCPausesTrend;

  var sparklineOptions = {
    width : '230px',
    height : '72px',
    lineColor : '#FAB948',
    fillColor : false,
    spotRadius : 2.5,
    labelPosition : 'left',
    spotColor : false,
    minSpotColor : false,
    maxSpotColor : false,
    lineWidth : 2
  };

  $('#gcPauses').sparkline(gCPausesTrend, sparklineOptions);
  $('#currentGCPauses').html(data.currentGCPauses);
};

/**
 * function used for getting the response of Cluster ThroughputWrites widget and
 * wired them with HTML representing tags.
 */
var getClusterDiskThroughputBack = function(data) {

  var throughputReads = data.throughputReads;
  var throughputWrites = data.throughputWrites;

  var readsSum = 0;
  for ( var i = 0; i < throughputReads.length; i++) {
    readsSum += throughputReads[i];
  }

  var avgThroughputReads = 0;
  if (throughputReads.length > 0) {
    avgThroughputReads = readsSum / throughputReads.length;
  }

  var writesSum = 0;
  for ( var i = 0; i < throughputWrites.length; i++) {
    writesSum += throughputWrites[i];
  }

  var avgThroughputWrites = 0;
  if (throughputWrites.length > 0) {
    avgThroughputWrites = writesSum / throughputWrites.length;
  }

  var sparklineOptions = {
    width : '230px',
    height : '72px',
    lineColor : '#FAB948',
    lineWidth : 2,
    fillColor : false,
    spotRadius : 3,
    spotColor : false,
    minSpotColor : false,
    maxSpotColor : false,
    highlightSpotColor : false,
    composite : false
  };

  // Reads trends
  // sparklineOptions.lineColor = '#87b025';
  $('#diskReadsThroughputTrend').sparkline(throughputReads, sparklineOptions);
  $('#diskReadsThroughput').html(applyNotApplicableCheck(avgThroughputReads.toFixed(2)));

  // Writes trends
  sparklineOptions.lineColor = '#2e84bb';
  $('#diskWritesThroughputTrend').sparkline(throughputWrites, sparklineOptions);
  $('#diskWritesThroughput').html(applyNotApplicableCheck(avgThroughputWrites.toFixed(2)));
};

/**
 * function used for getting the response of Member Clients widget and wired
 * them with HTML representing tags.
 */
var getMemberClientsBack = function(data) {
  $('#memberClientsList').jqGrid('clearGridData');
  var clients = data.memberClients;
  if (clients.length > 0) {
    $('#memberClientsWidget').show();
    for ( var i = 0; i < clients.length; i++) {
      var clientData = clients[i];
      $('#memberClientsList').jqGrid('addRowData', i + 1, clientData);
    }

    var gridUserData = jQuery("#memberClientsList").getGridParam('userData');

    // Apply sort order ans sort columns on updated jqgrid data
    jQuery("#memberClientsList").jqGrid('setGridParam', {
      sortname : gridUserData.sortColName,
      sortorder : gridUserData.sortOrder
    });
    // Reload jqgrid
    jQuery("#memberClientsList").trigger("reloadGrid");

  } else {
    $('#memberClientsWidget').hide();
  }
};

/**
 * function used for getting the response of Member Basic Details widget and wired
 * them with HTML representing tags.
 */
var getMemberDetailsBack = function(data) {
  if (data.connectedFlag)
    $('#connectionStatusDiv').hide();
  else {
    $('#connectionStatusDiv').show();
    $('#connectionErrorMsgDiv').html(data.connectedErrorMsg);
  }

  // check if data for member is available or not
  if (data.errorOnMember != undefined && data.errorOnMember != null
      && $.trim(data.errorOnMember) != "") {

    // Display error message
    var message = data.errorOnMember;
    message += "<div class=\"clear\"></div>"
               + "<a onclick=\"openClusterDetail();\" href=\"#.\" >" 
               + "Go To Cluster View</a>";
    $('#connectionErrorMsgDiv').html(message);
    $('#connectionStatusDiv').show();
    return;
  }

  $('#userName').html(data.userName);
  $('#clusterNameLink').html(data.clusterName);
  $('#memberName').html(data.name);
  $('#memberName').prop('title', data.name);
  $('#memberRegionsCount').html(data.regionsCount);
  $('#loadAverage').html(applyNotApplicableCheck(data.loadAverage));
  //$('#sockets').html(applyNotApplicableCheck(data.sockets));
  $('#openFDs').html(applyNotApplicableCheck(data.openFDs));
  $('#threads').html(data.threads);

  // off heap free size
  var offHeapFreeSize = convertBytesToMBorGB(data.offHeapFreeSize);
  $('#offHeapFreeSize').html(offHeapFreeSize[0]+" "+offHeapFreeSize[1]);

  // off heap used size
  var offHeapUsedSize = convertBytesToMBorGB(data.offHeapUsedSize);
  $('#offHeapUsedSize').html(offHeapUsedSize[0]+" "+offHeapUsedSize[1]);

  $('#numClients').html(data.numClients);
  
  // setting member status according to notification alert
  $('#memberStatus').html(data.status);
  if (data.status == "Severe") { // Severes
    if ($("#memberStatusIcon").hasClass("normalStatus"))
      $("#memberStatusIcon").removeClass("normalStatus");
    if ($("#memberStatusIcon").hasClass("errorStatus"))
      $("#memberStatusIcon").removeClass("errorStatus");
    if ($("#memberStatusIcon").hasClass("warningStatus"))
      $("#memberStatusIcon").removeClass("warningStatus");

    $('#memberStatus').html("Severe");
    $("#memberStatusIcon").addClass("severeStatus");

  } else if (data.status == "Error") { // Error

    if ($("#memberStatusIcon").hasClass("normalStatus")) {
      $("#memberStatusIcon").removeClass("normalStatus");
    }
    if ($("#memberStatusIcon").hasClass("warningStatus")) {
      $("#memberStatusIcon").removeClass("warningStatus");
    }
    $("#memberStatusIcon").addClass("errorStatus");
    $('#memberStatus').html("Error");
  } else if (data.status == "Warning") { // Warning

    if ($("#memberStatusIcon").hasClass("normalStatus")) {
      $("#memberStatusIcon").removeClass("normalStatus");
    }
    if ($("#memberStatusIcon").hasClass("errorStatus")) {
      $("#memberStatusIcon").removeClass("errorStatus");
    }
    if ($("#memberStatusIcon").hasClass("severeStatus")) {
      $("#memberStatusIcon").removeClass("severeStatus");
    }
    $('#memberStatus').html("Warning");
    $("#memberStatusIcon").addClass("warningStatus");
  } else { // Normal

    if ($("#memberStatusIcon").hasClass("severeStatus"))
      $("#memberStatusIcon").removeClass("severeStatus");
    if ($("#memberStatusIcon").hasClass("errorStatus"))
      $("#memberStatusIcon").removeClass("errorStatus");
    if ($("#memberStatusIcon").hasClass("warningStatus"))
      $("#memberStatusIcon").removeClass("warningStatus");
    $('#memberStatus').html("Normal");
    $("#memberStatusIcon").addClass("normalStatus");
  }
  
};

/**
 * function used for getting the response of Member KeyStatistics widget and
 * wired them with HTML representing tags.
 */
var getMemberKeyStatisticsBack = function(data) {
  // sparkline graph options
  var sparklineOptions = {
    width : '300px',
    height : '20px',
    lineColor : '#00CCFF',
    fillColor : false,
    spotColor : false,
    minSpotColor : false,
    maxSpotColor : false
  };

  // cpu usage
  var sumCPUUsage = 0;
  var cpuUsageTrend = data.cpuUsageTrend;

  for ( var i = 0; i < cpuUsageTrend.length; i++) {
    sumCPUUsage += cpuUsageTrend[i];
  }

  var avgCPUUsage = 0;
  if (cpuUsageTrend.length > 0) {
    avgCPUUsage = sumCPUUsage / cpuUsageTrend.length;
  }

  $('#memberCPUUsageValue').html(avgCPUUsage.toFixed(2));
  $('#memberCPUUsageSparkLine').sparkline(data.cpuUsageTrend, sparklineOptions);

  // gets per sec
  var readPerSecTrend = data.readPerSecTrend;
  var sumReads = 0;
  for ( var i = 0; i < readPerSecTrend.length; i++) {
    sumReads += readPerSecTrend[i];
  }

  var avgReads = 0;
  if (readPerSecTrend.length > 0) {
    avgReads = sumReads / readPerSecTrend.length;
  }

  $('#memberGetsPerSecValue').html(applyNotApplicableCheck(avgReads.toFixed(2)));
  $('#memberGetsPerSecSparkLine').sparkline(data.readPerSecTrend,
      sparklineOptions);

  // puts per sec
  var writePerSecTrend = data.writePerSecTrend;
  var sumWrites = 0;
  for ( var i = 0; i < writePerSecTrend.length; i++) {
    sumWrites += writePerSecTrend[i];
  }

  var avgWrites = 0;
  if (writePerSecTrend.length > 0) {
    avgWrites = sumWrites / writePerSecTrend.length;
  }

  $('#memberPutsPerSecValue').html(applyNotApplicableCheck(avgWrites.toFixed(2)));
  $('#memberPutsPerSecSparkLineBar').sparkline(data.writePerSecTrend,
      sparklineOptions);
};

/**
 * function used for getting the response of Member GCPauses widget and wired
 * them with HTML representing tags.
 */
var getMemberGCPausesBack = function(data) {

  gcPausesTrend = data.gcPausesTrend;

  var sparklineOptions = {
    width : '230px',
    height : '72px',
    lineColor : '#FAB948',
    fillColor : false,
    spotRadius : 2.5,
    labelPosition : 'left',
    spotColor : false,
    minSpotColor : false,
    maxSpotColor : false,
    lineWidth : 2
  };

  $('#memberGCPauses').sparkline(gcPausesTrend, sparklineOptions);
  $('#memberGcPausesAvg').html(data.gcPausesCount);
};

/**
 * function used for getting the response of Member HeapUsage widget and wired
 * them with HTML representing tags.
 */
var getMemberHeapUsageBack = function(data) {
  var heapUsageTrend = [];
  heapUsageTrend = data.heapUsageTrend;
  var sumMemory = 0;
  for ( var i = 0; i < heapUsageTrend.length; i++) {
    sumMemory += heapUsageTrend[i];
  }

  var avgMemory = 0;
  if (heapUsageTrend.length > 0) {
    avgMemory = sumMemory / heapUsageTrend.length;
  }

  var sparklineOptions = {
    width : '230px',
    height : '72px',
    lineColor : '#FAB948',
    fillColor : false,
    spotRadius : 2.5,
    labelPosition : 'left',
    spotColor : false,
    minSpotColor : false,
    maxSpotColor : false,
    lineWidth : 2
  };

  $('#memberHeapUsage').sparkline(heapUsageTrend, sparklineOptions);
  // set current value
  if (avgMemory > 1024) {
    var heapUsageGB = (avgMemory / 1024).toFixed(2);
    $('#memberHeapUsageAvg').html(
        heapUsageGB + ' <span class="font-size23 paddingL5">GB</span>');

  } else {
    $('#memberHeapUsageAvg')
        .html(
            applyNotApplicableCheck(avgMemory.toFixed(2))
                + ' <span class="font-size23 paddingL5">MB</span>');
  }
};

/**
 * function used for getting the response of Member TroughputWrites widget and
 * wired them with HTML representing tags.
 */
var getMemberDiskThroughputBack = function(data) {
  var throughputReadsTrend = data.throughputReadsTrend;
  var throughputWritesTrend = data.throughputWritesTrend;

  var readsSum = 0;
  for ( var i = 0; i < throughputReadsTrend.length; i++) {
    readsSum += throughputReadsTrend[i];
  }

  var avgThroughputReads = 0;
  if (throughputReadsTrend.length > 0) {
    avgThroughputReads = readsSum / throughputReadsTrend.length;
  }

  var writesSum = 0;
  for ( var i = 0; i < throughputWritesTrend.length; i++) {
    writesSum += throughputWritesTrend[i];
  }

  var avgThroughputWrites = 0;
  if (throughputWritesTrend.length > 0) {
    avgThroughputWrites = writesSum / throughputWritesTrend.length;
  }

  var sparklineOptions = {
    width : '230px',
    height : '72px',
    lineColor : '#FAB948',
    lineWidth : 2,
    fillColor : false,
    spotRadius : 3,
    spotColor : false,
    minSpotColor : false,
    maxSpotColor : false,
    highlightSpotColor : false,
    composite : false
  };

  // Reads trends
  // sparklineOptions.lineColor = '#87b025';
  $('#diskReadsThroughputTrend').sparkline(throughputReadsTrend,
      sparklineOptions);
  $('#diskReadsThroughput').html(applyNotApplicableCheck(avgThroughputReads.toFixed(2)));

  // Writes trends
  sparklineOptions.lineColor = '#2e84bb';
  $('#diskWritesThroughputTrend').sparkline(throughputWritesTrend,
      sparklineOptions);
  $('#diskWritesThroughput').html(applyNotApplicableCheck(avgThroughputWrites.toFixed(2)));

};

/**
 * function used for getting the response of Member GatewayHub widget and wired
 * them with HTML representing tags.
 */
var getMemberGatewayHubBack = function(data) {

  var gatewayDetails = data;

  if ((!data.isGatewayReceiver) && (!data.isGatewaySender)) {
    // hide widget
    $('#MemberGatewayHubWidget').hide();
    return;
  } else {
    // show widget
    $('#MemberGatewayHubWidget').show();
  }

  if (data.isGatewayReceiver) {
    if (data.listeningPort != undefined && data.listeningPort != "") {
      $('#receiverListeningPort').html(data.listeningPort);
    } else {
      $('#receiverListeningPort').html('-');
    }

    if (data.linkTroughput != undefined && data.linkTroughput != "") {
      $('#receiverLinkThroughput').html(data.linkTroughput);
    } else {
      $('#receiverLinkThroughput').html('-');
    }

    if (data.avgBatchLatency != undefined && data.avgBatchLatency != "") {
      $('#receiverAvgBatchLatency').html(
          data.avgBatchLatency
              + ' <span class="font-size13 paddingL5">ms</span>');
    } else {
      $('#receiverAvgBatchLatency').html('-');
    }
  } else {
    $('#receiverListeningPort').html('-');
    $('#receiverLinkThroughput').html('-');
    $('#receiverAvgBatchLatency').html('-');
  }

  if (data.isGatewaySender) {

    var endPoints = gatewayDetails.gatewaySenders;
    var htmlListEndPoints = "";

    for ( var i = 0; i < endPoints.length; i++) {
      htmlListEndPoints += '<div>'
          + '<span class="pointDetailsPaddingMember pointC1" title="'
          + endPoints[i].id + '">' + endPoints[i].id + '</span>'
          + '<span class="pointDetailsPaddingMember pointC2" title="'
          + endPoints[i].queueSize + '">' + endPoints[i].queueSize + '</span>';
      if (endPoints[i].status == true) {
        htmlListEndPoints += '<span class="pointDetailsPaddingMember pointC3"><img src="images/status-up.png"></span>';
      } else if (endPoints[i].status == false) {
        htmlListEndPoints += '<span class="pointDetailsPaddingMember pointC3"><img src="images/status-down.png"></span>';
      }
      htmlListEndPoints += '</div>';
    }
    $('#gatewaySendersList').html(htmlListEndPoints);

    var regionList = gatewayDetails.regionsInvolved;
    var htmlListRegions = "";
    for ( var i = 0; i < regionList.length; i++) {
      htmlListRegions += '<div> <span class="pointDetailsPaddingMember pointC1-2">'
          + regionList[i].name
          + '</span><span class="pointDetailsPaddingMember pointC3"></div>';
    }
    $('#regionInvolvedList').html(htmlListRegions);

    $('.pointGridData').jScrollPane();

  }
};

var getClearAllAlertsBack = function(data) {
  numTotalSeverAlerts = 0;
  numTotalErrorAlerts = 0;
  numTotalWarningAlerts = 0;

  severAlerts.splice(0, severAlerts.length);
  errorAlerts.splice(0, errorAlerts.length);
  warningAlerts.splice(0, warningAlerts.length);

  document.getElementById("severeList").innerHTML = "";
  document.getElementById("severeTotalCount").innerHTML = "";
  document.getElementById("severeTotalCount").innerHTML = "0";

  document.getElementById("errorList").innerHTML = "";
  document.getElementById("errorTotalCount").innerHTML = "";
  document.getElementById("errorTotalCount").innerHTML = "0";

  document.getElementById("warningList").innerHTML = "";
  document.getElementById("warningTotalCount").innerHTML = "";
  document.getElementById("warningTotalCount").innerHTML = "0";

  document.getElementById("allAlertList").innerHTML = "";
  document.getElementById("allAlertCount").innerHTML = "";
  document.getElementById("allAlertCount").innerHTML = "0";

  // Hide Load More
  $('#containerLoadMoreAlertsLink').hide();

  // update dashboard stats
  displayClusterStatus();

  if (!(document.getElementById("memberStatusIcon") == null)) {
    $('#memberStatus').html("Normal");
    $("#memberStatusIcon").addClass("normalStatus");

    if ($("#memberStatusIcon").hasClass("severeStatus"))
      $("#memberStatusIcon").removeClass("severeStatus");
    if ($("#memberStatusIcon").hasClass("errorStatus"))
      $("#memberStatusIcon").removeClass("errorStatus");
    if ($("#memberStatusIcon").hasClass("warningStatus"))
      $("#memberStatusIcon").removeClass("warningStatus");
  }
  $("#allAlertScrollPane").addClass("hide-scroll-pane");
};

function getSystemAlertsBack(data) {

  $('#pageNumber').val(data.pageNumber);

  // Update global alerts variables
  severAlerts = new Array();
  if(undefined != data.systemAlerts.severe){
    severAlerts = data.systemAlerts.severe;
  }

  errorAlerts = new Array();
  if(undefined != data.systemAlerts.errors){
    errorAlerts = data.systemAlerts.errors;
  }

  warningAlerts = new Array();
  if(undefined != data.systemAlerts.warnings){
    warningAlerts = data.systemAlerts.warnings;
  }

  infoAlerts = new Array();
  if(undefined != data.systemAlerts.info){
    infoAlerts = data.systemAlerts.info;
  }
  
  // Apply filter if text criteria is already specified by user on ui
  applyFilterOnNotificationsList(currentActiveNotificationTab);
  
};

/**
 * function used for getting the response of Cluster R Graph widget and wired
 * them with HTML representing tags.
 */
var getClusterMembersRGraphBack = function(data) {
  // if member count is < 50 then make R-Graph as default view
  if ("" == flagActiveTab && data.memberCount <= 50) {
    flagActiveTab = "MEM_R_GRAPH";
    tabGraph();
  } else if ("" == flagActiveTab && data.memberCount > 50
      && data.memberCount <= 100) { // if member count is > 50 and <=100
    // then
    // make Tree map as default view
    flagActiveTab = "MEM_TREE_MAP";
    tabTreeMap();
  } else if ("" == flagActiveTab && data.memberCount > 100) { // if member
    // count is >
    // 100 then make
    // grid as
    // default view
    flagActiveTab = "MEM_GRID";
    tabClusterGrid();
  }
  if (flagActiveTab == "MEM_R_GRAPH") // if
  {
    clusteRGraph.loadJSON(data.clustor);
    clusteRGraph.compute('end');
    if (vMode != 8)
      refreshNodeAccAlerts();
    clusteRGraph.refresh();
  }
};

/* builds and returns json from given members details sent by server */
function buildTreeMapJSON(members) {

  var childerensVal = [];
  for ( var i = 0; i < members.length; i++) {

    var color = "#a0c44a";
    // setting node color according to the status of member
    // like if member has severe notification then the node color will be
    // '#ebbf0f'
    for ( var j = 0; j < warningAlerts.length; j++) {
      if (members[i].name == warningAlerts[j].memberName) {
        color = '#ebbf0f';
        break;
      }
    }
    // if member has severe notification then the node color will be
    // '#de5a25'
    for ( var j = 0; j < errorAlerts.length; j++) {
      if (members[i].name == errorAlerts[j].memberName) {
        color = '#de5a25';
        break;
      }
    }
    // if member has severe notification then the node color will be
    // '#b82811'
    for ( var j = 0; j < severAlerts.length; j++) {
      if (members[i].name == severAlerts[j].memberName) {
        color = '#b82811';
        break;
      }
    }
    var heapSize = members[i].currentHeapUsage;
    // if (heapSize == 0)
    // heapSize = 1;
    var name = "";
    name = members[i].name;
    var id = "";
    id = members[i].memberId;
    // passing all the required information of member to tooltip
    var dataVal = {
      "name" : name,
      "id" : id,
      "$color" : color,
      "$area" : heapSize,
      "cpuUsage" : members[i].cpuUsage,
      "heapUsage" : members[i].currentHeapUsage,
      "loadAvg" : members[i].loadAvg,
      "threads" : members[i].threads,
      "sockets" : members[i].sockets,
      "openFDs" : members[i].openFDs,
      "initial" : false
    };
    var childrenVal = {
      "children" : [],
      "data" : dataVal,
      "id" : id,
      "name" : name
    };
    childerensVal[i] = childrenVal;
  }
  var localjson = {
    "children" : childerensVal,
    "data" : {},
    "id" : "root",
    "name" : "Members"
  };
  return localjson;
}

var getClusterMembersBack = function(data) {

  var members = data.members;

  memberCount = members.length;

  if (flagActiveTab == "MEM_TREE_MAP") {
    updateClusterMembersTreeMap(data);
  } else if (flagActiveTab == "MEM_GRID") {
    updateClusterMembersGrid(data);
  } else {
    globalJson = buildTreeMapJSON(members);
  }
};

/**
 * function used for getting the response of Cluster Member Tree Map widget and
 * wired them with HTML representing tags.
 */
var updateClusterMembersTreeMap = function(data) {
  var members = data.members;

  memberCount = members.length;

  var json = buildTreeMapJSON(members);

  clusterMemberTreeMap.loadJSON(json);
  clusterMemberTreeMap.refresh();
};

var updateClusterMembersGrid = function(data) {
  var members = data.members;

  memberCount = members.length;

  $('#memberList').jqGrid('clearGridData');

  for ( var i = 0; i < members.length; i++) {
    var memberData = members[i];
    $('#memberList').jqGrid('addRowData',
        memberData.memberId + "&" + memberData.name, memberData);
  }

  var gridUserData = jQuery("#memberList").getGridParam('userData');

  // Apply sort order ans sort columns on updated jqgrid data
  jQuery("#memberList").jqGrid('setGridParam', {
    sortname : gridUserData.sortColName,
    sortorder : gridUserData.sortOrder
  });
  // Reload jqgrid
  jQuery("#memberList").trigger("reloadGrid");

  // apply scroll if grid container block is not minimized
  if ($("#LargeBlock_1").css("display") != "none") {
    $('.ui-jqgrid-bdiv').jScrollPane();
  }
};

// function used for applying filter of cluster regions in data view screen
var applyFilterOnClusterRegions = function() {
  // console.log("applyFilterOnClusterRegions called");
  var searchKeyword = extractFilterTextFrom("filterClusterRegionsBox");

  if (searchKeyword != "") {
    var filteredClusterRegions = new Array();
    for ( var i = 0; i < clusterDataViewRegions.length; i++) {
      // filtered list
      if (clusterDataViewRegions[i].name.toLowerCase().indexOf(searchKeyword) !== -1) {
        filteredClusterRegions.push(clusterDataViewRegions[i]);
      }
    }
    updateDataViewDetails(filteredClusterRegions);
  } else {
    updateDataViewDetails(clusterDataViewRegions);
  }
};

// update the cluster regions grid and treemap view on data view page
function updateDataViewDetails(clusterRegions) {

  $('#regionList').jqGrid('clearGridData');
  jQuery("#regionList").trigger("reloadGrid");

  // variable to store value of total of entry counts of all regions
  var totalOfEntryCounts = 0;
  // flag to determine if all regions are having entry count = 0
  var flagSetEntryCountsToZero = false;

  // Calculate the total of all regions entry counts
  for ( var i = 0; i < clusterRegions.length; i++) {
    totalOfEntryCounts += clusterRegions[i].entryCount;
  }

  // If totalOfEntryCounts is zero and at least one region is present
  // then set flagSetEntryCountsToZero to avoid displaying circles
  // in treemap as all valid regions are zero area regions and also display
  // all regions with evenly placement of blocks
  if (totalOfEntryCounts == 0 && clusterRegions.length > 0) {
    flagSetEntryCountsToZero = true;
  }

  var childerensVal = [];
  if (clusterRegions.length > 0) {

    if (selectedDataViewTM == "") {
      selectedDataViewTM = clusterRegions[0].regionPath;
    } else {
      // check selected region exists or not
      // if selected region does not exists, set first region as selected
      // region
      var selectedRegionExists = false;
      for ( var i = 0; i < clusterRegions.length; i++) {
        if (selectedDataViewTM == clusterRegions[i].regionPath) {
          selectedRegionExists = true;
        }
      }
      if (!selectedRegionExists) {
        selectedDataViewTM = clusterRegions[0].regionPath;
      }
    }

    for ( var i = 0; i < clusterRegions.length; i++) {

      $('#regionList').jqGrid('addRowData', i + 1, clusterRegions[i]);
      var entryCount = clusterRegions[i].systemRegionEntryCount;
      // If flagSetEntryCountsToZero is true then set entry count to
      // display all
      // regions with evenly placement of blocks
      if (flagSetEntryCountsToZero && entryCount == 0) {
        entryCount = 1;
      }
      var color = colorCodeForRegions;
      if(clusterRegions[i].systemRegionEntryCount == 0){
        color = colorCodeForZeroEntryCountRegions;
      }

      if (selectedDataViewTM == clusterRegions[i].regionPath)
        color = colorCodeForSelectedRegion;

      var wanEnabled = clusterRegions[i].wanEnabled;
      var wanEnabledxt = "";
      if (wanEnabled == true)
        wanEnabledxt = "WAN Enabled";
      // if (entryCount == 0)
      // entryCount = 1;
      var dataVal = {
        "name" : clusterRegions[i].name,
        "id" : clusterRegions[i].regionPath,
        "$color" : color,
        "$area" : entryCount,
        "systemRegionEntryCount" : clusterRegions[i].systemRegionEntryCount,
        "type" : clusterRegions[i].type,
        "regionPath" : clusterRegions[i].regionPath,
        "entrySize" : clusterRegions[i].entrySize,
        "memberCount" : clusterRegions[i].memberCount,
        "writes" : clusterRegions[i].putsRate,
        "reads" : clusterRegions[i].getsRate,
        "emptyNodes" : clusterRegions[i].emptyNodes,
        "persistence" : clusterRegions[i].persistence,
        "isEnableOffHeapMemory" : clusterRegions[i].isEnableOffHeapMemory,
        "compressionCodec" : clusterRegions[i].compressionCodec,
        "isHDFSWriteOnly" : clusterRegions[i].isHDFSWriteOnly,
        "memberNames" : clusterRegions[i].memberNames,
        "memoryWritesTrend" : clusterRegions[i].memoryWritesTrend,
        "memoryReadsTrend" : clusterRegions[i].memoryReadsTrend,
        "diskWritesTrend" : clusterRegions[i].diskWritesTrend,
        "diskReadsTrend" : clusterRegions[i].diskReadsTrend,
        "averageWritesTrend" : clusterRegions[i].averageWritesTrend,
        "averageReadsTrend" : clusterRegions[i].averageReadsTrend,
        "memoryUsage" : clusterRegions[i].memoryUsage,
        "dataUsage" : clusterRegions[i].dataUsage,
        "totalDataUsage" : clusterRegions[i].totalDataUsage,
        "totalMemory" : clusterRegions[i].totalMemory
      };
      var childrenVal = {
        "children" : [],
        "data" : dataVal,
        "id" : clusterRegions[i].regionPath,
        "name" : wanEnabledxt
      };
      if (selectedDataViewTM == clusterRegions[i].regionPath)
        displayRegionDetails(dataVal);
      childerensVal[i] = childrenVal;
    }

  } else {
    var dataVal = {
      "name" : "",
      "id" : "",
      "$color" : "",
      "$area" : 0,
      "systemRegionEntryCount" : "",
      "type" : "",
      "regionPath" : "",
      "entrySize" : "",
      "memberCount" : "",
      "writes" : "",
      "reads" : "",
      "emptyNodes" : "",
      "persistence" : "",
      "isEnableOffHeapMemory" : "",
      "compressionCodec" : "",
      "isHDFSWriteOnly" : "",
      "memberNames" : "",
      "memoryWritesTrend" : "",
      "memoryReadsTrend" : "",
      "diskWritesTrend" : "",
      "diskReadsTrend" : "",
      "averageWritesTrend" : "",
      "averageReadsTrend" : "",
      "memoryUsage" : 0,
      "dataUsage" : "",
      "totalDataUsage" : "",
      "totalMemory" : 0
    };
    displayRegionDetails(dataVal);
  }

  var json = {
    "children" : childerensVal,
    "data" : {},
    "id" : "root",
    "name" : "Regions"
  };

  var gridUserData = jQuery("#regionList").getGridParam('userData');

  // Apply sort order ans sort columns on updated jqgrid data
  jQuery("#regionList").jqGrid('setGridParam', {
    sortname : gridUserData.sortColName,
    sortorder : gridUserData.sortOrder
  });
  // Reload jqgrid
  jQuery("#regionList").trigger("reloadGrid");
  $('.ui-jqgrid-bdiv').jScrollPane();

  // Load treemap
  dataViewRegionTM.loadJSON(json);
  dataViewRegionTM.refresh();

}

// Callback function for ClusterRegion serice
var getClusterRegionBack = function(data) {
  if (data.connectedFlag)
    $('#connectionStatusDiv').hide();
  else {
    $('#connectionStatusDiv').show();
    $('#connectionErrorMsgDiv').html(data.connectedErrorMsg);
  }

  $('#userName').html(data.userName);
  document.getElementById("clusterName").innerHTML = data.clusterName;
  clusterDataViewRegions = data.region;
  // aaply filter and update data view details
  applyFilterOnClusterRegions();
};

// function used for applying filter of member names in data view screen
var applyFilterOnMembersList = function() {
  var searchKeyword = extractFilterTextFrom('filterMembersListBox');
  var htmlMemberListWithFilter = '';
  if (searchKeyword != "") {
    for ( var i = 0; i < memberList.length; i++) {
      // filtered list
      if (memberList[i].name.toLowerCase().indexOf(searchKeyword) !== -1) {
        var divId = memberList[i].id + "&" + memberList[i].name;
        htmlMemberListWithFilter += "<div class='pointDetailsPadding' title='"
            + memberList[i].name + "' id='" + divId
            + "' onClick = 'javascript:openMemberDetails(this.id)'>"
            + memberList[i].name + "</div>";
      }
    }
  } else {
    for ( var i = 0; i < memberList.length; i++) {
      // non filtered list
      var divId = memberList[i].id + "&" + memberList[i].name;
      htmlMemberListWithFilter += "<div class='pointDetailsPadding' title='"
          + memberList[i].name + "' id='" + divId
          + "' onClick = 'javascript:openMemberDetails(this.id)'>"
          + memberList[i].name + "</div>";
    }
  }
  document.getElementById("memberNames").innerHTML = htmlMemberListWithFilter;
  $('.regionMembersSearchBlock').jScrollPane();
};

// function used for redirecting to member details page on click of any member
// name from
// the member names list.
var openMemberDetails = function(divId) {
  var member = divId.split("&");
  location.href = 'MemberDetails.html?member=' + member[0] + '&memberName='
      + member[1];
};

// function used for applying filter of member regions in member details page
var applyFilterOnMemberRegions = function() {
  // console.log("applyFilterOnMemberRegions called");
  var searchKeyword = extractFilterTextFrom("filterMemberRegionsBox");

  if (searchKeyword != "") {
    var filteredMemberRegions = new Array();
    for ( var i = 0; i < memberRegions.length; i++) {
      // filtered list
      if (memberRegions[i].name.toLowerCase().indexOf(searchKeyword) !== -1) {
        filteredMemberRegions.push(memberRegions[i]);
      }
    }
    updateMemberRegionViewDetails(filteredMemberRegions);
  } else {
    updateMemberRegionViewDetails(memberRegions);
  }
};

// update the member regions grid and treemap view on members details page
function updateMemberRegionViewDetails(regions) {

  $('#memberRegionsList').jqGrid('clearGridData');
  jQuery("#memberRegionsList").trigger("reloadGrid");

  var childerensVal = [];

  // variable to store value of total of entry counts of all regions
  var totalOfEntryCounts = 0;
  // flag to determine if all regions are having entry count = 0
  var flagSetEntryCountsToZero = false;

  // Calculate the total of all regions entry counts
  for ( var i = 0; i < regions.length; i++) {
    totalOfEntryCounts += regions[i].entryCount;
  }

  // If totalOfEntryCounts is zero and at least one region is present
  // then set flagSetEntryCountsToZero to avoid displaying circles
  // in treemap as all valid regions are zero area regions and also display
  // all regions with evenly placement of blocks
  if (totalOfEntryCounts == 0 && regions.length > 0) {
    flagSetEntryCountsToZero = true;
  }

  for ( var i = 0; i < regions.length; i++) {
    var regionData = regions[i];
    $('#memberRegionsList').jqGrid('addRowData', i + 1, regionData);
    var name = "";
    name = regions[i].name;
    var regionFullPath = regions[i].fullPath;
    var entryCount = regions[i].entryCount;
    // If flagSetEntryCountsToZero is true then set entry count to display
    // all
    // regions with evenly placement of blocks
    if (flagSetEntryCountsToZero && entryCount == 0) {
      entryCount = 1;
    }
    // if(entryCount == 0)
    // entryCount = 1;

    var colorCode = colorCodeForRegions;
    if(regions[i].entryCount == 0){
      colorCode = colorCodeForZeroEntryCountRegions;
    }

    var dataVal = {
      "name" : name,
      "id" : regionFullPath,
      "$color" : colorCode,
      "$area" : entryCount,
      "regionType" : regions[i].type,
      "entryCount" : regions[i].entryCount,
      "entrySize" : regions[i].entrySize
    };
    var childrenVal = {
      "children" : [],
      "data" : dataVal,
      "name" : name,
      "id" : regionFullPath
    };
    childerensVal[i] = childrenVal;
  }

  var gridUserData = jQuery("#memberRegionsList").getGridParam('userData');

  // Apply sort order ans sort columns on updated jqgrid data
  jQuery("#memberRegionsList").jqGrid('setGridParam', {
    sortname : gridUserData.sortColName,
    sortorder : gridUserData.sortOrder
  });
  // Reload jqgrid
  jQuery("#memberRegionsList").trigger("reloadGrid");
  // apply scroll if grid container block is not minimized
  if ($("#LargeBlock_1").css("display") != "none") {
    $('.ui-jqgrid-bdiv').jScrollPane();
  }

  if (regions.length > 0) {
    var json = {
      "children" : childerensVal,
      "data" : {},
      "name" : "Regions",
      "id" : "Root"
    };

    memberRegionsTreeMap.loadJSON(json);
    memberRegionsTreeMap.refresh();
  } else {
    var json = {
      "children" : [],
      "data" : {},
      "name" : "Regions",
      "id" : "Root"
    };
    memberRegionsTreeMap.loadJSON(json);
    memberRegionsTreeMap.refresh();
  }

}

// Callback function for MemberRegions serice
var getMemberRegionsBack = function(data) {

  $('#memberRegionsList').jqGrid('clearGridData');

  memberRegions = data.memberRegions;
  // apply filter and update region details
  applyFilterOnMemberRegions();
};

/**
 * function used for getting the response of Members Name List widget and wired
 * them with HTML representing tags.
 */
var getMembersListBack = function(data) {

  // Update global list of cluster members
  membersList = data.clusterMembers;

  // update tabname
  $('#clusterNameLink').html(data.clusterName);

  // add filter functionality
  if (!isMemberListFilterHandlerBound) {
    $('#filterMembersBox').bind("keyup", applyFilterOnMembersListDropDown);
    isMemberListFilterHandlerBound = true;
  }

  // apply filter criteria if exist
  $('#filterMembersBox').keyup();

};

var getQueryStatisticsBack = function(data) {
  
  // each refresh display jmx connectiopn status
  if (data.connectedFlag) {
    $('#connectionStatusDiv').hide();
  } else {
    $('#connectionStatusDiv').show();
    $('#connectionErrorMsgDiv').html(data.connectedErrorMsg);
  }
  
  //load the data
  $('#queryStatisticsList').jqGrid('clearGridData');
  if (data.queriesList.length > 0) {

    // load all queries
    grid.jqGrid("getGridParam").data = data.queriesList;
    
    var gridUserData = jQuery("#queryStatisticsList").getGridParam('userData');
    // Apply sort order ans sort columns on updated jqgrid data
    jQuery("#queryStatisticsList").jqGrid('setGridParam', {
      sortname : gridUserData.sortColName,
      sortorder : gridUserData.sortOrder
    });
    
    // Reload jqgrid
    jQuery("#queryStatisticsList").trigger("reloadGrid");
    var queryStatisticsList = $('#gview_queryStatisticsList');
    var queryStatisticsListChild = queryStatisticsList
        .children('.ui-jqgrid-bdiv');
    var api = queryStatisticsListChild.data('jsp');
    api.reinitialise();
    
    $('#queryStatisticsList').toggle();
    refreshTheGrid($('#queryStatisticsList'));
  }
};

function refreshTheGrid(gridDiv) {
  setTimeout(function(){gridDiv.toggle();}, 500);
}
