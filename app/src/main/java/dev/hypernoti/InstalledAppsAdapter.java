package dev.hypernoti;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DiffUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

final class InstalledAppsAdapter extends RecyclerView.Adapter<InstalledAppsAdapter.Holder> {
    interface Open { void packageName(String name); }
    private final Context context;
    private final Open advanced, settings;
    private final List<ApplicationInfo> applications=new ArrayList<>();
    InstalledAppsAdapter(Context context,Open advanced,Open settings){this.context=context;this.advanced=advanced;this.settings=settings;}
    void submit(List<ApplicationInfo> list) {
        List<ApplicationInfo> previous = new ArrayList<>(applications);
        DiffUtil.DiffResult difference = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return previous.size(); }
            @Override public int getNewListSize() { return list.size(); }
            @Override public boolean areItemsTheSame(int oldPosition, int newPosition) {
                return previous.get(oldPosition).packageName.equals(list.get(newPosition).packageName);
            }
            @Override public boolean areContentsTheSame(int oldPosition, int newPosition) {
                // Filtering retains objects; a fresh PackageManager query intentionally rebinds metadata/icons.
                return previous.get(oldPosition) == list.get(newPosition);
            }
        });
        applications.clear(); applications.addAll(list); difference.dispatchUpdatesTo(this);
    }
    @Override public int getItemCount(){return applications.size();}
    @Override public Holder onCreateViewHolder(ViewGroup parent,int type){
        MaterialCardView card=new MaterialCardView(context);card.setRadius(Ui.dp(context,16));card.setCardElevation(0);card.setStrokeWidth(0);
        card.setCardBackgroundColor(Ui.color(context,com.google.android.material.R.attr.colorSurfaceContainer));
        RecyclerView.LayoutParams params=new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);params.bottomMargin=Ui.dp(context,8);card.setLayoutParams(params);
        LinearLayout row=Ui.row(context);row.setPadding(Ui.dp(context,16),Ui.dp(context,12),Ui.dp(context,8),Ui.dp(context,12));card.addView(row);
        ImageView icon=new ImageView(context);row.addView(icon,new LinearLayout.LayoutParams(Ui.dp(context,40),Ui.dp(context,40)));
        LinearLayout labels=Ui.column(context);labels.setPadding(Ui.dp(context,14),0,Ui.dp(context,4),0);row.addView(labels,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        TextView title=Ui.text(context,labels,"",16,true);title.setMaxLines(2);title.setEllipsize(TextUtils.TruncateAt.END);
        TextView pkg=Ui.text(context,labels,"",12,false);pkg.setMaxLines(1);pkg.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        MaterialButton action=new MaterialButton(context);
        action.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));action.setElevation(0);
        action.setIconTint(android.content.res.ColorStateList.valueOf(Ui.color(context,com.google.android.material.R.attr.colorOnSurfaceVariant)));
        action.setIconResource(R.drawable.ic_shield);action.setIconPadding(0);action.setIconSize(Ui.dp(context,22));action.setMinWidth(0);action.setMinimumWidth(0);action.setPadding(Ui.dp(context,12),0,Ui.dp(context,12),0);
        action.setContentDescription(context.getString(R.string.advanced));row.addView(action,new LinearLayout.LayoutParams(Ui.dp(context,48),Ui.dp(context,48)));
        return new Holder(card,icon,title,pkg,action);
    }
    @Override public void onBindViewHolder(Holder holder,int position){
        ApplicationInfo app=applications.get(position);holder.title.setText(app.loadLabel(context.getPackageManager()));holder.pkg.setText(app.packageName);
        holder.icon.setImageDrawable(app.loadIcon(context.getPackageManager()));
        holder.itemView.setOnClickListener(view -> settings.packageName(app.packageName));
        holder.action.setOnClickListener(view -> advanced.packageName(app.packageName));
        holder.action.setContentDescription(context.getString(R.string.app_advanced_description,holder.title.getText()));
    }
    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;final TextView title,pkg;final MaterialButton action;
        Holder(MaterialCardView view,ImageView icon,TextView title,TextView pkg,MaterialButton action){super(view);this.icon=icon;this.title=title;this.pkg=pkg;this.action=action;}
    }
}
