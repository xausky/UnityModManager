package io.github.xausky.unitymodmanager;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.multidex.MultiDexApplication;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.stub.VASettings;

import io.fabric.sdk.android.Fabric;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import io.github.xausky.unitymodmanager.adapter.VisibilityAdapter;
import io.github.xausky.unitymodmanager.utils.ModUtils;

/**
 * Created by xausky on 2018/2/1.
 */

public class MainApplication extends MultiDexApplication {
    public static final String LOG_TAG = "UnityModManager";
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            VirtualCore.get().startup(base);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException("virtual app core startup failed:" + throwable.getMessage());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        Fresco.initialize(this);
        SharedPreferences preferences = this.getSharedPreferences("default", MODE_PRIVATE);
        SharedPreferences visibilityPreferences = this.getSharedPreferences(VisibilityAdapter.VISIBILITY_SHARED_PREFERENCES_KEY, MODE_PRIVATE);
        if(preferences.getBoolean("first", true)){
            //微信和支付宝默认可见（用于氪金）
            visibilityPreferences.edit()
                    .putBoolean("com.eg.android.AlipayGphone", true)
                    .putBoolean("com.tencent.mm", true).apply();
            preferences.edit().putBoolean("first", false).apply();
        }
        InputStream mapInputStream = null;
        try {
            mapInputStream = this.getAssets().open("map.json");
            byte[] bytes = new byte[mapInputStream.available()];
            if(mapInputStream.read(bytes) == -1){
                throw new IOException("map.json read failed.");
            }
            String json = new String(bytes);
            ModUtils.map = new JSONObject(json);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
