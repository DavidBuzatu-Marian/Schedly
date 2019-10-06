package com.example.schedly.model;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import com.example.schedly.R;
import static android.app.Notification.DEFAULT_VIBRATE;

public class InternetReceiver extends BroadcastReceiver {
    private static final String ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    private static final String NOTIFICATION_CHANNEL_ID = "com.example.schedly";

    public InternetReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION)) {
            if (!NetworkChecker.isNetworkAvailable(context)) {
                startNotificationForDifferentSDK(context);
            }
        }
    }

    private void sendNotification(Context context) {
        NotificationCompat.Builder _builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Schedly")
                .setContentText(context.getString(R.string.notification_monitor_internet))
                .setPriority(Notification.PRIORITY_HIGH)
                .setDefaults(DEFAULT_VIBRATE);
        Notification _notification = _builder.build();
        NotificationManager _notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        _notificationManager.notify(0, _notification);
    }

    private void startNotificationForDifferentSDK(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel _channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    "Schedly no internet alert",
                    NotificationManager.IMPORTANCE_HIGH);
            _channel.setDescription("No internet for monitoring");
            _channel.setShowBadge(true);
            _channel.enableVibration(true);
            _channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager _manager = context.getSystemService(NotificationManager.class);
            _manager.createNotificationChannel(_channel);
            sendNotification(context);
        }
    }

}