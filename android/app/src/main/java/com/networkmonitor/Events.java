package com.networkmonitor;

import android.location.Location;

/**
 * Created by ling on 2/26/16.
 */
public class Events {
    public static class PingResult {
        public final NetworkMonitor.ConnectionType connType;
        public final Double ping;
        public final int rssiLevel;

        public PingResult(NetworkMonitor.ConnectionType connectionType,
                          Double ping, int rssiLevel) {
            this.connType = connectionType;
            this.ping = ping;
            this.rssiLevel = rssiLevel;
        }
    }

    public static class BandwidthResult {
        public final Double speed;

        public BandwidthResult(Double speed) {
            this.speed = speed;
        }
    }

    public static class LocationUpdate {
        public final Location location;

        public LocationUpdate(Location location) {
            this.location = location;
        }
    }

    public static class LastResults {

    }
}
