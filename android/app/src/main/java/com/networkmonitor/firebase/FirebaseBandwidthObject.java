package com.networkmonitor.firebase;

import com.firebase.client.ServerValue;

import java.util.Map;

/**
 * Created by ling on 2/26/16.
 */
public class FirebaseBandwidthObject {
    public int anonId;
    public double speed;
    public int rssi;
    public double lat;
    public double lng;
    public double time;

    public FirebaseBandwidthObject() {

    }

    public FirebaseBandwidthObject(int anonId, double speed, int rssi, double lat, double lng) {
        this.anonId = anonId;
        this.speed = speed;
        this.rssi = rssi;
        this.lat = lat;
        this.lng = lng;
        time = (double) System.currentTimeMillis();
    }
}
