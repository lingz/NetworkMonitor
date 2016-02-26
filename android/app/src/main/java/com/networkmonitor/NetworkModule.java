package com.networkmonitor;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.networkmonitor.utils.BusServiceConnection;

/**
 * Created by ling on 2/25/16.
 */
public class NetworkModule extends ReactContextBaseJavaModule {

    DeviceEventManagerModule.RCTDeviceEventEmitter mEmitModule;
    private Context mContext;

    private BusServiceConnection mMonitoringService;

    public NetworkModule(Context context, ReactApplicationContext reactContext) {
        super(reactContext);
        mContext = context;
        DeviceEventManagerModule.RCTDeviceEventEmitter mEmitModule = reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);

        ((NetworkMonitorApplication) mContext.getApplicationContext())
                .getBus().register(this);
        Log.e("LING", "START");
    }


    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @ReactMethod
    public void startMonitoringService() {
        mMonitoringService = new BusServiceConnection();
        mContext.bindService(
                new Intent(mContext, NetworkMonitoringService.class),
                mMonitoringService,
                Context.BIND_AUTO_CREATE);
    }

    @ReactMethod
    public void stopMonitoringService() {
        mContext.unbindService(mMonitoringService);
    }


}
