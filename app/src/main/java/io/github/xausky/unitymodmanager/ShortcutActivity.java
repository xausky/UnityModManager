package io.github.xausky.unitymodmanager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;

/**
 * Created by xausky on 4/7/18.
 */

public class ShortcutActivity extends Activity{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shortcut_launch_activity);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        String launchPackage = getIntent().getStringExtra("io.github.xausky.unitymodmanager.launchPackage");
        Intent intent = VirtualCore.get().getLaunchIntent(launchPackage, 0);
        VActivityManager.get().startActivity(intent, 0);
        finish();
    }
}
