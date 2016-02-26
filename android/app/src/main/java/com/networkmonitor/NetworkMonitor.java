package com.networkmonitor;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

/**
 * Created by ling on 2/26/16.
 */
public class NetworkMonitor {
    final ConnectivityManager mConnectivityManager;


    public NetworkMonitor(Context context) {
        mConnectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public enum ConnectionType {
        OFFLINE("OFFLINE"),
        MOBILE("MOBILE"),
        WIFI("WIFI");

        private final String name;

        private ConnectionType(String name) {
            this.name = name;
        }

        public String toString() {
            return  this.name;
        }
    }

    public static class NetworkStatus {
        public final boolean hasInternet;
        public final ConnectionType connectionType;
        // Kbps
        public final Double downloadBandwidth;

        protected NetworkStatus(boolean hasInternet,
                                ConnectionType connectionType,
                                Double downloadBandwidth) {
            this.hasInternet = hasInternet;
            this.connectionType = connectionType;
            this.downloadBandwidth = downloadBandwidth;
        }
    }

    public NetworkStatus check(String pingHost, int pingPort, String downloadUrl) {
        final boolean hasInternet = hasInternet(pingHost, pingPort);

        ConnectionType connectionType = getConnectionType();
        Double downloadBandwidth;

        if (hasInternet) {
            downloadBandwidth = checkBandwidth(downloadUrl);
        } else {
            downloadBandwidth = null;
        }

        NetworkStatus networkStatus = new NetworkStatus(
                hasInternet,
                connectionType,
                downloadBandwidth
        );

        return networkStatus;

    }

    public ConnectionType getConnectionType() {
        final NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();

        if (networkInfo == null) {
            return ConnectionType.OFFLINE;
        } else if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            return ConnectionType.WIFI;
        } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
            return ConnectionType.MOBILE;
        } else {
            return ConnectionType.OFFLINE;
        }
    }

    // Returns null if the test failed
    // Otherwise returns bandwidth in Kb/s
    public Double checkBandwidth(String host) {
        Double start = (double) System.currentTimeMillis();
        try {
            URL url = new URL(host);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setConnectTimeout(2000);
            urlConnection.setReadTimeout(5000);

            byte[] buffer = new byte[512];
            InputStream in = urlConnection.getInputStream();
            double size = 0;
            int nRead;

            while ((nRead = in.read(buffer)) != -1) {
                size += nRead;
            }
            in.close();
            Log.e("LING", "Bytes read: " + size);
            Log.e("LING", "Time: " + (System.currentTimeMillis() - start));
            return size / 1024 / (System.currentTimeMillis() - start) * 1000;
        } catch (Exception e) {
            return null;
        }
    }

    // Returns null if there was no internet
    public Double checkPing(String host, int port) {
        Double start = (double) System.currentTimeMillis();
        boolean hasInternet = hasInternet(host, port);
        if (hasInternet) {
            return System.currentTimeMillis() - start;
        } else {
            return null;
        }
    }

    public boolean hasInternet(String host, int port) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 2000);
            return true;
        } catch (IOException e) {
            // Either we have a timeout or unreachable host or failed DNS lookup
            System.out.println(e);
            return false;
        }
    }

}
