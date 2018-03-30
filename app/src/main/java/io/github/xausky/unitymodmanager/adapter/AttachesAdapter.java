package io.github.xausky.unitymodmanager.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
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
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.remote.InstalledAppInfo;

import java.util.ArrayList;
import java.util.List;

import io.github.xausky.unitymodmanager.R;
import io.github.xausky.unitymodmanager.dialog.ConfirmDialog;

/**
 * Created by xausky on 2018/2/1.
 */

public class AttachesAdapter extends RecyclerView.Adapter<AttachesAdapter.ViewHolder> implements DialogInterface.OnClickListener {

    private List<ApplicationInfo> applications = new ArrayList<>();
    private Context context;
    private VirtualCore va;
    private PackageManager manager;
    private ConfirmDialog dialog;
    private String uninstallPackageName;
    private String excludePackageName;

    public AttachesAdapter(VirtualCore va, String excludePackageName) {
        this.va = va;
        this.manager = va.getPackageManager();
        update(excludePackageName);
    }

    public void setRecyclerView(RecyclerView view){
        context = view.getContext();
        dialog = new ConfirmDialog(context, this);
        new ItemTouchHelper(new CallBack()).attachToRecyclerView(view);
        view.setLayoutManager(new LinearLayoutManager(context));
        view.setAdapter(this);
        view.addItemDecoration(new DividerItemDecoration(context,DividerItemDecoration.VERTICAL));
    }

    public void update(String excludePackageName){
        this.excludePackageName = excludePackageName;
        applications.clear();
        List<InstalledAppInfo> installedApplications = va.getInstalledApps(0);
        for(InstalledAppInfo info: installedApplications){
            if(!info.packageName.equals(excludePackageName)) {
                applications.add(info.getApplicationInfo(0));
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
    public void onBindViewHolder(AttachesAdapter.ViewHolder holder, int position) {
        final ApplicationInfo info = applications.get(position);
        holder.name.setText(manager.getApplicationLabel(info));
        holder.icon.setImageDrawable(manager.getApplicationIcon(info));
        holder.packageName.setText(info.packageName);
        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = VirtualCore.get().getLaunchIntent(info.packageName, 0);
                VActivityManager.get().startActivity(intent, 0);
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
            if (va.uninstallPackage(uninstallPackageName)){
                update(excludePackageName);
                Toast.makeText(context, R.string.attach_delete_success, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, R.string.attach_delete_fail, Toast.LENGTH_LONG).show();
            }
        }
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
            ViewHolder holder = (ViewHolder)target;
            String name = holder.name.getText().toString();
            uninstallPackageName = holder.packageName.getText().toString();
            String message = String.format(context.getString(R.string.attach_delete_confirm_message), name);
            dialog.setMessage(message);
            dialog.show();
        }
        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }
    }
}
