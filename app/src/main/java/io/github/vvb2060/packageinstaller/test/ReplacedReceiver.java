package io.github.vvb2060.packageinstaller.test;

import static android.app.NotificationManager.IMPORTANCE_HIGH;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;

public final class ReplacedReceiver extends BroadcastReceiver {
    private static final String ChannelID = "replaced";
    private static final int NotificationID = 42;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) return;
        var nm = context.getSystemService(NotificationManager.class);
        createNotificationChannel(nm);
        nm.notify(NotificationID, createNotification(context));
    }

    private void createNotificationChannel(NotificationManager nm) {
        var channel = new NotificationChannel(ChannelID, ChannelID, IMPORTANCE_HIGH);
        nm.createNotificationChannel(channel);
    }

    private Notification createNotification(Context context) {
        var flag = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        var intent = new Intent(context, MainActivity.class);
        var pending = PendingIntent.getActivity(context, 0, intent, flag);
        var icon = Icon.createWithResource(context, android.R.drawable.ic_dialog_info);
        var builder = new Notification.Builder(context, ChannelID);
        return builder.setContentIntent(pending)
                .setContentTitle("apk updated")
                .setContentText("tap to open")
                .setSmallIcon(icon)
                .setAutoCancel(true)
                .build();
    }
}
