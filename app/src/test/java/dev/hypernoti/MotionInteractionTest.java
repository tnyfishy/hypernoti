package dev.hypernoti;

import android.content.Context;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import java.time.Duration;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,qualifiers="w411dp-h891dp-mdpi")
public class MotionInteractionTest {
    @Before public void reset() {
        RuntimeEnvironment.getApplication().getSharedPreferences("hypernoti",Context.MODE_PRIVATE).edit().clear().putBoolean("dynamic_color",false).commit();
    }
    private void layout(MainActivity activity) {
        View view=activity.findViewById(android.R.id.content);
        view.measure(View.MeasureSpec.makeMeasureSpec(411,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(891,View.MeasureSpec.EXACTLY));view.layout(0,0,411,891);
    }
    private void idle(long millis) {Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis));}
    @Test public void reversingSectionMidAnimationSettlesAndSurvivesRecreation() {
        try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity=controller.get();layout(activity);
            GlassSection section=activity.findViewById(R.id.device_section);
            assertEquals(View.GONE,section.body.getVisibility());
            section.header.performClick();idle(100);layout(activity);
            section.header.performClick();idle(80);layout(activity);
            section.header.performClick();idle(500);layout(activity);
            assertTrue(section.isExpanded());assertEquals(View.VISIBLE,section.body.getVisibility());
            assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT,section.body.getLayoutParams().height);
            assertEquals(1f,section.body.getAlpha(),0);assertTrue(section.body.getHeight()>0);
            controller.recreate();section=controller.get().findViewById(R.id.device_section);
            assertTrue(section.isExpanded());
            section.header.performClick();idle(500);
            assertEquals(View.GONE,section.body.getVisibility());
        }
    }
    @Test public void reduceMotionShowsFinalStateImmediatelyAndRapidTabsStayUsable() {
        RuntimeEnvironment.getApplication().getSharedPreferences("hypernoti",Context.MODE_PRIVATE).edit().putBoolean("reduce_motion",true).commit();
        try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity=controller.get();layout(activity);
            assertFalse(Motion.enabled(activity));
            GlassSection section=activity.findViewById(R.id.device_section);section.header.performClick();
            assertEquals(View.VISIBLE,section.body.getVisibility());assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT,section.body.getLayoutParams().height);
            section.header.performClick();assertEquals(View.GONE,section.body.getVisibility());
            BottomNavigationView nav=activity.findViewById(R.id.main_navigation);
            nav.setSelectedItemId(R.id.nav_apps);nav.setSelectedItemId(R.id.nav_settings);nav.setSelectedItemId(R.id.nav_home);nav.setSelectedItemId(R.id.nav_apps);
            View page=(View)activity.findViewById(R.id.app_search).getParent();
            while(page.getParent() instanceof View && ! (page.getParent() instanceof android.widget.FrameLayout))page=(View)page.getParent();
            assertEquals(1f,page.getAlpha(),0);assertNotNull(activity.findViewById(R.id.app_list));
            assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
        }
    }
    @Test public void systemAnimatorOffDisablesCustomMotion() throws Exception {
        java.lang.reflect.Method duration=android.animation.ValueAnimator.class.getDeclaredMethod("setDurationScale",float.class);
        duration.setAccessible(true);
        try {
            duration.invoke(null,0f);
            assertFalse(Motion.enabled(RuntimeEnvironment.getApplication()));
        } finally {duration.invoke(null,1f);}
    }
    @Test public void leavingAnExpandingSectionRestoresItsFinalStateOnReturn() {
        try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity=controller.get();controller.visible();layout(activity);
            GlassSection section=activity.findViewById(R.id.device_section);section.header.performClick();idle(90);
            BottomNavigationView nav=activity.findViewById(R.id.main_navigation);nav.setSelectedItemId(R.id.nav_settings);nav.setSelectedItemId(R.id.nav_home);idle(500);layout(activity);
            assertEquals(View.VISIBLE,section.body.getVisibility());assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT,section.body.getLayoutParams().height);assertEquals(1f,section.body.getAlpha(),0);
        }
    }
}
