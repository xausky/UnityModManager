package io.github.xausky.unitymodmanager.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.xausky.unitymodmanager.R;

/**
 * Created by xausky on 18-2-9.
 */


public class ApplicationsAdapter extends BaseAdapter {
    private List<ApplicationInfo> applicationInfos = new ArrayList<>();
    private LayoutInflater inflater;
    private PackageManager manager;

    public ApplicationsAdapter(Context context, String packageRegex, boolean launchFilter) {
        this.inflater = LayoutInflater.from(context);
        manager = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        if(launchFilter) {
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
        }
        List<ResolveInfo> infos = manager.queryIntentActivities(intent, 0);
        for(ResolveInfo info: infos){
            if(info.activityInfo.packageName.matches(packageRegex)){
                applicationInfos.add(info.activityInfo.applicationInfo);
            }
        }
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
            view = inflater.inflate(R.layout.application_info_item, viewGroup, false);
            ViewHolder holder = new ViewHolder();
            holder.name = view.findViewById(R.id.choose_client_dialog_clients_item_name);
            holder.packageName = view.findViewById(R.id.choose_client_dialog_clients_item_package_name);
            holder.icon = view.findViewById(R.id.choose_client_dialog_clients_item_icon);
            view.setTag(holder);
        }
        ViewHolder holder = (ViewHolder)view.getTag();
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
