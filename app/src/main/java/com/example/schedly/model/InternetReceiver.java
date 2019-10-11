package com.example.schedly.model;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import com.example.schedly.R;

import static android.app.Notification.DEFAULT_ALL;
import static android.app.Notification.DEFAULT_VIBRATE;

public class InternetReceiver extends BroadcastReceiver {
    private static final String ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    private static final String NOTIFICATION_CHANNEL_ID = "channel_internet";

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
        NotificationCompat.Builder _builder = buildNotification(context);
        NotificationManager _notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        _notificationManager.notify(1000, _builder.build());

        removeNotification(_notificationManager);
    }

    private void removeNotification(final NotificationManager notificationManager) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                notificationManager.cancel(1000);
            }
        }).start();
    }

    private NotificationCompat.Builder buildNotification(Context context) {
        NotificationCompat.Builder _builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Schedly")  // required
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentText(context.getString(R.string.notification_monitor_internet))
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(null)
                .setTicker("Schedly")
                .setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
        if((Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)) {
            _builder.setPriority(Notification.PRIORITY_HIGH);
        }
        return _builder;
    }

    private void startNotificationForDifferentSDK(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager _manager = context.getSystemService(NotificationManager.class);
            NotificationChannel _channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    "Schedly",
                    NotificationManager.IMPORTANCE_HIGH);
            _channel.setDescription("Monitor Internet changes");
            _channel.enableVibration(true);
            _channel.setLightColor(Color.GREEN);
            _channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});

            _manager.createNotificationChannel(_channel);
            sendNotification(context);
        } else {
            sendNotification(context);
        }
    }

}