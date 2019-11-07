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
            return false;
        } else {
            if(_netInfo.isConnected()) {
                return true;
            } else {
                return true;
            }

        }
    }

}
