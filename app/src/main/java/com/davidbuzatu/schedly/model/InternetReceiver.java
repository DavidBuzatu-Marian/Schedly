package com.davidbuzatu.schedly.model;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import com.davidbuzatu.schedly.R;
import com.davidbuzatu.schedly.SettingsActivity;
import com.davidbuzatu.schedly.service.MonitorIncomingSMSService;

import java.util.HashMap;

import static com.davidbuzatu.schedly.CalendarActivity.SETTINGS_RETURN;

public class InternetReceiver extends BroadcastReceiver {
    private static final String ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    private static final String NOTIFICATION_CHANNEL_ID = "channel_internet";
    private Context mContext;
    private NotificationManager mNotificationManager;
    public InternetReceiver(MonitorIncomingSMSService monitorIncomingSMSService) {
        mContext = monitorIncomingSMSService;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        if (intent.getAction().equals(ACTION)) {
            if (!NetworkChecker.isNetworkAvailable(context)) {
                stopServiceSMSMonitoring(context);
                startNotificationForDifferentSDK(context);
            }
        }
    }

    private void stopServiceSMSMonitoring(Context context) {
        Intent stopServiceIntent = new Intent(context, MonitorIncomingSMSService.class);
        context.stopService(stopServiceIntent);
    }

    private void sendNotification(Context context) {
        Intent _startSettingsIntent = getIntentForSettings(context);
        PendingIntent _intentSettings = PendingIntent.getActivity(context, SETTINGS_RETURN, _startSettingsIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        NotificationCompat.Builder _builder = buildNotification(context, _intentSettings);
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1000, _builder.build());
    }

    private Intent getIntentForSettings(Context context) {
        Intent _intent = new Intent(context, SettingsActivity.class);
        _intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        return _intent;
    }

//    private void removeNotification(final NotificationManager notificationManager) {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Thread.sleep(2500);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                notificationManager.cancel(1000);
//            }
//        }).start();
//    }

    private NotificationCompat.Builder buildNotification(Context context, PendingIntent intentSettings) {
        NotificationCompat.Builder _builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Schedly")  // required
                .setSmallIcon(R.drawable.ic_notification_small)
                .setContentText(context.getString(R.string.notification_monitor_internet))
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentIntent(intentSettings)
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