package io.github.xausky.bh3modmanager.adapter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.remote.InstalledAppInfo;

import java.util.ArrayList;
import java.util.List;

import io.github.xausky.bh3modmanager.R;

/**
 * Created by xausky on 18-3-6.
 */

public class AttachesAdapter extends BaseAdapter {
    private List<ApplicationInfo> applicationInfos = new ArrayList<>();
    private LayoutInflater inflater;
    private PackageManager manager;
    private VirtualCore va;

    public AttachesAdapter(Context context, VirtualCore va, String excludePackageName) {
        this.inflater = LayoutInflater.from(context);
        manager = va.getPackageManager();
        this.va = va;
        update(excludePackageName);
    }

    public void update(String excludePackageName){
        applicationInfos.clear();
        List<InstalledAppInfo> installedApplications = va.getInstalledApps(0);
        for(InstalledAppInfo info: installedApplications){
            if(!info.packageName.equals(excludePackageName)) {
                applicationInfos.add(info.getApplicationInfo(0));
            }
        }
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return applicationInfos.size();
    }

    @Override
    public ApplicationInfo getItem(int i) {
        return applicationInfos.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if(view == null){
            view = inflater.inflate(R.layout.choose_client_dialog_clients_item, viewGroup, false);
            AttachesAdapter.ViewHolder holder = new AttachesAdapter.ViewHolder();
            holder.name = view.findViewById(R.id.choose_client_dialog_clients_item_name);
            holder.packageName = view.findViewById(R.id.choose_client_dialog_clients_item_package_name);
            holder.icon = view.findViewById(R.id.choose_client_dialog_clients_item_icon);
            view.setTag(holder);
        }
        AttachesAdapter.ViewHolder holder = (AttachesAdapter.ViewHolder)view.getTag();
        ApplicationInfo info = getItem(i);
        holder.packageName.setText(info.packageName);
        holder.name.setText(manager.getApplicationLabel(info));
        holder.icon.setImageDrawable(manager.getApplicationIcon(info));
        return view;
    }

    private static class ViewHolder {
        TextView name;
        TextView packageName;
        ImageView icon;
    }
}