package dev.hypernoti;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class MainActivity extends Activity {
    private SharedPreferences prefs;
    private TextView googleStatus, progress;
    private final int[] steps = {R.string.check_google, R.string.check_auto, R.string.check_battery, R.string.check_notify};

    @Override protected void attachBaseContext(Context base) {
        String language = base.getSharedPreferences("hypernoti", MODE_PRIVATE).getString("language", "");
        if (!language.isEmpty()) {
            Configuration config = new Configuration(base.getResources().getConfiguration());
            config.setLocale(Locale.forLanguageTag(language));
            base = base.createConfigurationContext(config);
        }
        super.attachBaseContext(base);
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("hypernoti", MODE_PRIVATE);
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding, padding, padding);
        content.setBackgroundColor(Color.rgb(245, 247, 252));
        scroll.addView(content);
        setContentView(scroll);
        TextView title = text(content, getString(R.string.title), 30);
        title.setTextColor(Color.rgb(40, 67, 145));
        text(content, getString(R.string.subtitle), 18);
        Button language = new Button(this);
        language.setText(R.string.language_selector);
        language.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Language / Ngôn ngữ")
                .setItems(new String[]{"English", "Tiếng Việt"}, (dialog, which) -> {
                    prefs.edit().putString("language", which == 0 ? "en" : "vi").apply();
                    recreate();
                }).show());
        content.addView(language);
        text(content, getString(R.string.limits), 16);
        googleStatus = text(content, "", 18);
        button(content, R.string.google_settings, v -> open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:com.google.android.gms"))));
        button(content, R.string.autostart, v -> {
            Intent intent = new Intent().setComponent(new android.content.ComponentName(
                    "com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            open(intent);
        });
        button(content, R.string.battery, v -> open(new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)));
        button(content, R.string.apps, v -> open(new Intent(Settings.ACTION_APPLICATION_SETTINGS)));
        text(content, getString(R.string.guide), 16);
        for (int i = 0; i < steps.length; i++) {
            final int index = i;
            CheckBox check = new CheckBox(this);
            check.setText(steps[i]);
            check.setChecked(prefs.getBoolean("step_" + i, false));
            check.setOnCheckedChangeListener((view, checked) -> {
                prefs.edit().putBoolean("step_" + index, checked).apply();
                updateProgress();
            });
            content.addView(check);
        }
        progress = text(content, "", 16);
        updateProgress();
        button(content, R.string.test, v -> new AlertDialog.Builder(this).setTitle(R.string.test)
                .setMessage(R.string.test_body).setPositiveButton(android.R.string.ok, null).show());
        button(content, R.string.refresh, v -> refresh());
        button(content, R.string.advanced, v -> startActivity(new Intent(this, AdvancedActivity.class)));
    }

    @Override protected void onResume() { super.onResume(); if (googleStatus != null) refresh(); }

    private void refresh() {
        boolean installed = false, enabled = false;
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo("com.google.android.gms", 0);
            installed = true;
            enabled = info.enabled;
        } catch (PackageManager.NameNotFoundException ignored) { }
        boolean available = Readiness.googleAvailable(installed, enabled);
        googleStatus.setText(available ? R.string.google_ok : R.string.google_missing);
        googleStatus.setTextColor(available ? Color.rgb(20, 100, 65) : Color.rgb(155, 50, 40));
    }

    private void updateProgress() {
        boolean[] confirmed = new boolean[steps.length];
        for (int i = 0; i < steps.length; i++) confirmed[i] = prefs.getBoolean("step_" + i, false);
        progress.setText(getString(R.string.progress, Readiness.completed(confirmed)));
    }

    private void open(Intent intent) {
        try { startActivity(intent); }
        catch (android.content.ActivityNotFoundException | SecurityException exception) {
            Toast.makeText(this, R.string.fallback, Toast.LENGTH_LONG).show();
        }
    }

    private TextView text(LinearLayout parent, String value, int size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(Color.rgb(30, 39, 58));
        view.setPadding(0, 12, 0, 12);
        parent.addView(view);
        return view;
    }

    private void button(LinearLayout parent, int label, View.OnClickListener action) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(action);
        parent.addView(button);
    }
}
