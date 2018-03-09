package io.github.xausky.unitymodmanager.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lody.virtual.client.core.VirtualCore;

import java.util.ArrayList;
import java.util.List;

import io.github.xausky.unitymodmanager.R;
import io.github.xausky.unitymodmanager.dialog.ConfirmDialog;

/**
 * Created by xausky on 18-3-9.
 */

public class VisibilityAdapter  extends RecyclerView.Adapter<VisibilityAdapter.ViewHolder> implements DialogInterface.OnClickListener {
    public static final String VISIBILITY_SHARED_PREFERENCES_KEY = "visibility";

    private List<ApplicationInfo> applications = new ArrayList<>();
    private Context context;
    private VirtualCore va;
    private PackageManager manager;
    private ConfirmDialog dialog;
    private String removePackageName;
    private SharedPreferences preferences;

    public VisibilityAdapter(VirtualCore va, Context context) {
        this.va = va;
        this.context = context;
        this.manager = context.getPackageManager();
        this.preferences = context.getSharedPreferences(VISIBILITY_SHARED_PREFERENCES_KEY,Context.MODE_PRIVATE);
        update();
    }

    public void setRecyclerView(RecyclerView view){
        context = view.getContext();
        dialog = new ConfirmDialog(context, this);
        new ItemTouchHelper(new CallBack()).attachToRecyclerView(view);
        view.setLayoutManager(new LinearLayoutManager(context));
        view.setAdapter(this);
    }

    public void update(){
        applications.clear();
        List<ApplicationInfo> installedApplications = manager.getInstalledApplications(0);
        for(ApplicationInfo info: installedApplications){
            if(preferences.getBoolean(info.packageName, false)){
                va.addVisibleOutsidePackage(info.packageName);
            }
            if(va.isOutsidePackageVisible(info.packageName)) {
                applications.add(info);
            }
        }
        this.notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.application_info_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(VisibilityAdapter.ViewHolder holder, int position) {
        final ApplicationInfo info = applications.get(position);
        holder.name.setText(manager.getApplicationLabel(info));
        holder.icon.setImageDrawable(manager.getApplicationIcon(info));
        holder.packageName.setText(info.packageName);
        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = manager.getLaunchIntentForPackage(info.packageName);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return applications.size();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if(which == AlertDialog.BUTTON_POSITIVE){
            va.removeVisibleOutsidePackage(removePackageName);
            preferences.edit().remove(removePackageName).apply();
            Toast.makeText(context, R.string.visibility_delete_success, Toast.LENGTH_LONG).show();
        }
        update();
    }

    public void addVisibleOutsidePackage(String packageName){
        va.addVisibleOutsidePackage(packageName);
        update();
        preferences.edit().putBoolean(packageName, true).apply();
    }


    class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView packageName;
        ImageView icon;
        View view;
        ViewHolder(View itemView) {
            super(itemView);
            view = itemView;
            name = itemView.findViewById(R.id.choose_client_dialog_clients_item_name);
            packageName = itemView.findViewById(R.id.choose_client_dialog_clients_item_package_name);
            icon = itemView.findViewById(R.id.choose_client_dialog_clients_item_icon);

        }
    }

    class CallBack extends ItemTouchHelper.Callback {

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.UP|ItemTouchHelper.DOWN;
            int swipeFlags = ItemTouchHelper.LEFT|ItemTouchHelper.RIGHT;
            return makeMovementFlags(dragFlags,swipeFlags);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(final RecyclerView.ViewHolder target, int direction) {
            VisibilityAdapter.ViewHolder holder = (VisibilityAdapter.ViewHolder)target;
            String name = holder.name.getText().toString();
            removePackageName = holder.packageName.getText().toString();
            String message = String.format(context.getString(R.string.visibility_delete_confirm_message), name);
            dialog.setMessage(message);
            dialog.show();
        }

    }
}
