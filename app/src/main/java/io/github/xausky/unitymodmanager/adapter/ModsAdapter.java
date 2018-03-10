package io.github.xausky.unitymodmanager.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.lody.virtual.client.core.VirtualCore;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.xausky.unitymodmanager.R;
import io.github.xausky.unitymodmanager.dialog.ConfirmDialog;
import io.github.xausky.unitymodmanager.domain.Mod;
import io.github.xausky.unitymodmanager.utils.FileUtils;
import io.github.xausky.unitymodmanager.utils.ZipUtils;

/**
 * Created by xausky on 18-3-9.
 */

public class ModsAdapter extends RecyclerView.Adapter<ModsAdapter.ViewHolder> implements DialogInterface.OnClickListener{
    public static final String MODS_SHARED_PREFERENCES_KEY = "mods";

    private List<Mod> mods = new ArrayList<>();
    private SharedPreferences preferences;
    private Context context;
    private File storage;
    private OnDataChangeListener listener;
    private ConfirmDialog dialog;
    private int operationPosition;
    private int enableItemCount;

    public ModsAdapter(File storage, Context context) {
        this.storage = storage;
        this.preferences = context.getSharedPreferences(MODS_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        this.enableItemCount = 0;
        File[] modFiles = storage.listFiles();
        for(File file: modFiles){
            if(file.isDirectory()){
                String name = file.getName();
                Mod mod = new Mod(name, preferences.getBoolean(name + ":enable", false),
                        preferences.getString(name + ":password", null),
                        preferences.getInt(name + ":order", Integer.MAX_VALUE));
                if(mod.enable){
                    ++enableItemCount;
                }
                mods.add(mod);
            }
        }
        Collections.sort(mods);
    }

    public int getEnableItemCount() {
        return enableItemCount;
    }

    public void setRecyclerView(RecyclerView view){
        this.context = view.getContext();
        this.dialog = new ConfirmDialog(context, this);
        new ItemTouchHelper(new ModsAdapter.CallBack()).attachToRecyclerView(view);
        view.setLayoutManager(new LinearLayoutManager(context));
        view.setAdapter(this);
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
            mod.order = i;
            editor.putBoolean(mod.name + ":enable", mod.enable);
            editor.putString(mod.name + ":password", mod.password);
            editor.putInt(mod.name + ":order", mod.order);
        }
        editor.apply();
    }

    public void addMods(String path, List<String> names){
        String name = names.get(0);
        File targetFile = new File(storage.getAbsolutePath() + "/" + name);
        if(targetFile.exists()){
           Toast.makeText(context, R.string.import_mod_exists, Toast.LENGTH_LONG).show();
        }
        if(!targetFile.mkdir()){
            Toast.makeText(context, R.string.store_mkdir_failed, Toast.LENGTH_LONG).show();
        }
        if(ZipUtils.unzipFile(path + name, targetFile.getAbsolutePath(), null) == ZipUtils.RESULT_STATE_OK){
            mods.add(new Mod(name, false, null, Integer.MAX_VALUE));
            notifyDataSetChanged();
        }
    }

    public void setListener(OnDataChangeListener listener) {
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.mod_item_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Mod mod = mods.get(position);
        holder.name.setText(mod.name);
        holder.aSwitch.setChecked(mod.enable);
        holder.aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(buttonView.isPressed()){
                    enableItemCount += isChecked?1:-1;
                    mod.enable = isChecked;
                    if(listener!=null){
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

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            Mod mod  = mods.get(operationPosition);
            if (FileUtils.deleteFile(new File(storage.getAbsolutePath() + "/" + mod.name))) {
                mods.remove(operationPosition);
            } else {
                Toast.makeText(context, context.getString(R.string.mod_file_delete_failed) + mod.name, Toast.LENGTH_SHORT).show();
            }
        }
        ModsAdapter.this.notifyDataSetChanged();
    }


    class ViewHolder extends RecyclerView.ViewHolder {
        private Switch aSwitch;
        private ImageView icon;
        private TextView name;
        public ViewHolder(View itemView) {
            super(itemView);
            aSwitch = itemView.findViewById(R.id.mod_list_item_switch);
            icon = itemView.findViewById(R.id.mod_list_item_icon);
            name = itemView.findViewById(R.id.mod_list_item_name);
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
            int order = originMod.order;
            originMod.order = targetMod.order;
            targetMod.order = order;
            Collections.swap(mods,origin.getAdapterPosition(),target.getAdapterPosition());
            ModsAdapter.this.notifyItemMoved(origin.getAdapterPosition(),target.getAdapterPosition());
            if(listener!=null){
                listener.onDataChange();
            }
            return false;
        }

        @Override
        public void onSwiped(final RecyclerView.ViewHolder target, int direction) {
            operationPosition = target.getAdapterPosition();
            Mod mod = mods.get(operationPosition);
            dialog.setMessage(String.format(context.getString(R.string.mod_delete_confirm), mod.name));
            dialog.show();
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
