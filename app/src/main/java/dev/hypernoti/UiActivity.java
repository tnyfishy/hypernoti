package dev.hypernoti;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;
import com.google.android.material.color.DynamicColors;
import java.util.Locale;

public abstract class UiActivity extends AppCompatActivity {
    protected SharedPreferences preferences;

    @Override protected void attachBaseContext(Context base) {
        String language = base.getSharedPreferences("hypernoti", MODE_PRIVATE).getString("language", "");
        if (!language.isEmpty()) {
            Configuration configuration = new Configuration(base.getResources().getConfiguration());
            configuration.setLocale(Locale.forLanguageTag(language));
            base = base.createConfigurationContext(configuration);
        }
        super.attachBaseContext(base);
    }

    @Override protected void onCreate(Bundle saved) {
        preferences = getSharedPreferences("hypernoti", MODE_PRIVATE);
        String appearance = preferences.getString("appearance", "system");
        getDelegate().setLocalNightMode(appearance.equals("light") ? AppCompatDelegate.MODE_NIGHT_NO
                : appearance.equals("dark") || appearance.equals("amoled") ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        super.onCreate(saved);
        if (preferences.getBoolean("dynamic_color", true)) DynamicColors.applyToActivityIfAvailable(this);
        boolean dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (appearance.equals("amoled") && dark) getTheme().applyStyle(R.style.ThemeOverlay_HyperNoti_Amoled, true);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(!dark);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightNavigationBars(!dark);
    }
}
