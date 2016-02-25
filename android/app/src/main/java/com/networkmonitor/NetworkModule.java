package com.networkmonitor;

import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;

/**
 * Created by ling on 2/25/16.
 */
public class NetworkModule extends ReactContextBaseJavaModule {
    ReactApplicationContext mReactContext;
    public NetworkModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mReactContext = reactContext;
        Log.e("LING", "START");
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @ReactMethod
    public void test(String message) {
        Log.e("LING", "Got " + message);
        mReactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("newString", "hello" + message);
    }
}
