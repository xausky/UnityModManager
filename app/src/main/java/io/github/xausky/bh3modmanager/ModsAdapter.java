package io.github.xausky.bh3modmanager;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
    private CompoundButton.OnCheckedChangeListener checkedChangeListener = null;
    private AdapterView.OnItemLongClickListener itemLongClickListener = null;
    private LayoutInflater inflater;

    public ModsAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    public List<Mod> getMods() {
        return mods;
    }

    public void setMods(List<Mod> mods) {
        this.mods = mods;
    }

    public void setCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
        this.checkedChangeListener = listener;
    }

    public void setItemLongClickListener(AdapterView.OnItemLongClickListener itemLongClickListener) {
        this.itemLongClickListener = itemLongClickListener;
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
    public View getView(final int i, final View view, ViewGroup viewGroup) {
        final View item = inflater.inflate(R.layout.mods_list_item, null);
        Switch aSwitch = item.findViewById(R.id.mods_list_item_switch);
        final Mod mod = (Mod) getItem(i);
        aSwitch.setChecked(mod.enable);
        aSwitch.setText(mod.name);
        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Log.d("BH3ModManager", compoundButton.getText() + ":" + b);
                mod.enable = b;
                if(checkedChangeListener != null){
                    checkedChangeListener.onCheckedChanged(compoundButton, b);
                }
            }
        });
        aSwitch.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return itemLongClickListener.onItemLongClick(null, item, i, getItemId(i));
            }
        });
        return item;
    }
}
