package dev.hypernoti;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
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
public class AdvancedInteractionTest {
    @Before public void reset() {
        RuntimeEnvironment.getApplication().getSharedPreferences("hypernoti",Context.MODE_PRIVATE).edit().clear().commit();
        PackageInfo info=new PackageInfo();info.packageName="com.example.notifications";
        info.applicationInfo=new ApplicationInfo();info.applicationInfo.packageName=info.packageName;info.applicationInfo.nonLocalizedLabel="Notification app";
        Shadows.shadowOf(RuntimeEnvironment.getApplication().getPackageManager()).installPackage(info);
    }
    private View find(View view,String text) {
        if(view instanceof TextView && ((TextView)view).getText().toString().equals(text))return view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View result=find(((ViewGroup)view).getChildAt(i),text);if(result!=null)return result;}
        return null;
    }
    private void loaded(AdvancedActivity activity) throws Exception {
        Button apply=(Button)find(activity.getWindow().getDecorView(),activity.getString(R.string.apply_background));
        for(int i=0;i<100 && !apply.isEnabled();i++){Thread.sleep(10);Shadows.shadowOf(Looper.getMainLooper()).idle();}
        assertTrue("App list should finish loading",apply.isEnabled());
    }
    @Test public void requestedPackageIsSelected() throws Exception {
        Intent intent=new Intent(RuntimeEnvironment.getApplication(),AdvancedActivity.class).putExtra("package","com.example.notifications");
        try(ActivityController<AdvancedActivity> controller=Robolectric.buildActivity(AdvancedActivity.class,intent).setup()) {
            AdvancedActivity activity=controller.get();loaded(activity);
            Spinner picker=findSpinner(activity.getWindow().getDecorView());
            assertTrue(picker.getSelectedItem().toString().contains("com.example.notifications"));
        }
    }
    private Spinner findSpinner(View view) {
        if(view instanceof Spinner)return (Spinner)view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){Spinner found=findSpinner(((ViewGroup)view).getChildAt(i));if(found!=null)return found;}
        return null;
    }
    @Test public void rootChoiceRequiresConfirmationAndCancelPreservesSession() throws Exception {
        try(ActivityController<AdvancedActivity> controller=Robolectric.buildActivity(AdvancedActivity.class).setup()) {
            AdvancedActivity activity=controller.get();loaded(activity);
            activity.findViewById(R.id.root_mode_button).performClick();
            assertNotNull(find(activity.getWindow().getDecorView(),activity.getString(R.string.choose_mode)));
            find(activity.getWindow().getDecorView(),activity.getString(R.string.apply_background)).performClick();
            AlertDialog alert=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
            assertNotNull(alert);
            alert.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            assertNotNull(find(activity.getWindow().getDecorView(),activity.getString(R.string.choose_mode)));
            assertNull(find(activity.getWindow().getDecorView(),activity.getString(R.string.executing)));
        }
    }
    @Test public void missingShizukuShowsStatusWithoutChangingPermissions() throws Exception {
        try(ActivityController<AdvancedActivity> controller=Robolectric.buildActivity(AdvancedActivity.class).setup()) {
            AdvancedActivity activity=controller.get();loaded(activity);
            find(activity.getWindow().getDecorView(),activity.getString(R.string.connect_shizuku)).performClick();
            assertNotNull(find(activity.getWindow().getDecorView(),activity.getString(R.string.shizuku_missing)));
        }
    }
}
