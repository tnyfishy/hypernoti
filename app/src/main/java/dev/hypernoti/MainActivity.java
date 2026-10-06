package dev.hypernoti;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.net.Uri;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends UiActivity {
    private static final int[] TABS = {R.id.nav_home, R.id.nav_apps, R.id.nav_checklist, R.id.nav_settings};
    private static final int[] TITLES = {R.string.title, R.string.nav_apps, R.string.nav_checklist, R.string.nav_settings};
    private static final int[] STEPS = {R.string.check_google, R.string.check_auto, R.string.check_battery, R.string.check_notify};
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final List<ApplicationInfo> applications = new ArrayList<>();
    private final View[] pages = new View[4];
    private FrameLayout pageContainer;
    private MaterialToolbar toolbar;
    private BottomNavigationView navigation;
    private MaterialCardView googleCard;
    private android.widget.ImageView googleIcon;
    private TextView googleDescription;
    private TextView googleStatus, homeProgress, checklistProgress, stepsCount, appsCount, appResult;
    private LinearProgressIndicator homeIndicator, checklistIndicator;
    private InstalledAppsAdapter appsAdapter;
    private TextInputEditText search;
    private int selectedTab = R.id.nav_home;
    private boolean applicationsLoaded;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        LinearLayout root = Ui.root(this);
        toolbar = Ui.toolbar(this, root, R.string.title, false);
        MenuItem refresh = toolbar.getMenu().add(getString(R.string.refresh));
        refresh.setIconTintList(ColorStateList.valueOf(Ui.color(this,com.google.android.material.R.attr.colorOnSurface)));
        refresh.setIcon(R.drawable.ic_refresh).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        refresh.setOnMenuItemClickListener(item -> { refresh(); loadApplications(); Toast.makeText(this, R.string.status_refreshed, Toast.LENGTH_SHORT).show(); return true; });
        MenuItem settings = toolbar.getMenu().add(getString(R.string.nav_settings));
        settings.setIconTintList(ColorStateList.valueOf(Ui.color(this,com.google.android.material.R.attr.colorOnSurface)));
        settings.setIcon(R.drawable.ic_settings).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        settings.setOnMenuItemClickListener(item -> { navigation.setSelectedItemId(R.id.nav_settings); return true; });
        pageContainer = new FrameLayout(this);
        root.addView(pageContainer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        addNavigation(root);
        selectedTab = saved == null ? R.id.nav_home : saved.getInt("selected_tab", R.id.nav_home);
        navigation.setSelectedItemId(selectedTab);
        showPage(selectedTab);
        loadApplications();
    }

    private void addNavigation(LinearLayout root) {
        FrameLayout area = new FrameLayout(this);
        root.addView(area, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 100)));
        MaterialCardView floating = new MaterialCardView(this);
        floating.setRadius(Ui.dp(this, 24)); floating.setCardElevation(Ui.dp(this, 4)); floating.setStrokeWidth(0);
        floating.setCardBackgroundColor(Ui.color(this, com.google.android.material.R.attr.colorSurfaceContainer));
        int width = Math.min(Ui.dp(this, 284), getResources().getDisplayMetrics().widthPixels - Ui.dp(this, 32));
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(width, Ui.dp(this, 72), Gravity.CENTER);
        area.addView(floating, cardParams);
        navigation = new BottomNavigationView(this); navigation.setId(R.id.main_navigation);
        navigation.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        navigation.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_UNLABELED);
        navigation.setItemHorizontalTranslationEnabled(false);
        navigation.setItemIconSize(Ui.dp(this, 24));
        navigation.setItemActiveIndicatorEnabled(true);
        navigation.setItemActiveIndicatorWidth(Ui.dp(this, 56)); navigation.setItemActiveIndicatorHeight(Ui.dp(this, 56));
        navigation.setItemActiveIndicatorColor(ColorStateList.valueOf(Ui.color(this, com.google.android.material.R.attr.colorSecondaryContainer)));
        navigation.setItemActiveIndicatorShapeAppearance(ShapeAppearanceModel.builder().setAllCornerSizes(Ui.dp(this, 16)).build());
        int primary = Ui.color(this, com.google.android.material.R.attr.colorPrimary);
        int normal = Ui.color(this, com.google.android.material.R.attr.colorOnSurfaceVariant);
        navigation.setItemIconTintList(new ColorStateList(new int[][]{{android.R.attr.state_checked},{}},new int[]{primary, normal}));
        int[] labels = {R.string.nav_home,R.string.nav_apps,R.string.nav_checklist,R.string.nav_settings};
        int[] icons = {R.drawable.ic_home,R.drawable.ic_apps,R.drawable.ic_checklist,R.drawable.ic_settings};
        for (int i=0;i<TABS.length;i++) navigation.getMenu().add(0,TABS[i],i,labels[i]).setIcon(icons[i]).setContentDescription(getString(labels[i]));
        floating.addView(navigation, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        navigation.setOnItemSelectedListener(item -> { showPage(item.getItemId()); return true; });
    }

    private void showPage(int id) {
        int index = 0; for (int i=0;i<TABS.length;i++) if (TABS[i] == id) index=i;
        if (selectedTab == R.id.nav_apps && TABS[index] != R.id.nav_apps && search != null) {
            search.clearFocus();
            ((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0);
        }
        selectedTab = TABS[index]; toolbar.setTitle(TITLES[index]);
        if (pages[index] == null) {
            FrameLayout page = new FrameLayout(this); pages[index] = page;
            if (index==0) home(page); else if (index==1) apps(page); else if (index==2) checklist(page); else settings(page);
        }
        pageContainer.removeAllViews(); pageContainer.addView(pages[index]);
        refresh(); updateProgress();
    }

    private void home(FrameLayout page) {
        LinearLayout content = Ui.scrolling(this, page);
        googleCard = Ui.card(this, content, Ui.color(this, com.google.android.material.R.attr.colorSecondaryContainer));
        LinearLayout status = Ui.cardContent(this, googleCard, 24);
        LinearLayout heading = Ui.row(this);
        googleIcon=Ui.icon(this,R.drawable.ic_shield,Ui.color(this,com.google.android.material.R.attr.colorOnSurface),32);
        heading.addView(googleIcon);
        LinearLayout copy = Ui.column(this); copy.setPadding(Ui.dp(this,20),0,0,0);
        heading.addView(copy,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        googleStatus = Ui.text(this, copy, "", 18, true);
        googleDescription=Ui.text(this, copy, getString(R.string.default_mode), 12, false);
        status.addView(heading);
        googleCard.setOnClickListener(view -> googleSettings());
        LinearLayout metrics = Ui.row(this);
        content.addView(metrics);
        LinearLayout appMetric = metric(metrics, R.drawable.ic_apps, R.string.nav_apps, false);
        appsCount = Ui.text(this,appMetric,getString(R.string.loading_count),28,true);
        LinearLayout stepMetric = metric(metrics, R.drawable.ic_checklist, R.string.nav_checklist, true);
        stepsCount = Ui.text(this,stepMetric,"",28,true);
        Ui.section(this,content,R.string.setup_overview);
        LinearLayout progressCard = Ui.card(this,content);
        Ui.text(this,progressCard,getString(R.string.setup_title),18,true);
        homeProgress = Ui.text(this,progressCard,"",13,false);
        homeIndicator = indicator(progressCard);
        Ui.button(this,progressCard,R.string.continue_setup,R.drawable.ic_checklist,false,() -> navigation.setSelectedItemId(R.id.nav_checklist));
        Ui.section(this,content,R.string.device_information);
        LinearLayout device = Ui.card(this,content);
        deviceValue(device,R.string.device,Build.MANUFACTURER + " " + Build.MODEL);
        deviceValue(device,R.string.android_version,getString(R.string.android_version_value,Build.VERSION.RELEASE,Build.VERSION.SDK_INT));
        deviceValue(device,R.string.rom,Build.DISPLAY);
        deviceValue(device,R.string.app_version,BuildConfig.VERSION_NAME);
        LinearLayout advanced = Ui.card(this,content);
        Ui.text(this,advanced,getString(R.string.privileged_access),18,true);
        Ui.text(this,advanced,getString(R.string.advanced_short),13,false);
        Ui.button(this,advanced,R.string.advanced,R.drawable.ic_shield,false,() -> advanced(null));
        LinearLayout limits = Ui.card(this,content);
        Ui.text(this,limits,getString(R.string.about_delivery),16,true);
        Ui.text(this,limits,getString(R.string.limits),13,false);
    }

    private LinearLayout metric(LinearLayout row, int icon, int title, boolean end) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(Ui.dp(this,16));card.setCardElevation(0);card.setStrokeWidth(0);
        card.setCardBackgroundColor(Ui.color(this,com.google.android.material.R.attr.colorSecondaryContainer));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1);
        params.bottomMargin=Ui.dp(this,16); if (end) params.leftMargin=Ui.dp(this,7); else params.rightMargin=Ui.dp(this,7);
        row.addView(card,params);
        LinearLayout content = Ui.cardContent(this,card,16);
        content.addView(Ui.icon(this,icon,Ui.color(this,com.google.android.material.R.attr.colorOnSurfaceVariant),24));
        Ui.text(this,content,getString(title),14,true);
        card.setOnClickListener(view -> navigation.setSelectedItemId(end ? R.id.nav_checklist : R.id.nav_apps));
        return content;
    }

    private void deviceValue(LinearLayout card,int label,String value) {
        Ui.text(this,card,getString(label),12,false);
        TextView text = Ui.text(this,card,value,14,true);text.setTextIsSelectable(true);
    }

    private LinearProgressIndicator indicator(LinearLayout card) {
        LinearProgressIndicator progress = new LinearProgressIndicator(this); progress.setMax(4);progress.setTrackCornerRadius(Ui.dp(this,4));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Ui.dp(this,6));
        params.topMargin=Ui.dp(this,8);params.bottomMargin=Ui.dp(this,16);card.addView(progress,params);return progress;
    }

    private void checklist(FrameLayout page) {
        LinearLayout content=Ui.scrolling(this,page);
        LinearLayout intro=Ui.card(this,content);
        Ui.text(this,intro,getString(R.string.setup_title),20,true);
        checklistProgress=Ui.text(this,intro,"",13,false);checklistIndicator=indicator(intro);
        LinearLayout checkCard=Ui.card(this,content);
        for (int i=0;i<STEPS.length;i++) {
            final int index=i;
            MaterialCheckBox check=new MaterialCheckBox(this);check.setText(STEPS[i]);check.setTextSize(15);
            check.setTextColor(Ui.color(this,com.google.android.material.R.attr.colorOnSurface));
            check.setMinHeight(Ui.dp(this,56));check.setChecked(preferences.getBoolean("step_"+i,false));
            check.setOnCheckedChangeListener((button,checked) -> {preferences.edit().putBoolean("step_"+index,checked).apply();updateProgress();});
            checkCard.addView(check,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        Ui.section(this,content,R.string.quick_settings);
        LinearLayout shortcuts=Ui.cardContent(this,Ui.card(this,content,Ui.color(this,com.google.android.material.R.attr.colorSurfaceContainer)),0);
        Ui.setting(this,shortcuts,R.drawable.ic_language,R.string.google_settings,null,this::googleSettings);
        Ui.setting(this,shortcuts,R.drawable.ic_shield,R.string.autostart,null,() -> open(new Intent().setComponent(new android.content.ComponentName("com.miui.securitycenter","com.miui.permcenter.autostart.AutoStartManagementActivity"))));
        Ui.setting(this,shortcuts,R.drawable.ic_battery,R.string.battery,null,() -> open(new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)));
        Ui.setting(this,shortcuts,R.drawable.ic_apps,R.string.apps,null,() -> open(new Intent(Settings.ACTION_APPLICATION_SETTINGS)));
        LinearLayout guide=Ui.card(this,content);Ui.text(this,guide,getString(R.string.guide),14,false);
        Ui.button(this,guide,R.string.test,R.drawable.ic_notification,true,() -> new MaterialAlertDialogBuilder(this).setTitle(R.string.test).setMessage(R.string.test_body).setPositiveButton(android.R.string.ok,null).show());
    }

    private void apps(FrameLayout page) {
        LinearLayout content=Ui.column(this);content.setPadding(Ui.dp(this,16),Ui.dp(this,8),Ui.dp(this,16),0);page.addView(content);
        TextInputLayout input = new TextInputLayout(this,null,com.google.android.material.R.attr.textInputOutlinedStyle);
        input.setHint(getString(R.string.search_apps));input.setBoxCornerRadii(Ui.dp(this,24),Ui.dp(this,24),Ui.dp(this,24),Ui.dp(this,24));
        search=new TextInputEditText(input.getContext());search.setId(R.id.app_search);search.setSingleLine(true);search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);input.addView(search);
        content.addView(input,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        Ui.text(this,content,getString(R.string.apps_help),13,false);
        appResult=Ui.text(this,content,getString(R.string.loading_apps),13,false);
        RecyclerView list=new RecyclerView(this);list.setId(R.id.app_list);list.setLayoutManager(new LinearLayoutManager(this));
        appsAdapter=new InstalledAppsAdapter(this, this::advanced, this::openApplication);
        list.setAdapter(appsAdapter);content.addView(list,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count) { filterApplications(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        filterApplications();
    }

    private void settings(FrameLayout page) {
        LinearLayout content=Ui.scrolling(this,page);
        Ui.section(this,content,R.string.appearance_section);
        LinearLayout appearance=Ui.cardContent(this,Ui.card(this,content,Ui.color(this,com.google.android.material.R.attr.colorSurfaceContainer)),0);
        String language=preferences.getString("language","");
        LinearLayout languageRow=Ui.setting(this,appearance,R.drawable.ic_language,R.string.language,language.equals("vi") ? "Tiếng Việt" : language.equals("en") ? "English" : getString(R.string.system_default),this::languageDialog);
        languageRow.setId(R.id.language_setting);
        LinearLayout themeRow=Ui.setting(this,appearance,R.drawable.ic_palette,R.string.appearance,appearanceLabel(),this::appearanceDialog);
        themeRow.setId(R.id.theme_setting);
        LinearLayout dynamic=Ui.row(this);dynamic.setPadding(Ui.dp(this,16),Ui.dp(this,8),Ui.dp(this,16),Ui.dp(this,12));
        LinearLayout labels=Ui.column(this);dynamic.addView(labels,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        Ui.text(this,labels,getString(R.string.dynamic_colors),16,true);Ui.text(this,labels,getString(Build.VERSION.SDK_INT>=31 ? R.string.dynamic_colors_summary : R.string.dynamic_colors_unavailable),13,false);
        MaterialSwitch monet=new MaterialSwitch(this);monet.setId(R.id.dynamic_color_setting);monet.setContentDescription(getString(R.string.dynamic_colors));
        monet.setChecked(preferences.getBoolean("dynamic_color",true));monet.setEnabled(Build.VERSION.SDK_INT>=31);
        monet.setOnCheckedChangeListener((view,checked) -> {preferences.edit().putBoolean("dynamic_color",checked).apply();recreate();});dynamic.addView(monet);appearance.addView(dynamic);
        Ui.section(this,content,R.string.privileged_access);
        LinearLayout access=Ui.cardContent(this,Ui.card(this,content,Ui.color(this,com.google.android.material.R.attr.colorSurfaceContainer)),0);
        Ui.setting(this,access,R.drawable.ic_shield,R.string.advanced,getString(R.string.advanced_short),() -> advanced(null));
        Ui.setting(this,access,R.drawable.ic_settings,R.string.write_settings,null,() -> open(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+getPackageName()))));
        Ui.section(this,content,R.string.about);
        LinearLayout about=Ui.card(this,content);Ui.text(this,about,getString(R.string.title),22,true);
        Ui.text(this,about,getString(R.string.version_value,BuildConfig.VERSION_NAME),13,false);
        Ui.text(this,about,getString(R.string.subtitle),14,false);Ui.text(this,about,getString(R.string.limits),13,false);
        Ui.text(this,about,getString(R.string.app_list_privacy),13,false);
    }

    private String appearanceLabel() {
        String value=preferences.getString("appearance","system");
        return getString(value.equals("light") ? R.string.theme_light : value.equals("dark") ? R.string.theme_dark : value.equals("amoled") ? R.string.theme_amoled : R.string.system_default);
    }
    private void languageDialog() {
        String current=preferences.getString("language","");
        new MaterialAlertDialogBuilder(this).setTitle(R.string.language).setSingleChoiceItems(new String[]{getString(R.string.system_default),"English","Tiếng Việt"},current.equals("en") ? 1 : current.equals("vi") ? 2 : 0,(dialog,which) -> {
            preferences.edit().putString("language",new String[]{"","en","vi"}[which]).apply();dialog.dismiss();recreate();
        }).setNegativeButton(android.R.string.cancel,null).show();
    }
    private void appearanceDialog() {
        String[] modes={"system","light","dark","amoled"};int current=0;for(int i=0;i<modes.length;i++) if(modes[i].equals(preferences.getString("appearance","system"))) current=i;
        new MaterialAlertDialogBuilder(this).setTitle(R.string.appearance).setSingleChoiceItems(new String[]{getString(R.string.system_default),getString(R.string.theme_light),getString(R.string.theme_dark),getString(R.string.theme_amoled)},current,(dialog,which) -> {
            preferences.edit().putString("appearance",modes[which]).apply();dialog.dismiss();recreate();
        }).setNegativeButton(android.R.string.cancel,null).show();
    }

    private void loadApplications() {
        worker.execute(() -> {
            try {
                List<ApplicationInfo> found=getPackageManager().getInstalledApplications(0);
                found.sort((a,b) -> a.loadLabel(getPackageManager()).toString().compareToIgnoreCase(b.loadLabel(getPackageManager()).toString()));
                runOnUiThread(() -> {if(isDestroyed())return;applications.clear();applications.addAll(found);applicationsLoaded=true;if(appsCount!=null)appsCount.setText(getString(R.string.count_value,found.size()));filterApplications();});
            } catch (RuntimeException exception) {
                runOnUiThread(() -> {if(!isDestroyed() && appResult!=null)appResult.setText(R.string.apps_load_failed);});
            }
        });
    }
    private void filterApplications() {
        if (appsAdapter==null) return;
        String query=search.getText()==null ? "" : search.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<ApplicationInfo> filtered=new ArrayList<>();
        for(ApplicationInfo info:applications) if(info.packageName.toLowerCase(Locale.ROOT).contains(query) || info.loadLabel(getPackageManager()).toString().toLowerCase(Locale.ROOT).contains(query)) filtered.add(info);
        appsAdapter.submit(filtered);
        appResult.setText(!applicationsLoaded ? getString(R.string.loading_apps) : filtered.isEmpty() ? getString(R.string.no_apps) : getResources().getQuantityString(R.plurals.app_count,filtered.size(),filtered.size()));
    }
    private void advanced(String pkg) {Intent intent=new Intent(this,AdvancedActivity.class);if(pkg!=null)intent.putExtra("package",pkg);startActivity(intent);}
    private void openApplication(String pkg) {open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+pkg)));}
    private void googleSettings() {openApplication("com.google.android.gms");}
    private void open(Intent intent) {try{startActivity(intent);}catch(android.content.ActivityNotFoundException | SecurityException exception){Toast.makeText(this,R.string.fallback,Toast.LENGTH_LONG).show();}}

    @Override protected void onResume() {super.onResume();refresh();}
    private void refresh() {
        if(googleStatus==null)return;
        boolean installed=false,enabled=false;
        try{ApplicationInfo info=getPackageManager().getApplicationInfo("com.google.android.gms",0);installed=true;enabled=info.enabled;}catch(PackageManager.NameNotFoundException ignored){}
        boolean available=Readiness.googleAvailable(installed,enabled);
        googleStatus.setText(available ? R.string.google_ok : R.string.google_missing);
        googleCard.setCardBackgroundColor(Ui.color(this,available ? com.google.android.material.R.attr.colorPrimary : com.google.android.material.R.attr.colorErrorContainer));
        int foreground=Ui.color(this,available ? com.google.android.material.R.attr.colorOnPrimary : com.google.android.material.R.attr.colorOnErrorContainer);
        googleStatus.setTextColor(foreground);googleDescription.setTextColor(foreground);googleIcon.setImageTintList(ColorStateList.valueOf(foreground));
        if(appsCount!=null && applicationsLoaded)appsCount.setText(getString(R.string.count_value,applications.size()));
    }
    private void updateProgress() {
        boolean[] completed=new boolean[STEPS.length];for(int i=0;i<STEPS.length;i++)completed[i]=preferences.getBoolean("step_"+i,false);
        int count=Readiness.completed(completed);
        if(homeProgress!=null)homeProgress.setText(getString(R.string.progress,count));
        if(checklistProgress!=null)checklistProgress.setText(getString(R.string.progress,count));
        if(stepsCount!=null)stepsCount.setText(getString(R.string.step_count,count));
        if(homeIndicator!=null)homeIndicator.setProgress(count);if(checklistIndicator!=null)checklistIndicator.setProgress(count);
    }
    @Override protected void onSaveInstanceState(Bundle state){state.putInt("selected_tab",selectedTab);super.onSaveInstanceState(state);}
    @Override protected void onDestroy(){worker.shutdown();super.onDestroy();}
}
