package com.dailyserviceapp.core.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;

/**
 * Monitors network connectivity status for the application.
 * Provides real-time network availability checking and callbacks
 * for network state changes (connected/disconnected).
 * 
 * <p>Supports WiFi, cellular, and ethernet connections on API 24+.</p>
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-08
 */
public class NetworkMonitor {
    
    /** Android connectivity manager instance */
    private final ConnectivityManager connectivityManager;
    
    /** Network callback for monitoring connection changes */
    private NetworkCallback networkCallback;
    
    /** Current network connection status */
    private boolean isConnected = false;
    
    /**
     * Listener interface for network connectivity changes.
     * Implement this to receive callbacks when network becomes available or lost.
     */
    public interface OnNetworkChangeListener {
        /**
         * Called when network connection becomes available.
         */
        void onNetworkAvailable();
        
        /**
         * Called when network connection is lost.
         */
        void onNetworkLost();
    }
    
    /**
     * Constructs a NetworkMonitor with the application context.
     * Automatically checks initial network connectivity status.
     * 
     * @param context The application context
     */
    public NetworkMonitor(Context context) {
        this.connectivityManager = (ConnectivityManager) 
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        checkInitialConnection();
    }
    
    /**
     * Checks and stores the initial network connection status.
     */
    private void checkInitialConnection() {
        isConnected = isNetworkAvailable();
    }
    
    /**
     * Checks if network is currently available.
     * Detects WiFi, cellular, or ethernet connections.
     * 
     * @return true if network is available, false otherwise
     */
    public boolean isNetworkAvailable() {
        if (connectivityManager == null) return false;

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null && (
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        );
    }
    
    /**
     * Registers a callback to listen for network changes.
     * 
     * @param listener The listener to receive network change callbacks
     */
    public void registerNetworkCallback(OnNetworkChangeListener listener) {
        if (connectivityManager == null) return;

        NetworkRequest networkRequest = new NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build();

        networkCallback = new NetworkCallback(listener);
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }
    
    /**
     * Unregisters the network callback to stop listening for changes.
     * Safe to call even if callback was not registered.
     */
    public void unregisterNetworkCallback() {
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                networkCallback = null;
            }
        }
    }
    
    /**
     * Gets the current network connection status.
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * Internal callback implementation for network state changes.
     */
    private class NetworkCallback extends ConnectivityManager.NetworkCallback {
        /** Listener to notify of network changes */
        private final OnNetworkChangeListener listener;
        
        /**
         * Constructs a NetworkCallback with the specified listener.
         * 
         * @param listener The listener to receive callbacks
         */
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
