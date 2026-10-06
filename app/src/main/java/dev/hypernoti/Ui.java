package dev.hypernoti;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

final class Ui {
    private Ui() {}
    static int dp(Context context, float value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
    static int color(Context context, int attribute) { return MaterialColors.getColor(context, attribute, "HyperNoti"); }
    static LinearLayout column(Context context) {
        LinearLayout layout = new LinearLayout(context); layout.setOrientation(LinearLayout.VERTICAL); return layout;
    }
    static LinearLayout row(Context context) {
        LinearLayout layout = new LinearLayout(context); layout.setGravity(Gravity.CENTER_VERTICAL); return layout;
    }
    static LinearLayout root(UiActivity activity) {
        LinearLayout root = column(activity);
        root.setBackground(Glass.backdrop(activity));
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        activity.setContentView(root);
        ViewCompat.requestApplyInsets(root);
        return root;
    }
    static MaterialToolbar toolbar(UiActivity activity, LinearLayout parent, int title, boolean back) {
        MaterialToolbar toolbar = new MaterialToolbar(activity);
        toolbar.setNavigationIconTint(color(activity, com.google.android.material.R.attr.colorOnSurface));
        toolbar.setTitle(title); toolbar.setTitleTextColor(color(activity, com.google.android.material.R.attr.colorOnSurface));
        if (back) { toolbar.setNavigationIcon(R.drawable.ic_back); toolbar.setNavigationContentDescription(R.string.back); toolbar.setNavigationOnClickListener(view -> activity.finish()); }
        parent.addView(toolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 64)));
        return toolbar;
    }
    static LinearLayout scrolling(Context context, ViewGroup parent) {
        ScrollView scroll = new ScrollView(context); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        LinearLayout content = column(context); int padding = dp(context, 16); content.setPadding(padding, dp(context, 8), padding, padding);
        scroll.addView(content);
        parent.addView(scroll, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return content;
    }
    static MaterialCardView card(Context context, LinearLayout parent, int color) {
        MaterialCardView card = Glass.card(context, color);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(context, 16); parent.addView(card, params); return card;
    }
    static LinearLayout cardContent(Context context, MaterialCardView card, int padding) {
        LinearLayout content = column(context); int inset = dp(context, padding); content.setPadding(inset, inset, inset, inset);
        card.addView(content); return content;
    }
    static LinearLayout card(Context context, LinearLayout parent) {
        return cardContent(context, card(context, parent, color(context, com.google.android.material.R.attr.colorSurfaceContainer)), 20);
    }
    static TextView text(Context context, LinearLayout parent, CharSequence value, int size, boolean bold) {
        TextView text = new TextView(context); text.setText(value); text.setTextSize(size);
        text.setTextColor(color(context, bold ? com.google.android.material.R.attr.colorOnSurface : com.google.android.material.R.attr.colorOnSurfaceVariant));
        if (bold) text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        text.setLineSpacing(dp(context, 2), 1f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(context, 8); parent.addView(text, params); return text;
    }
    static void section(Context context, LinearLayout parent, int title) {
        TextView text = text(context, parent, context.getString(title), 14, true);
        text.setTextColor(color(context, com.google.android.material.R.attr.colorPrimary));
        text.setPadding(dp(context, 4), dp(context, 4), 0, dp(context, 4));
    }
    static ImageView icon(Context context, int drawable, int tint, int size) {
        ImageView image = new ImageView(context); image.setImageResource(drawable); image.setImageTintList(ColorStateList.valueOf(tint));
        image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        image.setLayoutParams(new LinearLayout.LayoutParams(dp(context, size), dp(context, size))); return image;
    }
    static MaterialButton button(Context context, LinearLayout parent, int title, int icon, boolean primary, Runnable action) {
        MaterialButton button = new MaterialButton(context, null, primary ? com.google.android.material.R.attr.materialButtonStyle : com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(title); button.setAllCaps(false); button.setCornerRadius(dp(context, 24)); button.setMinHeight(dp(context, 48));
        button.setInsetTop(dp(context, 4)); button.setInsetBottom(dp(context, 4));
        if (icon != 0) { button.setIconResource(icon); button.setIconSize(dp(context, 20)); }
        button.setOnClickListener(view -> action.run()); Motion.tactile(button);
        parent.addView(button, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)); return button;
    }
    static LinearLayout setting(Context context, LinearLayout parent, int icon, int title, CharSequence detail, Runnable action) {
        LinearLayout row = row(context); row.setMinimumHeight(dp(context, 72)); row.setPadding(dp(context, 16), dp(context, 10), dp(context, 16), dp(context, 10));
        row.addView(icon(context, icon, color(context, com.google.android.material.R.attr.colorOnSurfaceVariant), 24));
        LinearLayout labels = column(context); labels.setPadding(dp(context, 16), 0, dp(context, 8), 0);
        row.addView(labels, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        text(context, labels, context.getString(title), 16, true);
        if (detail != null && detail.length() > 0) { TextView subtitle = text(context, labels, detail, 13, false); subtitle.setPadding(0,0,0,0); }
        row.addView(icon(context, R.drawable.ic_chevron, color(context, com.google.android.material.R.attr.colorOnSurfaceVariant), 20));
        android.util.TypedValue ripple = new android.util.TypedValue(); context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
        row.setBackgroundResource(ripple.resourceId); row.setOnClickListener(view -> action.run()); Motion.tactile(row);
        parent.addView(row); return row;
    }
}
