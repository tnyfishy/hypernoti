package dev.hypernoti;

import android.app.Activity;
import android.app.AlertDialog;
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

public class AdvancedActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final List<ApplicationInfo> apps = new ArrayList<>();
    private Spinner appPicker, modePicker;
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

    @Override protected void attachBaseContext(android.content.Context base) {
        String language = base.getSharedPreferences("hypernoti", MODE_PRIVATE).getString("language", "");
        if (!language.isEmpty()) {
            android.content.res.Configuration config = new android.content.res.Configuration(base.getResources().getConfiguration());
            config.setLocale(java.util.Locale.forLanguageTag(language));
            base = base.createConfigurationContext(config);
        }
        super.attachBaseContext(base);
    }

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        Shizuku.addRequestPermissionResultListener(permissionListener);
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int)(20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding,padding,padding,padding);
        scroll.addView(layout); setContentView(scroll);
        label(layout, R.string.advanced);
        label(layout, R.string.advanced_scope);
        label(layout, R.string.app_list_privacy);
        appPicker = new Spinner(this); layout.addView(appPicker);
        status = label(layout, R.string.loading_apps);
        worker.execute(() -> {
            List<ApplicationInfo> found = getPackageManager().getInstalledApplications(0);
            found.sort((a,b) -> a.loadLabel(getPackageManager()).toString().compareToIgnoreCase(b.loadLabel(getPackageManager()).toString()));
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                apps.addAll(found);
                List<String> labels = new ArrayList<>();
                int gms = 0;
                for (int i=0;i<apps.size();i++) {
                    ApplicationInfo app=apps.get(i);
                    labels.add(app.loadLabel(getPackageManager()) + "\n" + app.packageName);
                    if (app.packageName.equals("com.google.android.gms")) gms=i;
                }
                appPicker.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
                appPicker.setSelection(gms);
                status.setText(R.string.choose_mode);
                buttonsEnabled(true);
            });
        });
        modePicker = new Spinner(this);
        modePicker.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Shizuku", getString(R.string.root_mode)})); layout.addView(modePicker);
        button(layout, R.string.connect_shizuku, this::connect);
        button(layout, R.string.write_settings, () -> {
            try { startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:"+getPackageName()))); }
            catch (Exception e) { status.setText(R.string.fallback); }
        });
        button(layout, R.string.selected_settings, () -> {
            String pkg = selectedPackage(); if (pkg == null) return;
            try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:"+pkg))); }
            catch (Exception e) { status.setText(R.string.fallback); }
        });
        enableButton = button(layout, R.string.apply_background, () -> confirm(true));
        resetButton = button(layout, R.string.reset_background, () -> confirm(false));
        buttonsEnabled(false);
        button(layout, R.string.back, this::finish);
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
        boolean root = modePicker.getSelectedItemPosition() == 1;
        new AlertDialog.Builder(this).setTitle(enable ? R.string.apply_background : R.string.reset_background)
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
    private TextView label(LinearLayout parent,int resource) {
        TextView text=new TextView(this);text.setText(resource);text.setTextSize(16);text.setPadding(0,12,0,12);parent.addView(text);return text;
    }
    private Button button(LinearLayout parent,int resource,Runnable action) {
        Button button=new Button(this);button.setText(resource);button.setAllCaps(false);button.setOnClickListener(v -> action.run());parent.addView(button);return button;
    }
    @Override protected void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        if (bound) { try { Shizuku.unbindUserService(args,connection,true); } catch (Exception ignored) {} }
        worker.shutdown(); super.onDestroy();
    }
}
