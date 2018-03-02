package io.github.xausky.bh3modmanager.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.xausky.bh3modmanager.MainService;
import io.github.xausky.bh3modmanager.domain.Mod;
import io.github.xausky.bh3modmanager.R;

/**
 * Created by xausky on 2018/2/1.
 */

public class ModsAdapter extends RecyclerView.Adapter<ModsAdapter.ViewHolder> {

    private List<Mod> mods = new ArrayList<>();
    private SharedPreferences preferences;
    private Context context;
    private File storage;
    private OnDataChangeListener listener;

    public ModsAdapter(RecyclerView view, SharedPreferences preferences, File storage, Context context) {
        new ItemTouchHelper(new CallBack()).attachToRecyclerView(view);
        this.preferences = preferences;
        this.context = context;
        this.storage = storage;
        File[] modFiles = storage.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(".zip");
            }
        });
        for(File file: modFiles){
            String name = file.getName();
            mods.add(new Mod(name, preferences.getBoolean(name + ":enable", false),
                    preferences.getString(name + ":password", null),
                    preferences.getInt(name + ":sort", Integer.MAX_VALUE)));
        }
        Collections.sort(mods);
    }

    public void cleanEnable(){
        SharedPreferences.Editor editor = preferences.edit();
        for(Mod mod: mods){
            mod.enable = false;
            editor.putBoolean(mod.name, false);
        }
        editor.apply();
    }

    public List<Mod> getMods(){
        return mods;
    }

    public void notifyApply(){
        SharedPreferences.Editor editor = preferences.edit();
        for(int i = 0; i < mods.size(); ++i){
            Mod mod = mods.get(i);
            mod.sort = i;
            editor.putBoolean(mod.name + ":enable", mod.enable);
            editor.putString(mod.name + ":password", mod.password);
            editor.putInt(mod.name + ":sort", mod.sort);
        }
        editor.apply();
    }

    public void setListener(OnDataChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.mods_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Mod mod = mods.get(position);
        holder.mod.setText(mod.name);
        holder.mod.setChecked(mod.enable);
        holder.mod.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(buttonView.isPressed()) {
                    mod.enable = isChecked;
                    Log.d(MainService.LOG_TAG, buttonView.getText() + "Mod State Change " + mod.name + ":" + mod.enable);
                    if (listener != null) {
                        listener.onDataChange();
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mods.size();
    }


    class ViewHolder extends RecyclerView.ViewHolder {
        private Switch mod = null;
        public ViewHolder(View itemView) {
            super(itemView);
            mod = itemView.findViewById(R.id.mods_list_item_switch);
        }
    }

    class CallBack extends ItemTouchHelper.Callback {

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.UP|ItemTouchHelper.DOWN;
            int swipeFlags = ItemTouchHelper.LEFT|ItemTouchHelper.RIGHT;
            return makeMovementFlags(dragFlags,swipeFlags);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder origin, RecyclerView.ViewHolder target) {
            Mod originMod = mods.get(origin.getAdapterPosition());
            Mod targetMod = mods.get(target.getAdapterPosition());
            int sort = originMod.sort;
            originMod.sort = targetMod.sort;
            targetMod.sort = sort;
            Collections.swap(mods,origin.getAdapterPosition(),target.getAdapterPosition());
            ModsAdapter.this.notifyItemMoved(origin.getAdapterPosition(),target.getAdapterPosition());
            if(listener!=null){
                listener.onDataChange();
            }
            return false;
        }

        @Override
        public void onSwiped(final RecyclerView.ViewHolder target, int direction) {
            final Mod mod = mods.get(target.getAdapterPosition());
            if(mod.enable){
                Toast.makeText(context,"无法删除已启用的模组，请先禁用：" + mod.name , Toast.LENGTH_SHORT).show();
                ModsAdapter.this.notifyDataSetChanged();
                return;
            }
            final AlertDialog.Builder builder=new AlertDialog.Builder(context);
            builder.setTitle("确认");
            builder.setMessage("删除模组:" + mod.name);
            builder.setCancelable(true);
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(!new File(storage.getAbsolutePath() + "/" + mod.name).delete()){
                        Toast.makeText(context,"警告，模组文件删除失败：" + mod.name , Toast.LENGTH_SHORT).show();
                    }
                    mods.remove(target.getAdapterPosition());
                    ModsAdapter.this.notifyApply();
                    ModsAdapter.this.notifyDataSetChanged();
                }
            });
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ModsAdapter.this.notifyDataSetChanged();
                }
            });
            builder.show();
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }
    }

    public interface OnDataChangeListener {
        void onDataChange();
    }

}
