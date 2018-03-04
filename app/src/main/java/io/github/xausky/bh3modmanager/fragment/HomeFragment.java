package io.github.xausky.bh3modmanager.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lody.virtual.client.core.VirtualCore;

import io.github.xausky.bh3modmanager.R;

/**
 * Created by xausky on 18-3-3.
 */

public class HomeFragment extends BaseFragment{
    private TextView summary;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_fragment, container, false);
        SharedPreferences settings = inflater.getContext().getSharedPreferences(SettingFragment.SETTINGS_PREFERENCE_NAME, Context.MODE_PRIVATE);
        summary = view.findViewById(R.id.home_summary);
        StringBuilder builder = new StringBuilder();
        try {
            String versionName = inflater.getContext().getPackageManager().getPackageInfo(inflater.getContext().getPackageName(), 0).versionName;
            builder.append("Software Version Name: ");
            builder.append(versionName);
            builder.append('\n');
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        builder.append("Imported Mods Count: ");
        builder.append(0);
        builder.append('\n');
        builder.append("Enabled Mods Count: ");
        builder.append(0);
        builder.append('\n');
        builder.append("Virtual Core Installed App Count: ");
        builder.append(VirtualCore.get().getInstalledAppCount());
        builder.append('\n');
        summary.setText(builder.toString());
        return view;
    }
}
