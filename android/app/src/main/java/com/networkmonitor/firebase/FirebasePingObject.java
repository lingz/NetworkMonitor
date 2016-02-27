package com.networkmonitor.firebase;

import com.firebase.client.ServerValue;

import java.util.Map;

/**
 * Created by ling on 2/26/16.
 */
public class FirebasePingObject {
    public int anonId;
    public double ping;
    public int rssi;
    public double lat;
    public double lng;
    public double time;

    public FirebasePingObject() {

    }

    public FirebasePingObject(int anonId, double ping, int rssi, double lat, double lng) {
        this.anonId = anonId;
        this.ping = ping;
        this.rssi = rssi;
        this.lat = lat;
        this.lng = lng;
        time = (double) System.currentTimeMillis();
    }
}
