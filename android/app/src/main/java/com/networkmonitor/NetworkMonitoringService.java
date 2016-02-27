package com.networkmonitor;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.firebase.client.Firebase;
import com.firebase.client.ServerValue;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.networkmonitor.firebase.FirebaseBandwidthObject;
import com.networkmonitor.firebase.FirebasePingObject;
import com.networkmonitor.firebase.FirebaseUrl;
import com.networkmonitor.utils.ErrorDialogHelper;
import com.networkmonitor.utils.ServiceBinder;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by ling on 2/26/16.
 */
public class NetworkMonitoringService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private NetworkMonitor mNetworkMonitor;
    Bus mBus;
    private volatile boolean mHasInternet;
    private volatile Events.PingResult mLastPingResult;
    private volatile Events.BandwidthResult mLastBandwidthResult;


    WifiManager mWifiManager;

    private GoogleApiClient mGoogleApiClient;
    private static final LocationRequest sLocationRequest;
    private Location mLastLocation = null;

    static {
        Firebase.getDefaultConfig().setPersistenceEnabled(true);
    }
    private Firebase mRootNode;
    private Firebase mPingNode;
    private Firebase mBandwidthNode;

    private static final long EXPERIMENT_END = ((long) 1456689601) * 1000;

    int mAnonId = -1;


    static {
        sLocationRequest = new LocationRequest();
        sLocationRequest.setInterval(500);
        sLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.getErrorCode() == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED) {
            ErrorDialogHelper.fatalCrashDialog(
                    this,
                    "You need to update your Google Play Services to use this app");
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("LING", "Google API Client Connected Suspended");
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.v("LING", "Google API Client Connected");
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, sLocationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        mBus.post(new Events.LocationUpdate(location));
    }

    private final ScheduledExecutorService mScheduler =
            Executors.newScheduledThreadPool(2);


    @Subscribe
    public void lastResultsListener(Events.LastResults lastResults) {
        if (mLastLocation != null) {
            mBus.post(new Events.LocationUpdate(mLastLocation));
        }
        if (mLastPingResult != null) {
            mBus.post(mLastPingResult);
        }
        if (mLastBandwidthResult != null) {
            mBus.post(mLastBandwidthResult);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mAnonId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID)
                .hashCode();


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();

        mWifiManager = (WifiManager) this.getSystemService(WIFI_SERVICE);

        Log.e("LING", "NETWORK MONITOR CREATED");
        mNetworkMonitor = new NetworkMonitor(this);
        mBus = ((NetworkMonitorApplication) this.getApplication())
                .getBus();
        final Runnable pingTest = new Runnable() {
            @Override
            public void run() {
                testPing();
            }
        };
        final Runnable bandwidthTest = new Runnable() {
            @Override
            public void run() {
                testBandwidth();
            }
        };
        mScheduler.scheduleAtFixedRate(pingTest, 0, 10, TimeUnit.SECONDS);
        mScheduler.scheduleAtFixedRate(bandwidthTest, 1, 60, TimeUnit.SECONDS);

        // Make a foreground service
        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("WIFI connectivity is being monitored")
                .setSmallIcon(R.drawable.wifitransparent)
                .setOngoing(true)
                .setContentIntent(resultPendingIntent)
                .build();
        startForeground(1, notification);

        // Firebase time
        Firebase.setAndroidContext(this);
        mRootNode = new Firebase(FirebaseUrl.FIREBASE_HOST);
        mBandwidthNode = mRootNode.child("bandwidth");
        mPingNode = mRootNode.child("ping");

        mBus.register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("LING", "NETWORK MONITOR DESTROYED");
        mGoogleApiClient.disconnect();
        mScheduler.shutdown();
        mBus.unregister(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ServiceBinder<NetworkMonitoringService>(this);
    }

    private void testPing() {
        NetworkMonitor.ConnectionType connectionType = mNetworkMonitor.getConnectionType();

        Double ping = null;

        if (connectionType == NetworkMonitor.ConnectionType.WIFI) {
            ping = mNetworkMonitor.checkPing("www.google.com", 80);
            mHasInternet = ping != null;
        } else {
            mHasInternet = false;
        }


        int rssiLevel = -1;

        if (connectionType == NetworkMonitor.ConnectionType.WIFI) {
            rssiLevel = WifiManager.calculateSignalLevel(
                    mWifiManager.getConnectionInfo().getRssi(), 5);
        }

        mLastPingResult = new Events.PingResult(connectionType, ping, rssiLevel);
        mBus.post(mLastPingResult);
    }

    private void testBandwidth() {
        Double speed = null;

        if (mHasInternet) {
            speed = mNetworkMonitor
                    .checkBandwidth("http://cdn.rawgit.com/lingz/NetworkMonitor/master/static/trialImage.jpg");
            mLastBandwidthResult = new Events.BandwidthResult(speed);
            mBus.post(mLastBandwidthResult);
        }
    }

    private boolean shouldUpload() {
        // On Wifi and the location is within the target zone
        return (mLastPingResult != null)
                && (mLastPingResult.connType == NetworkMonitor.ConnectionType.WIFI)
                && (mLastLocation != null)
                && (mLastLocation.getLatitude() > 24.517960)
                && (mLastLocation.getLatitude() < 24.529028)
                && (mLastLocation.getLongitude() > 54.430823)
                && (mLastLocation.getLongitude() < 54.438638)
                && (System.currentTimeMillis() < EXPERIMENT_END);

    }

    @Subscribe
    public void pingResultListener(Events.PingResult pingResult) {
        if (shouldUpload()) {
            mPingNode.push().setValue(new FirebasePingObject(
                    mAnonId,
                    pingResult.ping == null ? -1.0 : pingResult.ping,
                    pingResult.rssiLevel,
                    mLastLocation.getLatitude(),
                    mLastLocation.getLongitude()
            ));
        }
    }

    @Subscribe
    public void bandwidthResultListener(Events.BandwidthResult bandwidthResult) {
        if (shouldUpload()) {
            mBandwidthNode.push().setValue(new FirebaseBandwidthObject(
                    mAnonId,
                    bandwidthResult.speed == null ? -1.0 : bandwidthResult.speed,
                    mLastPingResult.rssiLevel,
                    mLastLocation.getLatitude(),
                    mLastLocation.getLongitude()
            ));
        }
    }




}
