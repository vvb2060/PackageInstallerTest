package io.github.vvb2060.packageinstaller.test;

import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.content.pm.PackageInstaller.EXTRA_SESSION_ID;
import static android.content.pm.PackageInstaller.EXTRA_STATUS;
import static android.content.pm.PackageInstaller.EXTRA_STATUS_MESSAGE;
import static android.content.pm.PackageInstaller.STATUS_FAILURE_INVALID;
import static android.content.pm.PackageInstaller.STATUS_PENDING_USER_ACTION;
import static android.content.pm.PackageInstaller.STATUS_SUCCESS;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller.Session;
import android.content.pm.PackageInstaller.SessionParams;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class APKInstall {
    private static final String TAG = "APKInstall";

    public static void installapk(Context context, File apk) {
        //noinspection InlinedApi
        var flag = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE;
        var intent = new Intent(TAG).setPackage(context.getPackageName());
        var pending = PendingIntent.getBroadcast(context, 0, intent, flag);
        var installer = context.getPackageManager().getPackageInstaller();
        var params = new SessionParams(SessionParams.MODE_FULL_INSTALL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params.setRequireUserAction(SessionParams.USER_ACTION_NOT_REQUIRED);
        }
        try (Session session = installer.openSession(installer.createSession(params))) {
            OutputStream out = session.openWrite(apk.getName(), 0, apk.length());
            try (var in = new FileInputStream(apk); out) {
                transfer(in, out);
            }
            session.commit(pending.getIntentSender());
        } catch (IOException e) {
            Log.e(TAG, "installer session", e);
        }
    }

    private static void transfer(InputStream in, OutputStream out) throws IOException {
        int size = 8192;
        var buffer = new byte[size];
        int read;
        while ((read = in.read(buffer, 0, size)) >= 0) {
            out.write(buffer, 0, read);
        }
    }

    public static void register(Context context) {
        var receiver = new InstallReceiver();
        context.registerReceiver(receiver, new IntentFilter(TAG));
    }

    private static class InstallReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent i) {
            int status = i.getIntExtra(EXTRA_STATUS, STATUS_FAILURE_INVALID);
            switch (status) {
                case STATUS_PENDING_USER_ACTION:
                    Intent intent = i.getParcelableExtra(Intent.EXTRA_INTENT);
                    handleUserAction(context, intent);
                    break;
                case STATUS_SUCCESS:
                    break;
                default:
                    Log.e(TAG, "onReceive: status=" + status +
                            " message=" + i.getStringExtra(EXTRA_STATUS_MESSAGE));
                    int id = i.getIntExtra(EXTRA_SESSION_ID, 0);
                    if (id > 0) {
                        var installer = context.getPackageManager().getPackageInstaller();
                        var info = installer.getSessionInfo(id);
                        if (info != null) {
                            installer.abandonSession(info.getSessionId());
                        }
                    }
            }
        }

        private void handleUserAction(Context context, Intent intent) {
            if (MainActivity.Foreground) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } else {
                var nm = context.getSystemService(NotificationManager.class);
                var channelID = "User Action";
                var channel = new NotificationChannel(channelID, channelID, IMPORTANCE_HIGH);
                nm.createNotificationChannel(channel);
                var flag = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
                var pending = PendingIntent.getActivity(context, 0, intent, flag);
                var icon = Icon.createWithResource(context, android.R.drawable.stat_sys_download_done);
                var builder = new Notification.Builder(context, channelID)
                        .setContentIntent(pending)
                        .setContentTitle("ready to update")
                        .setContentText("tap to install")
                        .setSmallIcon(icon)
                        .setAutoCancel(true);
                nm.notify(24, builder.build());
            }
        }
    }
}
