package io.github.xausky.bh3modmanager;

import android.app.Application;
import android.content.Context;
import com.lody.virtual.client.core.VirtualCore;

/**
 * Created by xausky on 2018/2/1.
 */

public class MainApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            VirtualCore.get().startup(base);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
