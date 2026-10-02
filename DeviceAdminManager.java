package com.example.deviceadmin;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

/**
 * Singleton wrapper around DevicePolicyManager.
 * All admin operations go through here — never call DPM directly.
 */
public class DeviceAdminManager {

    private static final String TAG = "DeviceAdminManager";
    private static DeviceAdminManager instance;

    private final DevicePolicyManager dpm;
    private final ComponentName adminComp;

    // ─── Singleton ───────────────────────────────────────────────────────────

    private DeviceAdminManager(Context ctx) {
        dpm       = (DevicePolicyManager) ctx.getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComp = new ComponentName(ctx, AdminReceiver.class);
    }

    public static synchronized DeviceAdminManager get(Context ctx) {
        if (instance == null) {
            instance = new DeviceAdminManager(ctx.getApplicationContext());
        }
        return instance;
    }

    // ─── Guard ───────────────────────────────────────────────────────────────

    public boolean isActive() {
        return dpm.isAdminActive(adminComp);
    }

    private boolean check(String action) {
        if (!isActive()) {
            Log.w(TAG, "Blocked '" + action + "': admin not active");
            return false;
        }
        return true;
    }

    // ─── Actions ─────────────────────────────────────────────────────────────

    /** Immediately lock the screen. */
    public boolean lockNow() {
        if (!check("lockNow")) return false;
        dpm.lockNow();
        Log.i(TAG, "Screen locked");
        return true;
    }

    /**
     * Wipe the device.
     * @param includeExternalStorage pass true to also wipe SD card
     */
    public boolean wipeDevice(boolean includeExternalStorage) {
        if (!check("wipeDevice")) return false;
        int flags = includeExternalStorage ? DevicePolicyManager.WIPE_EXTERNAL_STORAGE : 0;
        dpm.wipeData(flags);
        Log.i(TAG, "WIPE triggered (external=" + includeExternalStorage + ")");
        return true;
    }

    /** Disable or re-enable the device camera. */
    public boolean setCameraDisabled(boolean disabled) {
        if (!check("setCameraDisabled")) return false;
        dpm.setCameraDisabled(adminComp, disabled);
        Log.i(TAG, "Camera disabled=" + disabled);
        return true;
    }

    public boolean isCameraDisabled() {
        return dpm.getCameraDisabled(adminComp);
    }

    /**
     * Set the maximum time before the screen auto-locks.
     * @param ms milliseconds; 0 = use system default
     */
    public boolean setScreenTimeout(long ms) {
        if (!check("setScreenTimeout")) return false;
        dpm.setMaximumTimeToLock(adminComp, ms);
        Log.i(TAG, "Screen timeout set to " + ms + "ms");
        return true;
    }

    public long getScreenTimeout() {
        return dpm.getMaximumTimeToLock(adminComp);
    }

    /**
     * Enforce a minimum password length.
     * @param length minimum characters; 0 = no requirement
     */
    public boolean setPasswordMinLength(int length) {
        if (!check("setPasswordMinLength")) return false;
        dpm.setPasswordMinimumLength(adminComp, length);
        Log.i(TAG, "Password min length set to " + length);
        return true;
    }

    public int getPasswordMinLength() {
        return dpm.getPasswordMinimumLength(adminComp);
    }

    /**
     * Wipe the device after this many failed unlock attempts.
     * @param count number of failures; 0 = feature disabled
     */
    public boolean setMaxFailedPasswordsForWipe(int count) {
        if (!check("setMaxFailedPwd")) return false;
        dpm.setMaximumFailedPasswordsForWipe(adminComp, count);
        Log.i(TAG, "Max failed passwords set to " + count);
        return true;
    }

    public int getMaxFailedPasswordsForWipe() {
        return dpm.getMaximumFailedPasswordsForWipe(adminComp);
    }

    /**
     * Disable keyguard features (e.g. lock-screen widgets, unredacted notifications).
     * @param flags DevicePolicyManager.KEYGUARD_DISABLE_* constants
     */
    public boolean setKeyguardDisabledFeatures(int flags) {
        if (!check("setKeyguardFeatures")) return false;
        dpm.setKeyguardDisabledFeatures(adminComp, flags);
        return true;
    }

    public int getKeyguardDisabledFeatures() {
        return dpm.getKeyguardDisabledFeatures(adminComp);
    }

    /** Remove this app from the device admin list. */
    public void removeSelf() {
        dpm.removeActiveAdmin(adminComp);
        Log.i(TAG, "Admin self-removed");
    }

    public ComponentName getAdminComponent() { return adminComp; }
    public DevicePolicyManager getDpm()      { return dpm; }

    // ─── Status snapshot ─────────────────────────────────────────────────────

    public String getStatusSummary() {
        if (!isActive()) return "Admin: NOT ACTIVE";
        return "Admin: ACTIVE"
            + " | CAM: " + (isCameraDisabled() ? "OFF" : "ON")
            + " | LOCK: " + (getScreenTimeout() == 0 ? "default" : (getScreenTimeout()/1000) + "s")
            + " | PWD_MIN: " + getPasswordMinLength()
            + " | FAILED_WIPE: " + getMaxFailedPasswordsForWipe();
    }
}
