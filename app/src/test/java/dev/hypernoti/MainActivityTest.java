package dev.hypernoti;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class MainActivityTest {
 @Before public void clearPreferences() { RuntimeEnvironment.getApplication().getSharedPreferences("hypernoti",Context.MODE_PRIVATE).edit().clear().commit(); }
 private View find(View view, String label) {
  if (view instanceof TextView && ((TextView)view).getText().toString().equals(label)) return view;
  if (view instanceof ViewGroup) for (int i=0;i<((ViewGroup)view).getChildCount();i++) {
   View found=find(((ViewGroup)view).getChildAt(i),label); if (found!=null) return found;
  }
  return null;
 }
 @Test public void launchShowsMissingServicesAndNoCompletedSteps() {
  try (ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()) {
   MainActivity activity=controller.get(); View root=activity.getWindow().getDecorView();
   assertNotNull(find(root,activity.getString(R.string.google_missing)));
   assertNotNull(find(root,activity.getString(R.string.progress,0)));
   assertNotNull(find(root,activity.getString(R.string.advanced)));
  }
 }
 @Test public void checklistPersistsAcrossActivityRestart() {
  try (ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()) {
   MainActivity activity=controller.get();
   ((BottomNavigationView)activity.findViewById(R.id.main_navigation)).setSelectedItemId(R.id.nav_checklist);
   CheckBox check=(CheckBox)find(activity.getWindow().getDecorView(),activity.getString(R.string.check_google));
   check.setChecked(true);
   assertNotNull(find(activity.getWindow().getDecorView(),activity.getString(R.string.progress,1)));
  }
  try (ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()) {
   MainActivity activity=controller.get();
   ((BottomNavigationView)activity.findViewById(R.id.main_navigation)).setSelectedItemId(R.id.nav_checklist);
   assertTrue(((CheckBox)find(activity.getWindow().getDecorView(),activity.getString(R.string.check_google))).isChecked());
  }
 }
 @Test public void savedVietnameseSelectionAppliesToActivity() {
  RuntimeEnvironment.getApplication().getSharedPreferences("hypernoti",Context.MODE_PRIVATE).edit().putString("language","vi").commit();
  try (ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup()) {
   MainActivity activity=controller.get();
   assertEquals("Cài đặt nâng cao",activity.getString(R.string.advanced));
   assertNotNull(find(activity.getWindow().getDecorView(),"Cài đặt nâng cao"));
  }
 }
}
