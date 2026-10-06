package dev.hypernoti;


import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import rikka.shizuku.Shizuku;

public class AdvancedActivity extends UiActivity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final List<ApplicationInfo> apps = new ArrayList<>();
    private Spinner appPicker;
    private boolean rootSelected;
    private TextView status;
    private Button enableButton, resetButton;
    private IPrivilegedService service;
    private boolean bound, busy;
    private final Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(
            new ComponentName("dev.hypernoti", PrivilegedService.class.getName()))
            .daemon(false).processNameSuffix("privileged").version(1);
    private final Shizuku.OnRequestPermissionResultListener permissionListener = (code, grant) -> {
        if (code == 41 && grant == PackageManager.PERMISSION_GRANTED) connect();
        else if (status != null) status.setText(R.string.permission_denied);
    };
    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            service = IPrivilegedService.Stub.asInterface(binder);
            status.setText(R.string.shizuku_ready);
        }
        @Override public void onServiceDisconnected(ComponentName name) { service = null; status.setText(R.string.shizuku_missing); }
    };

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        Shizuku.addRequestPermissionResultListener(permissionListener);
        LinearLayout root = Ui.root(this);
        Ui.toolbar(this, root, R.string.advanced, true);
        FrameLayout page = new FrameLayout(this);
        root.addView(page, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        LinearLayout content = Ui.scrolling(this, page);
        Ui.section(this, content, R.string.execution_method);
        LinearLayout method = Ui.card(this, content);
        Ui.text(this, method, getString(R.string.advanced_short), 14, false);
        MaterialButtonToggleGroup modes = new MaterialButtonToggleGroup(this);
        modes.setSingleSelection(true); modes.setSelectionRequired(true);
        MaterialButton shizuku = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        shizuku.setText(R.string.shizuku_label); shizuku.setId(R.id.shizuku_mode_button);
        MaterialButton rootButton = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        rootButton.setText(R.string.root_label); rootButton.setContentDescription(getString(R.string.root_mode)); rootButton.setId(R.id.root_mode_button);
        modes.addView(shizuku, new LinearLayout.LayoutParams(0, Ui.dp(this, 56), 1));
        modes.addView(rootButton, new LinearLayout.LayoutParams(0, Ui.dp(this, 56), 1));
        modes.check(saved != null && saved.getBoolean("root_selected") ? R.id.root_mode_button : R.id.shizuku_mode_button);
        rootSelected = modes.getCheckedButtonId() == R.id.root_mode_button;
        modes.addOnButtonCheckedListener((group, id, checked) -> { if (checked) rootSelected = id == R.id.root_mode_button; });
        method.addView(modes);
        Ui.button(this, method, R.string.connect_shizuku, R.drawable.ic_shield, false, this::connect);
        Ui.section(this, content, R.string.target_app);
        LinearLayout appCard = Ui.card(this, content);
        appPicker = new Spinner(this); appPicker.setMinimumHeight(Ui.dp(this, 56));
        appPicker.setContentDescription(getString(R.string.target_app));
        appCard.addView(appPicker, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        Ui.button(this, appCard, R.string.selected_settings, R.drawable.ic_settings, false, () -> {
            String pkg = selectedPackage(); if (pkg == null) return;
            try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg))); }
            catch (android.content.ActivityNotFoundException | SecurityException exception) { status.setText(R.string.fallback); }
        });
        Ui.section(this, content, R.string.background_actions);
        LinearLayout actions = Ui.card(this, content);
        enableButton = Ui.button(this, actions, R.string.apply_background, R.drawable.ic_battery, true, () -> confirm(true));
        resetButton = Ui.button(this, actions, R.string.reset_background, R.drawable.ic_refresh, false, () -> confirm(false));
        buttonsEnabled(false);
        Ui.section(this, content, R.string.session_status);
        LinearLayout session = Ui.card(this, content);
        status = Ui.text(this, session, getString(R.string.loading_apps), 14, false);
        status.setTextIsSelectable(true);
        LinearLayout help = Ui.card(this, content);
        Ui.button(this, help, R.string.understand_changes, R.drawable.ic_info, false, () ->
            new MaterialAlertDialogBuilder(this).setTitle(R.string.understand_changes)
                .setMessage(getString(R.string.advanced_help_detail, getString(R.string.advanced_scope), getString(R.string.app_list_privacy)))
                .setPositiveButton(android.R.string.ok, null).show());
        Ui.button(this, help, R.string.write_settings, R.drawable.ic_settings, false, () -> {
            try { startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()))); }
            catch (android.content.ActivityNotFoundException | SecurityException exception) { status.setText(R.string.fallback); }
        });
        final String requested = saved == null ? getIntent().getStringExtra("package") : saved.getString("selected_package");
        worker.execute(() -> {
            try {
                List<ApplicationInfo> found = getPackageManager().getInstalledApplications(0);
                found.sort((a, b) -> a.loadLabel(getPackageManager()).toString().compareToIgnoreCase(b.loadLabel(getPackageManager()).toString()));
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    apps.addAll(found);
                    List<String> labels = new ArrayList<>();
                    int selection = 0;
                    for (int i = 0; i < apps.size(); i++) {
                        ApplicationInfo app = apps.get(i);
                        labels.add(app.loadLabel(getPackageManager()) + "\n" + app.packageName);
                        if (app.packageName.equals(requested == null ? "com.google.android.gms" : requested)) selection = i;
                    }
                    appPicker.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
                    appPicker.setSelection(selection);
                    status.setText(R.string.choose_mode);
                    buttonsEnabled(!apps.isEmpty());
                });
            } catch (RuntimeException exception) {
                runOnUiThread(() -> { if (!isDestroyed()) status.setText(R.string.apps_load_failed); });
            }
        });
    }

    private String selectedPackage() {
        int index = appPicker.getSelectedItemPosition();
        return index >= 0 && index < apps.size() ? apps.get(index).packageName : null;
    }

    private void connect() {
        try {
            if (!Shizuku.pingBinder()) { status.setText(R.string.shizuku_missing); return; }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) { Shizuku.requestPermission(41); return; }
            if (!bound) { Shizuku.bindUserService(args, connection); bound = true; status.setText(R.string.connecting); }
            else status.setText(service == null ? R.string.connecting : R.string.shizuku_ready);
        } catch (Exception e) { status.setText(getString(R.string.operation_failed_detail, e.getMessage())); }
    }

    private void confirm(boolean enable) {
        String pkg = selectedPackage(); if (pkg == null || busy) return;
        boolean root = rootSelected;
        new MaterialAlertDialogBuilder(this).setTitle(enable ? R.string.apply_background : R.string.reset_background)
                .setMessage(getString(enable ? R.string.confirm_apply : R.string.confirm_reset) + "\n\n" + pkg + "\n" + (root ? "Root" : "Shizuku"))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog,which) -> apply(pkg,enable,root)).show();
    }

    private void apply(String pkg, boolean enable, boolean root) {
        final IPrivilegedService connected = service;
        if (!root && connected == null) { status.setText(R.string.shizuku_missing); return; }
        busy=true; buttonsEnabled(false); status.setText(R.string.executing);
        worker.execute(() -> {
            String result;
            try {
                result = root ? PrivilegedService.run(new String[]{"su","-c",CommandPolicy.rootCommand(pkg,enable)})
                        : connected.apply(pkg,enable);
            } catch (Exception e) { result="ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage(); }
            final String output=result;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                busy=false; buttonsEnabled(true);
                status.setText(getString(R.string.command_result_detail, output, getString(R.string.verify_device)));
            });
        });
    }

    private void buttonsEnabled(boolean enabled) {
        if (enableButton != null) enableButton.setEnabled(enabled);
        if (resetButton != null) resetButton.setEnabled(enabled);
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("root_selected", rootSelected);
        state.putString("selected_package", selectedPackage());
        super.onSaveInstanceState(state);
    }
    @Override protected void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        if (bound) { try { Shizuku.unbindUserService(args,connection,true); } catch (Exception ignored) {} }
        worker.shutdown(); super.onDestroy();
    }
}
