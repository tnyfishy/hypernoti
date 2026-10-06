package dev.hypernoti;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.PathInterpolator;

final class Motion {
    static final PathInterpolator EASE = new PathInterpolator(.2f,0,0,1);
    static boolean enabled(Context context) {
        return ValueAnimator.areAnimatorsEnabled() && !context.getSharedPreferences("hypernoti",Context.MODE_PRIVATE).getBoolean("reduce_motion",false);
    }
    static void enter(View view, int direction) {
        view.animate().cancel(); view.setAlpha(1); view.setTranslationY(0); view.setTranslationX(0);
        if (!enabled(view.getContext())) return;
        view.setAlpha(0); view.setTranslationX(Ui.dp(view.getContext(),18)*direction);
        view.animate().alpha(1).translationX(0).setDuration(280).setInterpolator(EASE).start();
    }
    // The listener observes touch only; returning false lets View dispatch performClick normally.
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    static void tactile(View view) {
        view.setOnTouchListener((target,event) -> {
            if (!enabled(target.getContext())) return false;
            int action=event.getActionMasked();
            if (action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL) {
                float scale=action==MotionEvent.ACTION_DOWN ? .98f : 1;
                target.animate().scaleX(scale).scaleY(scale).setDuration(action==MotionEvent.ACTION_DOWN ? 100 : 220).setInterpolator(EASE).start();
            }
            return false; // Keep normal click, scroll cancellation and accessibility behavior.
        });
    }
    private Motion() {}
}
