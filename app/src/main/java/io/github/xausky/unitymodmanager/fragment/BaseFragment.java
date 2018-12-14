package io.github.xausky.unitymodmanager.fragment;

import android.app.Fragment;
import android.content.Context;
import android.view.View;

import java.util.Map;
import java.util.TreeMap;

import io.github.xausky.unitymodmanager.R;

/**
 * Created by xausky on 18-3-3.
 */

public abstract class BaseFragment extends Fragment {
    public void OnActionButtonClick(){
        throw new UnsupportedOperationException();
    }
    public void OnActionButtonLongClick(){
        throw new UnsupportedOperationException();
    }
    public int actionButtonVisibility(){
        return View.INVISIBLE;
    }
    private Context base;

    public Context getBase() {
        return base;
    }

    public BaseFragment setBase(Context base) {
        this.base = base;
        return this;
    }

    public static Map<Integer, Fragment> fragments;

    public static synchronized Map<Integer, Fragment> initialize(Context base){
        if (fragments == null) {
            fragments = new TreeMap<>();
            fragments.put(R.id.nav_mod, new ModFragment().setBase(base));
            fragments.put(R.id.nav_home, new HomeFragment().setBase(base));
            fragments.put(R.id.nav_info, new InfoFragment().setBase(base));
            fragments.put(R.id.nav_attach, new AttachFragment().setBase(base));
            fragments.put(R.id.nav_visibility, new VisibilityFragment().setBase(base));
            fragments.put(R.id.nav_setting, new SettingFragment());
        }
        return fragments;
    }

    public static Fragment fragment(int item, Context base){
        return initialize(base).get(item);
    }
}
