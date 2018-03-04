package io.github.xausky.bh3modmanager.fragment;

import android.app.Fragment;
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
    public static Map<Integer, Fragment> map;

    public static Fragment fragment(int item){
        if(map == null){
            map = new TreeMap<>();
            map.put(R.id.nav_home, new HomeFragment());
            map.put(R.id.nav_info, new InfoFragment());
            map.put(R.id.nav_attach, new AttachFragment());
            map.put(R.id.nav_mod, new ModFragment());
            map.put(R.id.nav_setting, new SettingFragment());
        }
        return map.get(item);
    }
}
