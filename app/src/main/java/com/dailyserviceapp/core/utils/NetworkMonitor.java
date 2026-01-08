package com.dailyserviceapp.core.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

import androidx.annotation.NonNull;

public class NetworkMonitor {
    
    private final ConnectivityManager connectivityManager;
    private NetworkCallback networkCallback;
    private boolean isConnected = false;
    
    public interface OnNetworkChangeListener {
        void onNetworkAvailable();
        void onNetworkLost();
    }
    
    public NetworkMonitor(Context context) {
        this.connectivityManager = (ConnectivityManager) 
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        checkInitialConnection();
    }
    
    private void checkInitialConnection() {
        isConnected = isNetworkAvailable();
    }
    
    public boolean isNetworkAvailable() {
        if (connectivityManager == null) return false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        } else {
            android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }
    
    public void registerNetworkCallback(OnNetworkChangeListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
            
            networkCallback = new NetworkCallback(listener);
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        }
    }
    
    public void unregisterNetworkCallback() {
        if (networkCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public boolean isConnected() {
        return isConnected;
    }
    
    private class NetworkCallback extends ConnectivityManager.NetworkCallback {
        private final OnNetworkChangeListener listener;
        
        NetworkCallback(OnNetworkChangeListener listener) {
            this.listener = listener;
        }
        
        @Override
        public void onAvailable(@NonNull Network network) {
            isConnected = true;
            if (listener != null) {
                listener.onNetworkAvailable();
            }
        }
        
        @Override
        public void onLost(@NonNull Network network) {
            isConnected = false;
            if (listener != null) {
                listener.onNetworkLost();
            }
        }
    }
}
