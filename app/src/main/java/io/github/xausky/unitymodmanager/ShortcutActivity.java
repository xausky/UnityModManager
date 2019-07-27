package io.github.xausky.unitymodmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.widget.Toast;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;

import io.github.xausky.unitymodmanager.adapter.VisibilityAdapter;
import io.github.xausky.unitymodmanager.fragment.HomeFragment;
import io.github.xausky.unitymodmanager.fragment.SettingFragment;

import static io.github.xausky.unitymodmanager.fragment.HomeFragment.APK_MODIFY_MODEL_VIRTUAL;

/**
 * Created by xausky on 4/7/18.
 */

public class ShortcutActivity extends Activity{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shortcut_launch_activity);
        VisibilityAdapter visibilityAdapter = new VisibilityAdapter(this);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        SharedPreferences preferences = getSharedPreferences(SettingFragment.SETTINGS_PREFERENCE_NAME, Context.MODE_PRIVATE);
        int apkModifyModel = Integer.valueOf(preferences.getString("apk_modify_model", "0"));
        if(apkModifyModel != APK_MODIFY_MODEL_VIRTUAL){
            Toast.makeText(this, R.string.no_virtual_model_shortcut, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        String launchPackage = preferences.getString(HomeFragment.PACKAGE_PREFERENCE_KEY, null);
        if(launchPackage == null){
            Toast.makeText(this, R.string.install_source_not_found, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Intent intent = VirtualCore.get().getLaunchIntent(launchPackage, 0);
        if(intent == null){
            Toast.makeText(this, R.string.install_source_not_found, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        VActivityManager.get().startActivity(intent, 0);
        finish();
    }
}
