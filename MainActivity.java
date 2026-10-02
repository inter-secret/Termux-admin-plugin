package com.example.deviceadmin;

import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class MainActivity extends AppCompatActivity {

    private static final String TAG    = "MainActivity";
    private static final int    REQ_ADMIN = 42;

    private DeviceAdminManager dam;

    // Views
    private View          statusDot;
    private TextView      tvAdminStatus;
    private LinearLayout  layoutPolicySummary;
    private TextView      tvCameraState, tvTimeoutState, tvPwdState;
    private MaterialButton btnActivate;

    private SwitchMaterial switchCamera;
    private SwitchMaterial switchKeyguard;

    private EditText etTimeout, etPwdMin, etFailedWipe;

    private TextView tvCommands;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dam = DeviceAdminManager.get(this);
        NotificationHelper.createChannel(this);

        bindViews();
        setupListeners();
        populateTermuxReference();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
    }

    // ─── View binding ────────────────────────────────────────────────────────

    private void bindViews() {
        statusDot          = findViewById(R.id.status_dot);
        tvAdminStatus      = findViewById(R.id.tv_admin_status);
        layoutPolicySummary= findViewById(R.id.layout_policy_summary);
        tvCameraState      = findViewById(R.id.tv_camera_state);
        tvTimeoutState     = findViewById(R.id.tv_timeout_state);
        tvPwdState         = findViewById(R.id.tv_pwd_state);
        btnActivate        = findViewById(R.id.btn_activate);

        switchCamera       = findViewById(R.id.switch_camera);
        switchKeyguard     = findViewById(R.id.switch_keyguard);

        etTimeout          = findViewById(R.id.et_timeout);
        etPwdMin           = findViewById(R.id.et_pwd_min);
        etFailedWipe       = findViewById(R.id.et_failed_wipe);

        tvCommands         = findViewById(R.id.tv_commands);
    }

    // ─── Listeners ───────────────────────────────────────────────────────────

    private void setupListeners() {

        // Activate / deactivate toggle
        btnActivate.setOnClickListener(v -> {
            if (dam.isActive()) {
                confirmThen("Remove admin access?",
                    "The app will lose all device policy capabilities.",
                    () -> { dam.removeSelf(); refreshUI(); }
                );
            } else {
                requestAdmin();
            }
        });

        // Lock now
        findViewById(R.id.btn_lock).setOnClickListener(v -> {
            if (!dam.isActive()) { snack("Activate admin first"); return; }
            dam.lockNow();
        });

        // Camera switch
        switchCamera.setOnCheckedChangeListener((btn, isChecked) -> {
            if (!dam.isActive()) { btn.setChecked(false); snack("Activate admin first"); return; }
            dam.setCameraDisabled(isChecked);
            updatePolicySummary();
        });

        // Keyguard switch
        switchKeyguard.setOnCheckedChangeListener((btn, isChecked) -> {
            if (!dam.isActive()) { btn.setChecked(false); snack("Activate admin first"); return; }
            dam.setKeyguardDisabledFeatures(isChecked
                ? DevicePolicyManager.KEYGUARD_DISABLE_WIDGETS
                    | DevicePolicyManager.KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS
                : DevicePolicyManager.KEYGUARD_DISABLE_FEATURES_NONE
            );
        });

        // Apply policy
        findViewById(R.id.btn_apply_policy).setOnClickListener(v -> applyPolicies());

        // Wipe
        findViewById(R.id.btn_wipe).setOnClickListener(v -> {
            if (!dam.isActive()) { snack("Activate admin first"); return; }
            confirmThen(
                "⚠ WIPE DEVICE?",
                "This will permanently erase all data. There is NO undo.",
                () -> dam.wipeDevice(false)
            );
        });

        // Remove admin
        findViewById(R.id.btn_remove_admin).setOnClickListener(v ->
            confirmThen("Remove admin?", "All policies will be cleared.",
                () -> { dam.removeSelf(); refreshUI(); })
        );
    }

    // ─── UI refresh ──────────────────────────────────────────────────────────

    private void refreshUI() {
        boolean active = dam.isActive();

        // Status dot color
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(active ? Color.parseColor("#3FB950") : Color.parseColor("#F85149"));
        statusDot.setBackground(dot);

        // Status label
        tvAdminStatus.setText(active ? "ACTIVE" : "NOT ACTIVE");
        tvAdminStatus.setTextColor(active
            ? Color.parseColor("#3FB950")
            : Color.parseColor("#F85149"));

        // Activate button text
        btnActivate.setText(active ? "Deactivate Admin" : "Activate Device Admin");

        // Policy summary bar
        layoutPolicySummary.setVisibility(active ? View.VISIBLE : View.GONE);

        if (active) {
            // Sync switches without triggering listeners
            switchCamera.setOnCheckedChangeListener(null);
            switchKeyguard.setOnCheckedChangeListener(null);
            switchCamera.setChecked(dam.isCameraDisabled());
            switchKeyguard.setChecked(
                dam.getKeyguardDisabledFeatures() != DevicePolicyManager.KEYGUARD_DISABLE_FEATURES_NONE
            );
            // Re-attach
            setupListeners();

            // Populate fields with current values
            long timeoutMs = dam.getScreenTimeout();
            etTimeout.setText(timeoutMs > 0 ? String.valueOf(timeoutMs / 1000) : "");

            int pwdMin = dam.getPasswordMinLength();
            etPwdMin.setText(pwdMin > 0 ? String.valueOf(pwdMin) : "");

            int failWipe = dam.getMaxFailedPasswordsForWipe();
            etFailedWipe.setText(failWipe > 0 ? String.valueOf(failWipe) : "");

            updatePolicySummary();
        }
    }

    private void updatePolicySummary() {
        tvCameraState.setText("CAM: " + (dam.isCameraDisabled() ? "OFF" : "ON"));
        long to = dam.getScreenTimeout();
        tvTimeoutState.setText("LOCK: " + (to == 0 ? "—" : (to / 1000) + "s"));
        int pm = dam.getPasswordMinLength();
        tvPwdState.setText("PWD: " + (pm == 0 ? "—" : pm + "+"));
    }

    // ─── Policy application ───────────────────────────────────────────────────

    private void applyPolicies() {
        if (!dam.isActive()) { snack("Activate admin first"); return; }

        boolean changed = false;

        // Screen timeout
        String sTimeout = etTimeout.getText().toString().trim();
        if (!TextUtils.isEmpty(sTimeout)) {
            try {
                long secs = Long.parseLong(sTimeout);
                dam.setScreenTimeout(secs > 0 ? secs * 1000 : 0);
                changed = true;
            } catch (NumberFormatException e) {
                snack("Invalid timeout value"); return;
            }
        }

        // Min password length
        String sPwdMin = etPwdMin.getText().toString().trim();
        if (!TextUtils.isEmpty(sPwdMin)) {
            try {
                dam.setPasswordMinLength(Integer.parseInt(sPwdMin));
                changed = true;
            } catch (NumberFormatException e) {
                snack("Invalid password length"); return;
            }
        }

        // Failed wipe threshold
        String sFailWipe = etFailedWipe.getText().toString().trim();
        if (!TextUtils.isEmpty(sFailWipe)) {
            try {
                dam.setMaxFailedPasswordsForWipe(Integer.parseInt(sFailWipe));
                changed = true;
            } catch (NumberFormatException e) {
                snack("Invalid failed-wipe value"); return;
            }
        }

        if (changed) {
            updatePolicySummary();
            snack("Policies applied ✓");
        } else {
            snack("Enter at least one value to apply");
        }
    }

    // ─── Admin activation ────────────────────────────────────────────────────

    private void requestAdmin() {
        Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, dam.getAdminComponent());
        i.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.admin_description));
        startActivityForResult(i, REQ_ADMIN);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_ADMIN) {
            refreshUI();
            snack(dam.isActive() ? "Admin activated ✓" : "Activation cancelled");
        }
    }

    // ─── Termux reference ────────────────────────────────────────────────────

    private void populateTermuxReference() {
        String pkg = getPackageName();
        tvCommands.setText(
            "# Add alias in Termux ~/.bashrc:\n"
          + "alias adm='am broadcast -a " + pkg + ".ACTION --es cmd'\n"
          + "\n"
          + "adm \"lock\"                   # lock screen\n"
          + "adm \"camera-off\"             # disable camera\n"
          + "adm \"camera-on\"              # enable camera\n"
          + "adm \"timeout\" --el val 30000 # auto-lock in 30s\n"
          + "adm \"timeout-off\"            # clear timeout\n"
          + "adm \"pwd-min\" --ei val 8     # min 8-char password\n"
          + "adm \"pwd-min-off\"            # clear password req\n"
          + "adm \"fail-wipe\" --ei val 10  # wipe after 10 fails\n"
          + "adm \"fail-wipe-off\"          # disable fail-wipe\n"
          + "adm \"keyguard-off\"           # disable lock widgets\n"
          + "adm \"keyguard-on\"            # restore lock screen\n"
          + "adm \"status\"                 # show current state\n"
          + "adm \"wipe\"                   # ⚠ ERASE DEVICE"
        );
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void snack(String msg) {
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT).show();
    }

    private void confirmThen(String title, String msg, Runnable action) {
        new AlertDialog.Builder(this, R.style.AlertDialogDark)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("Confirm", (d, w) -> action.run())
            .setNegativeButton("Cancel", null)
            .show();
    }
}
