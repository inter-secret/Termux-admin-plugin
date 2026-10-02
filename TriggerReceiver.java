package com.example.deviceadmin;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * ════════════════════════════════════════════════════════════
 *  TERMUX BRIDGE — send commands from your terminal
 * ════════════════════════════════════════════════════════════
 *
 *  SETUP (run once in Termux):
 *    alias adm='am broadcast -a com.example.deviceadmin.ACTION --es cmd'
 *
 *  COMMANDS:
 *    adm "lock"                   → lock screen immediately
 *    adm "camera-off"             → disable camera
 *    adm "camera-on"              → re-enable camera
 *    adm "timeout" --el val 30000 → auto-lock after 30s (ms)
 *    adm "timeout-off"            → remove timeout policy
 *    adm "pwd-min" --ei val 8     → require 8-char password
 *    adm "pwd-min-off"            → remove length requirement
 *    adm "fail-wipe" --ei val 10  → wipe after 10 fails
 *    adm "fail-wipe-off"          → disable fail-wipe policy
 *    adm "keyguard-off"           → disable keyguard widgets
 *    adm "keyguard-on"            → restore keyguard
 *    adm "status"                 → log + notify current state
 *    adm "wipe"                   → ⚠ WIPE DEVICE (no undo)
 *    adm "wipe-all"               → ⚠ WIPE + EXTERNAL STORAGE
 *
 * ════════════════════════════════════════════════════════════
 */
public class TriggerReceiver extends BroadcastReceiver {

    private static final String TAG = "TriggerReceiver";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String cmd = intent.getStringExtra("cmd");
        if (cmd == null) {
            Log.w(TAG, "Received intent with no 'cmd' extra — ignored");
            return;
        }
        cmd = cmd.trim().toLowerCase();
        Log.i(TAG, "Received command: " + cmd);

        DeviceAdminManager dam = DeviceAdminManager.get(ctx);

        if (!dam.isActive() && !cmd.equals("status")) {
            Log.w(TAG, "Admin not active — command blocked: " + cmd);
            NotificationHelper.show(ctx, "DeviceAdmin", "Command '" + cmd + "' blocked: admin not active.");
            return;
        }

        switch (cmd) {

            // ─── Screen ──────────────────────────────────────────────────────
            case "lock":
                dam.lockNow();
                notify(ctx, "Locked", "Screen locked via Termux.");
                break;

            // ─── Camera ──────────────────────────────────────────────────────
            case "camera-off":
                dam.setCameraDisabled(true);
                notify(ctx, "Camera", "Camera disabled.");
                break;

            case "camera-on":
                dam.setCameraDisabled(false);
                notify(ctx, "Camera", "Camera enabled.");
                break;

            // ─── Screen timeout ───────────────────────────────────────────────
            case "timeout": {
                long ms = intent.getLongExtra("val", 0);
                if (ms <= 0) { Log.w(TAG, "timeout: missing --el val"); break; }
                dam.setScreenTimeout(ms);
                notify(ctx, "Timeout", "Screen timeout set to " + (ms / 1000) + "s.");
                break;
            }

            case "timeout-off":
                dam.setScreenTimeout(0);
                notify(ctx, "Timeout", "Screen timeout policy removed.");
                break;

            // ─── Password policy ─────────────────────────────────────────────
            case "pwd-min": {
                int len = intent.getIntExtra("val", -1);
                if (len < 0) { Log.w(TAG, "pwd-min: missing --ei val"); break; }
                dam.setPasswordMinLength(len);
                notify(ctx, "Password", "Min password length set to " + len + ".");
                break;
            }

            case "pwd-min-off":
                dam.setPasswordMinLength(0);
                notify(ctx, "Password", "Password length requirement removed.");
                break;

            // ─── Failed-attempt wipe ─────────────────────────────────────────
            case "fail-wipe": {
                int n = intent.getIntExtra("val", -1);
                if (n < 0) { Log.w(TAG, "fail-wipe: missing --ei val"); break; }
                dam.setMaxFailedPasswordsForWipe(n);
                notify(ctx, "Fail-wipe", "Will wipe after " + n + " failed unlocks.");
                break;
            }

            case "fail-wipe-off":
                dam.setMaxFailedPasswordsForWipe(0);
                notify(ctx, "Fail-wipe", "Fail-wipe policy disabled.");
                break;

            // ─── Keyguard ────────────────────────────────────────────────────
            case "keyguard-off":
                dam.setKeyguardDisabledFeatures(
                    DevicePolicyManager.KEYGUARD_DISABLE_WIDGETS
                    | DevicePolicyManager.KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS
                );
                notify(ctx, "Keyguard", "Lock-screen widgets disabled.");
                break;

            case "keyguard-on":
                dam.setKeyguardDisabledFeatures(DevicePolicyManager.KEYGUARD_DISABLE_FEATURES_NONE);
                notify(ctx, "Keyguard", "Lock-screen features restored.");
                break;

            // ─── Status ──────────────────────────────────────────────────────
            case "status":
                String summary = dam.getStatusSummary();
                Log.i(TAG, "STATUS: " + summary);
                notify(ctx, "Status", summary);
                break;

            // ─── Wipe ────────────────────────────────────────────────────────
            case "wipe":
                notify(ctx, "⚠ WIPE", "Wipe initiated — device will erase.");
                dam.wipeDevice(false);
                break;

            case "wipe-all":
                notify(ctx, "⚠ WIPE ALL", "Full wipe initiated — all storage erasing.");
                dam.wipeDevice(true);
                break;

            default:
                Log.w(TAG, "Unknown command: " + cmd);
                notify(ctx, "DeviceAdmin", "Unknown command: '" + cmd + "'");
        }
    }

    private void notify(Context ctx, String title, String body) {
        NotificationHelper.show(ctx, title, body);
    }
}
