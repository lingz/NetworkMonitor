package com.networkmonitor;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.events.Event;
import com.networkmonitor.utils.BusServiceConnection;
import com.networkmonitor.utils.ErrorDialogHelper;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

/**
 * Created by ling on 2/25/16.
 */
public class NetworkModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private ReactApplicationContext mReactContext;
    private Context mContext;
    private DeviceEventManagerModule.RCTDeviceEventEmitter mEmitModule;

    private Bus mBus;

    private boolean mHasLocationPermissions = false;

    public NetworkModule(Context context, ReactApplicationContext reactContext) {
        super(reactContext);
        mContext = context;
        mReactContext = reactContext;

        mReactContext.addLifecycleEventListener(this);

        mBus = ((NetworkMonitorApplication) mContext.getApplicationContext())
                .getBus();
        mBus.register(this);
        Log.e("LING", "START");
    }

    @Override
    public void onHostResume() {

    }

    @Override
    public void onHostDestroy() {
        Log.e("LING", "Host destroyed");
    }

    @Override
    public void onHostPause() {
        Log.e("LING", "Host paused");
    }

    private DeviceEventManagerModule.RCTDeviceEventEmitter getEmitModule() {
        if (mEmitModule == null) {
            mEmitModule = mReactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
        }
        return mEmitModule;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @ReactMethod
    public void startMonitoringService() {
        requestPermissions();
        if (mHasLocationPermissions) {
            mContext.startService(new Intent(mContext, NetworkMonitoringService.class));
        } else {
            ErrorDialogHelper.fatalCrashDialog(mContext,
                    "You must give location permissions to run this app");
        }
    }

    @ReactMethod
    public void stopMonitoringService() {
        mContext.stopService(new Intent(mContext, NetworkMonitoringService.class));
    }


    @ReactMethod
    public void checkServiceStatus() {
        boolean active = false;
        ActivityManager am = (ActivityManager) mContext
                .getSystemService(mContext.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo serviceInfo
                : am.getRunningServices(Integer.MAX_VALUE)) {
            if (NetworkMonitoringService.class.getName()
                    .equals(serviceInfo.service.getClassName())) {
                active = true;
                break;
            }
        }
        getEmitModule().emit("active", active);
        mBus.post(new Events.LastResults());
    }


    // Returns true if permission is granted or not needed
    @ReactMethod
    public void requestPermissions() {
        if (mHasLocationPermissions) {
            Log.e("LING", "1");
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ) {
            Log.e("LING", "2");

            mHasLocationPermissions = true;
            return;
        }

        if (ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.e("LING", "3");

            mHasLocationPermissions = true;
            return;
        }

        Log.e("LING", "4");
        ActivityCompat.requestPermissions((Activity) mContext,
                new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                1);
    }

    @Subscribe
    public void pingResultListener(Events.PingResult pingResult) {
        getEmitModule().emit("connectionType", pingResult.connType.toString());
        getEmitModule().emit("ping", pingResult.ping == null ? -1.0 : pingResult.ping);
        getEmitModule().emit("rssi", pingResult.rssiLevel);
    }

    @Subscribe
    public void bandwidthResultListener(Events.BandwidthResult bandwidthResult) {
        getEmitModule().emit("speed", bandwidthResult.speed == null ?
                -1.0 : bandwidthResult.speed);
    }

    @Subscribe
    public void locationUpdateListener(Events.LocationUpdate locationUpdate) {
        Location location = locationUpdate.location;
        String locationString = location == null ? "Unknown" : String.format(
                "%.5f, %.5f",
                location.getLatitude(),
                location.getLongitude());
        getEmitModule().emit("location", locationString);
    }
}
