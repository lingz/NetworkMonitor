package com.networkmonitor;

import android.content.Context;
import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;

/**
 * Created by ling on 2/25/16.
 */
public class NetworkModule extends ReactContextBaseJavaModule {

    ReactApplicationContext mReactContext;
    NetworkMonitor mNetworkMonitor;

    public NetworkModule(Context context, ReactApplicationContext reactContext) {
        super(reactContext);
        mReactContext = reactContext;
        mNetworkMonitor = new NetworkMonitor(context);


        Log.e("LING", "START");
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @ReactMethod
    public void testNetwork() {
        DeviceEventManagerModule.RCTDeviceEventEmitter emitModule = mReactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);


        NetworkMonitor.ConnectionType connectionType = mNetworkMonitor.getConnectionType();
        Double ping = null;
        Double speed = null;
        if (connectionType == NetworkMonitor.ConnectionType.WIFI) {
            ping = mNetworkMonitor.checkPing("www.google.com", 80);
            if (ping != null) {
                speed = mNetworkMonitor.checkBandwidth("http://www.nasa.gov/sites/default/files/thumbnails/image/14797031062_4cbe0f218f_o.jpg");
            }
        }

        emitModule.emit("connectionType", connectionType.toString());
        emitModule.emit("ping", ping == null ? -1.0 : ping);
        emitModule.emit("speed", speed == null ? -1.0 : speed);
    }
}
