package dev.hypernoti;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.core.view.ViewCompat;

/** Reversible accordion; cancellation starts from the currently displayed height. */
// Constructed in Java with required section metadata; never inflated from XML.
@android.annotation.SuppressLint("ViewConstructor")
final class GlassSection extends LinearLayout {
    final LinearLayout body;
    final LinearLayout header;
    private final ImageView chevron;
    private final SharedPreferences preferences;
    private final String key;
    private boolean expanded;
    private ValueAnimator animator;
    private int generation;

    GlassSection(Context context, LinearLayout parent, int title, int icon, String key, boolean initiallyOpen) {
        super(context); setOrientation(VERTICAL); this.key="section_"+key;
        preferences=context.getSharedPreferences("hypernoti",Context.MODE_PRIVATE);
        expanded=preferences.getBoolean(this.key,initiallyOpen);
        Ui.card(context,parent,Ui.color(context,com.google.android.material.R.attr.colorSurfaceContainer)).addView(this);
        header=Ui.row(context); header.setMinimumHeight(Ui.dp(context,64));
        header.setPadding(Ui.dp(context,20),Ui.dp(context,12),Ui.dp(context,20),Ui.dp(context,12));
        header.addView(Ui.icon(context,icon,Ui.color(context,com.google.android.material.R.attr.colorPrimary),24));
        LinearLayout copy=Ui.column(context); copy.setPadding(Ui.dp(context,12),0,Ui.dp(context,8),0);
        header.addView(copy,new LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        Ui.text(context,copy,context.getString(title),16,true);
        chevron=Ui.icon(context,R.drawable.ic_chevron,Ui.color(context,com.google.android.material.R.attr.colorOnSurfaceVariant),20);
        header.addView(chevron);addView(header);
        android.util.TypedValue ripple=new android.util.TypedValue();context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground,ripple,true);
        header.setBackgroundResource(ripple.resourceId);header.setOnClickListener(view -> setExpanded(!expanded));Motion.tactile(header);
        body=Ui.column(context);body.setPadding(Ui.dp(context,20),0,Ui.dp(context,20),Ui.dp(context,16));addView(body);
        settle();
    }
    boolean isExpanded() { return expanded; }
    void setExpanded(boolean open) {
        expanded=open;preferences.edit().putBoolean(key,open).apply();
        int token=++generation;
        if (animator!=null) {animator.cancel();animator=null;}
        chevron.animate().cancel();
        ViewCompat.setStateDescription(header,getContext().getString(open ? R.string.section_expanded : R.string.section_collapsed));
        if (!Motion.enabled(getContext()) || getWidth()==0) {settle();return;}
        boolean hidden=body.getVisibility()==GONE;
        int start=hidden ? 0 : body.getLayoutParams().height>=0 ? body.getLayoutParams().height : body.getHeight();
        if(hidden)body.setAlpha(0);
        body.setVisibility(VISIBLE);
        body.measure(MeasureSpec.makeMeasureSpec(getWidth()-getPaddingLeft()-getPaddingRight(),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));
        int end=open ? body.getMeasuredHeight() : 0;
        chevron.animate().rotation(open ? 90 : 0).setDuration(320).setInterpolator(Motion.EASE).start();
        animator=ValueAnimator.ofInt(start,end);animator.setDuration(320);animator.setInterpolator(Motion.EASE);
        float from=body.getAlpha();
        animator.addUpdateListener(value -> {
            ViewGroup.LayoutParams params=body.getLayoutParams();params.height=(int)value.getAnimatedValue();body.setLayoutParams(params);
            float fraction=value.getAnimatedFraction();body.setAlpha(from+((open ? 1 : 0)-from)*fraction);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {if(token==generation){animator=null;settle();}}
        });
        animator.start();
    }
    private void settle() {
        body.setVisibility(expanded ? VISIBLE : GONE);
        ViewGroup.LayoutParams params=body.getLayoutParams();params.height=ViewGroup.LayoutParams.WRAP_CONTENT;body.setLayoutParams(params);body.setAlpha(1);
        chevron.setRotation(expanded ? 90 : 0);
        ViewCompat.setStateDescription(header,getContext().getString(expanded ? R.string.section_expanded : R.string.section_collapsed));
    }
    @Override protected void onDetachedFromWindow() {
        ++generation;if(animator!=null){animator.cancel();animator=null;}chevron.animate().cancel();settle();super.onDetachedFromWindow();
    }
}
