package io.github.xausky.bh3modmanager.adapter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.github.xausky.bh3modmanager.R;

/**
 * Created by xausky on 18-2-9.
 */


public class ClientsAdapter extends BaseAdapter {
    private List<ApplicationInfo> applicationInfos = new ArrayList<>();
    private LayoutInflater inflater;
    private PackageManager manager;

    public ClientsAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
        manager = context.getPackageManager();
        List<ApplicationInfo> installedApplications = manager.getInstalledApplications(0);
        for(ApplicationInfo info: installedApplications){
            if(info.packageName.startsWith("com.miHoYo.")){
                applicationInfos.add(info);
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
        View item = inflater.inflate(R.layout.choose_client_dialog_clients_item, null);
        TextView nameView = item.findViewById(R.id.choose_client_dialog_clients_item_name);
        TextView packageNameView = item.findViewById(R.id.choose_client_dialog_clients_item_package_name);
        ImageView iconView = item.findViewById(R.id.choose_client_dialog_clients_item_icon);
        ApplicationInfo info = getItem(i);
        packageNameView.setText(info.packageName);
        nameView.setText(manager.getApplicationLabel(info));
        iconView.setImageDrawable(manager.getApplicationIcon(info));
        return item;
    }
}
