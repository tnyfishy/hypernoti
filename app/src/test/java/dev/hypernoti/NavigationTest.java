package dev.hypernoti;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Looper;
import android.view.View;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class NavigationTest {
    @Before public void reset() {
        RuntimeEnvironment.getApplication().getSharedPreferences("hypernoti",Context.MODE_PRIVATE).edit().clear().putBoolean("dynamic_color",false).commit();
    }
    @Test public void appsSearchSurvivesTabSwitch() {
        try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity=controller.get();
            BottomNavigationView nav=activity.findViewById(R.id.main_navigation);
            nav.setSelectedItemId(R.id.nav_apps);
            TextInputEditText search=activity.findViewById(R.id.app_search);
            search.setText("google");
            nav.setSelectedItemId(R.id.nav_settings);
            nav.setSelectedItemId(R.id.nav_apps);
            assertEquals("google",((TextInputEditText)activity.findViewById(R.id.app_search)).getText().toString());
        }
    }
    @Test public void restoredTabShowsSettingsWithoutPrivilegeRequest() {
        try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()) {
            BottomNavigationView nav=controller.get().findViewById(R.id.main_navigation);
            nav.setSelectedItemId(R.id.nav_settings);
            controller.recreate();
            assertEquals(R.id.nav_settings,((BottomNavigationView)controller.get().findViewById(R.id.main_navigation)).getSelectedItemId());
            assertNotNull(controller.get().findViewById(R.id.theme_setting));
        }
    }
    @Test public void darkPreferenceAppliesNightResources() {
        RuntimeEnvironment.getApplication().getSharedPreferences("hypernoti",Context.MODE_PRIVATE).edit().putString("appearance","dark").commit();
        try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()) {
            assertEquals(Configuration.UI_MODE_NIGHT_YES,controller.get().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK);
        }
    }
    @Test public void amoledPreferenceUsesBlackBackground() {
        RuntimeEnvironment.getApplication().getSharedPreferences("hypernoti",Context.MODE_PRIVATE).edit().putString("appearance","amoled").commit();
        try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()) {
            assertEquals(Color.BLACK,Ui.color(controller.get(),com.google.android.material.R.attr.colorSurface));
        }
    }
    @Test public void languageChoicePersistsAndAppliesOnNextLaunch() {
        try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity=controller.get();
            ((BottomNavigationView)activity.findViewById(R.id.main_navigation)).setSelectedItemId(R.id.nav_settings);
            activity.findViewById(R.id.language_setting).performClick();
            android.app.Dialog dialog=org.robolectric.shadows.ShadowDialog.getLatestDialog();
            androidx.appcompat.app.AlertDialog alert=(androidx.appcompat.app.AlertDialog)dialog;
            alert.getListView().performItemClick(alert.getListView().getChildAt(2),2,2);
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertEquals("vi",activity.getSharedPreferences("hypernoti",Context.MODE_PRIVATE).getString("language",""));
        }
        try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()) {
            assertEquals("Cài đặt",controller.get().getString(R.string.nav_settings));
        }
    }
}
