package com.networkmonitor;

import android.app.Application;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

/**
 * Created by ling on 2/26/16.
 */
public class NetworkMonitorApplication extends Application {
    private final Bus mBus = new Bus(ThreadEnforcer.ANY);

    public Bus getBus() {
        return mBus;
    }

}
