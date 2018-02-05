package io.github.xausky.bh3modmanager;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.Switch;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xausky on 2018/2/1.
 */

public class ModsAdapter extends BaseAdapter {
    private Context context =  null;
    private List<Mod> mods = new ArrayList<>();
    private CompoundButton.OnCheckedChangeListener listener = null;

    public ModsAdapter(Context context) {
        this.context = context;
    }

    public List<Mod> getMods() {
        return mods;
    }

    public void setMods(List<Mod> mods) {
        this.mods = mods;
    }

    public void setListener(CompoundButton.OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return mods.size();
    }

    @Override
    public Object getItem(int i) {
        return mods.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        Switch new_view = new Switch(context);
        final Mod mod = (Mod) getItem(i);
        new_view.setChecked(mod.enable);
        new_view.setText(mod.name);
        new_view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        new_view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        new_view.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Log.d("BH3ModManager", compoundButton.getText() + ":" + b);
                mod.enable = b;
                if(listener != null){
                    listener.onCheckedChanged(compoundButton, b);
                }
            }
        });
        return new_view;
    }
}
