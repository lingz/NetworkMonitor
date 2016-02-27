"use strict";

var keys = require("./keys.js")
var Firebase = require("firebase")
var FirebaseTokenGenerator = require("firebase-token-generator")
var d3 = require("./hexbin.js")
var _ = require("lodash")
var speedData = require("./speedData.js");
var pingData = require("./pingData.js");

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
  return point.lat > latMin &&
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


var bins = {};
var dirtyBins = {};

var processPoint = function(point, isBandwidth) {
  if (!acceptPoint(point)) {
    console.log("reject");
    return;
  }

  const binId = hexbin(point);
  let existingBin = bins[binId];

  const isFailure = isBandwidth ?
    point.speed == -1 : point.ping == -1;

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

  if (isFailure) {
    existingBin.failures += 1
  } else {
    existingBin.rssi.total += point.rssi;
    existingBin.rssi.count += 1;
    if (isBandwidth) {
      existingBin.speed.total += point.speed;
      existingBin.speed.count += 1;
    } else {
      existingBin.ping.total += point.ping;
      existingBin.ping.count += 1;
    }
  }
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

var startEtl = () => {
  sourceDataNode.child("ping").on("child_added", (snapshot) => {
    const val = snapshot.val()
    processPoint(val, false);
    syncMap();
  })
  sourceDataNode.child("bandwidth").on("child_added", (snapshot) => {
    const val = snapshot.val()
    processPoint(val, true);
    syncMap();
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
  console.log("syncing - dirty: ");
  console.log(dirtyBins);
  destDataNode.child("map").update(dirtyBins);
  dirtyBins = {};
}, 5000);
