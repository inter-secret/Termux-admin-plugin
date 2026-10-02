package com.example.deviceadmin;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AdminReceiver extends DeviceAdminReceiver {

    private static final String TAG = "AdminReceiver";

    @Override
    public void onEnabled(Context ctx, Intent intent) {
        Log.i(TAG, "Device Admin ENABLED");
        NotificationHelper.show(ctx, "DeviceAdmin", "Administrator privileges activated.");
    }

    @Override
    public void onDisabled(Context ctx, Intent intent) {
        Log.i(TAG, "Device Admin DISABLED");
        NotificationHelper.show(ctx, "DeviceAdmin", "Administrator privileges removed.");
    }

    @Override
    public void onPasswordChanged(Context ctx, Intent intent) {
        Log.i(TAG, "Password changed");
        NotificationHelper.show(ctx, "DeviceAdmin", "Device password was changed.");
    }

    @Override
    public void onPasswordFailed(Context ctx, Intent intent) {
        DeviceAdminManager dam = DeviceAdminManager.get(ctx);
        int max = dam.getMaxFailedPasswordsForWipe();
        Log.w(TAG, "Password attempt failed (max=" + max + ")");
        if (max > 0) {
            NotificationHelper.show(ctx, "DeviceAdmin ⚠", "Wrong unlock attempt recorded.");
        }
    }

    @Override
    public void onPasswordSucceeded(Context ctx, Intent intent) {
        Log.i(TAG, "Password succeeded");
    }
}
