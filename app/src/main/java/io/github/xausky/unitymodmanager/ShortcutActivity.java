package io.github.xausky.unitymodmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;

import io.github.xausky.unitymodmanager.adapter.VisibilityAdapter;
import io.github.xausky.unitymodmanager.fragment.HomeFragment;
import io.github.xausky.unitymodmanager.fragment.SettingFragment;

/**
 * Created by xausky on 4/7/18.
 */

public class ShortcutActivity extends Activity{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shortcut_launch_activity);
        VisibilityAdapter visibilityAdapter = new VisibilityAdapter(VirtualCore.get(), this);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        String launchPackage = getSharedPreferences(SettingFragment.SETTINGS_PREFERENCE_NAME, Context.MODE_PRIVATE).getString(HomeFragment.PACKAGE_PREFERENCE_KEY, null);
        Intent intent = VirtualCore.get().getLaunchIntent(launchPackage, 0);
        VActivityManager.get().startActivity(intent, 0);
        finish();
    }
}
