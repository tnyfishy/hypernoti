package dev.hypernoti;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,qualifiers="w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class UiScreenshotTest {
    private void preferences(String mode) {
        RuntimeEnvironment.getApplication().getSharedPreferences("hypernoti",Context.MODE_PRIVATE).edit().clear().putString("language","vi").putString("appearance",mode).putBoolean("dynamic_color",false).commit();
    }
    private void snapshot(UiActivity activity,String name) throws Exception {
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(500));
        View view=activity.findViewById(android.R.id.content);
        view.measure(View.MeasureSpec.makeMeasureSpec(411,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(891,View.MeasureSpec.EXACTLY));
        view.layout(0,0,411,891);
        Bitmap bitmap=Bitmap.createBitmap(411,891,Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        File output=new File("build/screenshots",name+".png");assertTrue(output.getParentFile().isDirectory() || output.getParentFile().mkdirs());
        try(FileOutputStream stream=new FileOutputStream(output)){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,stream));}
        bitmap.recycle();
    }
    @Test public void renderLightPages() throws Exception {
        preferences("light");
        try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity=controller.get();snapshot(activity,"home-light-vi");
            GlassSection device=activity.findViewById(R.id.device_section);device.header.performClick();snapshot(activity,"home-expanded-light-vi");device.header.performClick();
            BottomNavigationView nav=activity.findViewById(R.id.main_navigation);
            nav.setSelectedItemId(R.id.nav_checklist);snapshot(activity,"checklist-light-vi");
            nav.setSelectedItemId(R.id.nav_settings);snapshot(activity,"settings-light-vi");
            nav.setSelectedItemId(R.id.nav_apps);snapshot(activity,"apps-light-vi");
            assertNotNull(activity.findViewById(R.id.app_search));
        }
    }
    @Test public void renderDarkHome() throws Exception {
        preferences("dark");
        try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){snapshot(controller.get(),"home-dark-vi");}
    }
    @Test public void renderEnglishHome() throws Exception {
        preferences("light");RuntimeEnvironment.getApplication().getSharedPreferences("hypernoti",Context.MODE_PRIVATE).edit().putString("language","en").commit();
        try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()){snapshot(controller.get(),"home-light-en");}
    }
    @Test public void renderAdvancedWithoutExecutingCommands() throws Exception {
        preferences("light");
        try(ActivityController<AdvancedActivity> controller=Robolectric.buildActivity(AdvancedActivity.class).setup()) {
            snapshot(controller.get(),"advanced-light-vi");
            assertNotNull(controller.get().findViewById(R.id.root_mode_button));
        }
    }
}
