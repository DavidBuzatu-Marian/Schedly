package com.example.schedly.model;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.schedly.model.NetworkChecker;

public class InternetReceiver extends BroadcastReceiver {
    private static final String ACTION = "android.net.conn.CONNECTIVITY_CHANGE";

    public InternetReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION)) {
            if (NetworkChecker.isNetworkAvailable(context)) {
                Toast.makeText(context, "WORKING", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "NOT WORKING", Toast.LENGTH_SHORT).show();
            }
        }
    }


}