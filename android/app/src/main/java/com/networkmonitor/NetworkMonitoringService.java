package com.networkmonitor;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.networkmonitor.utils.ServiceBinder;

/**
 * Created by ling on 2/26/16.
 */
public class NetworkMonitoringService extends Service {

    private NetworkMonitor mNetworkMonitor;

    @Override
    public void onCreate() {
        super.onCreate();
        mNetworkMonitor = new NetworkMonitor(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("LING", "NETWORK MONITOR DESTROYED");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ServiceBinder<NetworkMonitoringService>(this);
    }

    @ReactMethod
    public void testNetwork() {



        NetworkMonitor.ConnectionType connectionType = mNetworkMonitor.getConnectionType();
        Double ping = null;
        Double speed = null;
        if (connectionType == NetworkMonitor.ConnectionType.WIFI) {
            ping = mNetworkMonitor.checkPing("www.google.com", 80);
            if (ping != null) {
                speed = mNetworkMonitor
                        .checkBandwidth("https://lh3.googleusercontent.com/-rwSMlerPnY8/VsMyCkV_rpI/AAAAAAAAdAk/ZW08TTgrvH4/w4898-h3265/17ExplorerSport_315_as_C3.jpg");
            }
        }

        emitModule.emit("connectionType", connectionType.toString());
        emitModule.emit("ping", ping == null ? -1.0 : ping);
        emitModule.emit("speed", speed == null ? -1.0 : speed);
    }


}
