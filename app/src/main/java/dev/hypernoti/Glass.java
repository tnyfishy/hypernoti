package dev.hypernoti;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import com.google.android.material.card.MaterialCardView;

/** Static optical layers: no screen captures, render loops, or blur of readable content. */
final class Glass {
    static boolean dark(Context context) {
        return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }
    static int alpha(int color, int alpha) { return (color & 0x00ffffff) | (alpha << 24); }
    static MaterialCardView card(Context context, int tint) { return new Card(context, tint); }

    private static final class Card extends MaterialCardView {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF bounds = new RectF();
        private final boolean night;
        Card(Context context, int tint) {
            super(context); night = dark(context);
            setRadius(Ui.dp(context, 26)); setCardElevation(Ui.dp(context, 1));
            setStrokeWidth(Ui.dp(context, 1));
            setStrokeColor(night ? 0x38ffffff : 0xcfffffff);
            setCardBackgroundColor(tint); setWillNotDraw(false);
        }
        @Override public void setCardBackgroundColor(int color) {
            // Strong tint preserves the semantic status colors and text contrast.
            super.setCardBackgroundColor(alpha(color, dark(getContext()) ? 218 : 200));
        }
        @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w,h,oldw,oldh);
            bounds.set(1,1,w-1,h-1);
            paint.setShader(new LinearGradient(0,0,w,h,
                    new int[]{night ? 0x18ffffff : 0x60ffffff, 0x00ffffff, night ? 0x04ffffff : 0x14ffffff},
                    new float[]{0,.48f,1},Shader.TileMode.CLAMP));
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawRoundRect(bounds,getRadius(),getRadius(),paint);
        }
    }

    static Drawable backdrop(Context context) {
        return new Drawable() {
            final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            final boolean night = dark(context);
            final boolean amoled = context.getSharedPreferences("hypernoti",Context.MODE_PRIVATE).getString("appearance","system").equals("amoled");
            final int base = Ui.color(context,com.google.android.material.R.attr.colorSurface);
            final int accent = Ui.color(context,com.google.android.material.R.attr.colorPrimary);
            final Shader[] fields = new Shader[3];
            @Override protected void onBoundsChange(android.graphics.Rect bounds) {
                float w=bounds.width(), h=bounds.height();
                fields[0]=glow(w*.95f,h*.05f,w*.95f,alpha(accent,night ? 45 : 48));
                fields[1]=glow(-w*.12f,h*.48f,w*.85f,night ? 0x243c9f96 : 0x4069cdbc);
                fields[2]=glow(w*.92f,h*.95f,w*.85f,night ? 0x2e936fc5 : 0x409982de);
            }
            @Override public void draw(Canvas canvas) {
                canvas.drawColor(base);
                if (amoled) return;
                for(Shader field:fields) {if(field!=null){paint.setShader(field);canvas.drawRect(getBounds(),paint);}}
            }
            Shader glow(float x,float y,float radius,int color) {
                return new RadialGradient(x,y,Math.max(1,radius),color,alpha(color,0),Shader.TileMode.CLAMP);
            }
            @Override public void setAlpha(int alpha) {}
            @Override public void setColorFilter(ColorFilter filter) {}
            @Override public int getOpacity() { return PixelFormat.OPAQUE; }
        };
    }
}
