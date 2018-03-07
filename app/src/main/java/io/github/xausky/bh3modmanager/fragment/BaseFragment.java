package io.github.xausky.bh3modmanager.fragment;

import android.app.Application;
import android.app.Fragment;
import android.content.Context;
import android.view.View;

import java.util.Map;
import java.util.TreeMap;

import io.github.xausky.bh3modmanager.R;

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

    public BaseFragment(){

    }

    public Context getBase() {
        return base;
    }

    public BaseFragment setBase(Context base) {
        this.base = base;
        return this;
    }

    public static Map<Integer, Fragment> map;

    public static void initialize(Context base){
        if(map == null){
            map = new TreeMap<>();
            map.put(R.id.nav_home, new HomeFragment().setBase(base));
            map.put(R.id.nav_info, new InfoFragment().setBase(base));
            map.put(R.id.nav_attach, new AttachFragment().setBase(base));
            map.put(R.id.nav_mod, new ModFragment().setBase(base));
            map.put(R.id.nav_visibility, new VisibilityFragment().setBase(base));
            map.put(R.id.nav_setting, new SettingFragment());
        }
    }

    public static Fragment fragment(int item){
        return map.get(item);
    }
}
