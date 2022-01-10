package io.github.vvb2060.packageinstaller.test;

import static android.content.pm.PackageInstaller.EXTRA_STATUS;
import static android.content.pm.PackageInstaller.STATUS_FAILURE_INVALID;
import static android.content.pm.PackageInstaller.STATUS_PENDING_USER_ACTION;
import static android.content.pm.PackageInstaller.STATUS_SUCCESS;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller.Session;
import android.content.pm.PackageInstaller.SessionParams;
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
        var receiver = new InstallReceiver(context);
        context.registerReceiver(receiver, new IntentFilter(TAG));
    }

    private static class InstallReceiver extends BroadcastReceiver {
        private final Context context;

        private InstallReceiver(Context context) {
            this.context = context;
        }

        @Override
        public void onReceive(Context c, Intent i) {
            int status = i.getIntExtra(EXTRA_STATUS, STATUS_FAILURE_INVALID);
            switch (status) {
                case STATUS_PENDING_USER_ACTION:
                    Intent intent = i.getParcelableExtra(Intent.EXTRA_INTENT);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    break;
                case STATUS_SUCCESS:
                default:
                    Log.d(TAG, "onReceive: status=" + status);
                    context.unregisterReceiver(this);
            }
        }
    }
}
