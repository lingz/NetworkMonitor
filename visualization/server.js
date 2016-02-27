"use strict";

var keys = require("./keys.js")
var Firebase = require("firebase")
var FirebaseTokenGenerator = require("firebase-token-generator")
var d3 = require("./hexbin.js")
var _ = require("lodash")

var sourceDataNode = new Firebase(keys.sourceDataFirebaseUrl)
var destDataNode = new Firebase(keys.destDataFirebaseUrl)
var tokenGenerator = new FirebaseTokenGenerator(keys.firebaseSecret)
var token = tokenGenerator.createToken({uid: "etlServer"})

var hexbin = d3.hexbin()
              .radius(0.0001)
              .x((d) => {
                return d.lng - lngMin
              })
              .y((d) => {
                return d.lat - latMin
              })

var centers = hexbin.centers()
var latMin = 24.517960
var latMax = 24.529028
var lngMin = 54.430823
var lngMax = 54.438638


var acceptPoint = (point) => {
  return (point.time / 1000) > startTime &&
    (point.time / 1000) < endTime &&
    point.lat > latMin &&
    point.lat < latMax &&
    point.lng > lngMin &&
    point.lng < lngMax;
}
var lookupLatLng = (id) => {
  const indices = id.split("-").map((o) => {
    return parseInt(o);
  })
  return _.find(centers, (center) => {
    if (center.i == indices[0] && center.j == indices[1]) {
      return center
    }
  })
}

let data = [
{
  lat: 24.523487,
  lng: 54.434447
},
];

var addDataToBin = (point, bin, isBandwidth) => {
  const isFailure = isBandwidth ?
    point.speed == -1 : point.ping == -1;

  if (isFailure) {
    bin.failures += 1
  } else {
    bin.rssi.total += point.rssi;
    bin.rssi.count += 1;
    if (isBandwidth) {
      bin.speed.total += point.speed;
      bin.speed.count += 1;
    } else {
      bin.ping.total += point.ping;
      bin.ping.count += 1;
    }
  }
}

var bins = {};
var dirtyBins = {};

var processMapPoint = function(point, isBandwidth) {
  if (!acceptPoint(point)) {
    console.log("reject");
    return;
  }

  const binId = hexbin(point);
  let existingBin = bins[binId];


  if (!existingBin) {
    const center = lookupLatLng(binId)
    existingBin = {
      lat: center[1] + latMin,
      lng: center[0] + lngMin,
      rssi: {
        total: 0,
        count: 0
      },
      speed: {
        total: 0,
        count: 0
      },
      ping: {
        total: 0,
        count: 0
      },
      failures: 0
    };
    bins[binId] = existingBin;
  }

  addDataToBin(point, existingBin, isBandwidth)
  dirtyBins[binId] = existingBin;
}

destDataNode.authWithCustomToken(token, (err, auth) => {
  if (err) {
    console.log("AUTH FAILED")
  } else {
    console.log("AUTH SUCCESS")
    startEtl()
  }
})

var longTimeBuckets = {};
var shortTimeBuckets = {};
var dirtyLongTimeBuckets = {};
var dirtyShortTimeBuckets = {};
//var startTime = 1456516801;
// Real one
var startTime = 1456603201;
var endTime = 1456689601;
var shortWindow = 5;
var longWindow = 10 * 60;

var processTimePoint = (point, isBandwidth) => {
  if (!acceptPoint(point)) {
    return;
  }
  var now = new Date().getTime();

  var longBucketNum = Math.floor((point.time/1000 - startTime) / longWindow);
  var bucket = longTimeBuckets[longBucketNum];
  if (!bucket) {
    bucket = {
      time: startTime + (longBucketNum * longWindow),
      rssi: {
        total: 0,
        count: 0
      },
      speed: {
        total: 0,
        count: 0
      },
      ping: {
        total: 0,
        count: 0
      },
      failures: 0
    }
    longTimeBuckets[longBucketNum] = bucket;
  }

  addDataToBin(point, bucket, isBandwidth);
  dirtyLongTimeBuckets[longBucketNum] = bucket;
}

var users = {};
var numDataPoints = 0;
var numUsers = 0;
var numDataPointsChanged = false;
var numUsersChanged = false;

var processStatsPoint = (point) => {
  if (!acceptPoint(point)) {
    return;
  }
  numDataPoints++;
  numDataPointsChanged = true;
  if (!users[point.anonId]) {
    users[point.anonId] = true;
    numUsers++;
    numUsersChanged = true;
  }
}


var startEtl = () => {
  sourceDataNode.child("ping").on("child_added", (snapshot) => {
    const val = snapshot.val()
    processTimePoint(val, false);
    processMapPoint(val, false);
    processStatsPoint(val);
    syncTime();
    syncMap();
    syncStats();
  })
  sourceDataNode.child("bandwidth").on("child_added", (snapshot) => {
    const val = snapshot.val()
    processTimePoint(val, true);
    processMapPoint(val, true);
    processStatsPoint(val);
    syncTime();
    syncMap();
    syncStats();
  })
}

var debounceMap = {}
// curries the function
// runs max every timeout only
var debounce = (fn, timeout) => {
  return function() {
    const now = new Date().getTime();
    const mapEntry = debounceMap[fn];
    if (mapEntry === undefined || now - mapEntry > timeout) {
      debounceMap[fn] = now;
      setTimeout(() => {
        fn.apply(this, arguments);
      }, timeout);
    }
  }
}

var syncMap = debounce(() => {
  console.log("syncing map - dirty: ");
  console.log(dirtyBins);
  destDataNode.child("map").update(dirtyBins);
  dirtyBins = {};
}, 5000);

var syncTime = debounce(() => {
  console.log("syncing time - dirty: ");
  console.log(dirtyLongTimeBuckets);
  destDataNode.child("time").update(dirtyLongTimeBuckets);
  dirtyLongTimeBuckets = {};
}, 5000);

var syncStats = debounce(() => {
  if (numDataPointsChanged) {
    console.log("syncing stats - data points");
    numDataPointsChanged = false;
    destDataNode.child("numData").set(numDataPoints);
  }
  if (numUsersChanged) {
    console.log("syncing stats - users");
    numUsersChanged = false;
    destDataNode.child("numUsers").set(numUsers);
  }
}, 5000);
