package com.example.schedly.model;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class NetworkChecker {

    private static final String TAG = NetworkChecker.class.getSimpleName();

    public static boolean isNetworkAvailable(Context context) {
        NetworkInfo _netInfo = ((ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

        if(_netInfo == null ) {
            Log.d(TAG, "No internet");
            return false;
        } else {
            if(_netInfo.isConnected()) {
                Log.d(TAG, "internet available");
                return true;
            } else {
                Log.d(TAG, "internet not connected but exists");
                return true;
            }

        }
    }

}
