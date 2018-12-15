package io.github.xausky.unitymodmanager.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.JsonWriter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.hzy.libp7zip.P7ZipApi;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.xausky.unitymodmanager.MainApplication;
import io.github.xausky.unitymodmanager.R;
import io.github.xausky.unitymodmanager.dialog.ConfirmDialog;
import io.github.xausky.unitymodmanager.dialog.ModInfoDialog;
import io.github.xausky.unitymodmanager.dialog.PasswordDialog;
import io.github.xausky.unitymodmanager.domain.Mod;
import io.github.xausky.unitymodmanager.utils.ModUtils;
import io.github.xausky.unitymodmanager.utils.NativeUtils;

import static io.github.xausky.unitymodmanager.utils.NativeUtils.PatchApk;
import static io.github.xausky.unitymodmanager.utils.NativeUtils.RESULT_STATE_INTERNAL_ERROR;

/**
 * Created by xausky on 18-3-9.
 */

public class ModsAdapter extends RecyclerView.Adapter<ModsAdapter.ViewHolder> implements DialogInterface.OnClickListener, FileAlterationListener {
    public static final String MODS_SHARED_PREFERENCES_KEY = "mods";

    private List<Mod> mods = new ArrayList<>();
    private SharedPreferences preferences;
    private Context context;
    private File storage;
    private File externalCache;
    private OnDataChangeListener listener;
    private ConfirmDialog dialog;
    private int operationPosition;
    private int enableItemCount;
    private PasswordDialog passwordDialog;
    private FileAlterationMonitor monitor = new FileAlterationMonitor();
    private Map<String, FileAlterationObserver> observers = new HashMap<>();
    private long lastExternalChangeTime = -1;
    private boolean showConflict = false;

    public ModsAdapter(File storage, File externalCache, Context context) {
        this.storage = storage;
        this.externalCache = externalCache;
        this.preferences = context.getSharedPreferences(MODS_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        this.enableItemCount = 0;
        File[] modFiles = storage.listFiles();
        if(modFiles == null){
            Toast.makeText(context, R.string.no_storage_permissions, Toast.LENGTH_LONG).show();
            ((Activity)context).finish();
            return;
        }
        for (File file : modFiles) {
            String name = file.getName();
            String path = file.getAbsolutePath();
            Mod mod = new Mod(name,
                    preferences.getBoolean(name + ":enable", false),
                    preferences.getInt(name + ":order", Integer.MAX_VALUE),
                    preferences.getInt(name + ":fileCount", -1),
                    path,
                    preferences.getStringSet(name + ":conflict", null));
            if (mod.enable) {
                ++enableItemCount;
                if(file.isFile()){
                    try {
                        String externalPath = FileUtils.readFileToString(file);
                        FileAlterationObserver observer = new FileAlterationObserver(externalPath);
                        observer.addListener(ModsAdapter.this);
                        observers.put(mod.name,observer);
                        monitor.addObserver(observer);
                        Toast.makeText(context, context.getString(R.string.external_mods_observer_start, mod.name), Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            mods.add(mod);
        }
        Collections.sort(mods);
        try {
            monitor.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setShowConflict(boolean showConflict) {
        this.showConflict = showConflict;
    }

    public int getEnableItemCount() {
        return enableItemCount;
    }

    public void setRecyclerView(RecyclerView view) {
        this.context = view.getContext();
        this.dialog = new ConfirmDialog(context, this);
        this.passwordDialog = new PasswordDialog(context);
        new ItemTouchHelper(new ModsAdapter.CallBack()).attachToRecyclerView(view);
        view.setLayoutManager(new LinearLayoutManager(context));
        view.setAdapter(this);
        view.addItemDecoration(new DividerItemDecoration(context,DividerItemDecoration.VERTICAL));
    }

    public void cleanEnable() {
        SharedPreferences.Editor editor = preferences.edit();
        for (Mod mod : mods) {
            mod.enable = false;
            editor.putBoolean(mod.name, false);
        }
        editor.apply();
    }

    public List<Mod> getMods() {
        return mods;
    }

    public void notifyApply() {
        SharedPreferences.Editor editor = preferences.edit();
        for (int i = 0; i < mods.size(); ++i) {
            Mod mod = mods.get(i);
            mod.order = i;
            editor.putBoolean(mod.name + ":enable", mod.enable);
            editor.putInt(mod.name + ":order", mod.order);
            editor.putInt(mod.name + ":fileCount", mod.fileCount);
            editor.putStringSet(mod.name + ":conflict", mod.conflict);
        }
        editor.apply();
    }

    private void process(final String path, final String name, final Iterator<String> iterator, String password, boolean first) {
        final File targetFile = new File(storage.getAbsolutePath() + "/" + name);
        if (first) {
            if (targetFile.exists()) {
                Toast.makeText(context, String.format(context.getString(R.string.import_mod_exists), name), Toast.LENGTH_LONG).show();
                if (iterator.hasNext()) {
                    process(path, iterator.next(), iterator, null, true);
                }
                return;
            }
            if (!targetFile.mkdir()) {
                Toast.makeText(context, R.string.store_mkdir_failed, Toast.LENGTH_LONG).show();
                return;
            }
        }
        File sourceFile = new File(path + name);
        int result = ModUtils.RESULT_STATE_INTERNAL_ERROR;
        if(sourceFile.isFile()){
            File temp = new File(context.getCacheDir().getAbsolutePath() + "/" + System.currentTimeMillis());
            try {
                FileUtils.forceMkdir(temp);
                int p7zip = P7ZipApi.executeCommand(String.format("7z x '%s' '-o%s' '-p%s' -aoa", sourceFile.getAbsolutePath(), temp.getAbsolutePath(), password));
                if(p7zip == 0){
                    result = ModUtils.Standardization(temp, targetFile);
                }
                FileUtils.deleteDirectory(temp);
                if(p7zip == 2){
                    result = ModUtils.RESULT_STATE_PASSWORD_ERROR;
                }
            } catch (IOException e) {
                e.printStackTrace();
                result = ModUtils.RESULT_STATE_INTERNAL_ERROR;
            }
        }else if(sourceFile.isDirectory()){
            result = ModUtils.Standardization(sourceFile, targetFile);
        }
        if(result >= 0){
            if(result == 0){
                Toast.makeText(context, String.format(context.getString(R.string.valid_files_zero), name), Toast.LENGTH_LONG).show();
            }
            mods.add(new Mod(name, false, Integer.MAX_VALUE, result, targetFile.getAbsolutePath(), null));
            notifyDataSetChanged();
            if (iterator.hasNext()) {
                process(path, iterator.next(), iterator, null, true);
            } else {
                notifyApply();
            }
        }
        switch (result) {
            case RESULT_STATE_INTERNAL_ERROR:
                Toast.makeText(context, String.format(context.getString(R.string.mod_decompression_error), name), Toast.LENGTH_LONG).show();
                try {
                    FileUtils.deleteDirectory(targetFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (iterator.hasNext()) {
                    process(path, iterator.next(), iterator, null, true);
                } else {
                    notifyApply();
                }
                break;
            case ModUtils.RESULT_STATE_PASSWORD_ERROR:
                this.passwordDialog.show();
                this.passwordDialog.setTitle(context.getString(R.string.input_mod_password, name, password));
                this.passwordDialog.setPositiveListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ModsAdapter.this.passwordDialog.hide();
                        process(path, name, iterator, ModsAdapter.this.passwordDialog.edit.getText().toString(), false);
                    }
                });
                this.passwordDialog.setNegativeListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ModsAdapter.this.passwordDialog.hide();
                        try {
                            FileUtils.deleteDirectory(targetFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (iterator.hasNext()) {
                            process(path, iterator.next(), iterator, null, true);
                        } else {
                            notifyApply();
                        }
                    }
                });
                break;
        }
    }

    public void addMods(String path, List<String> names) {
        Log.d(MainApplication.LOG_TAG, String.format("addMods:path=%s,names[0]=%s", path, names.get(0)));
        Iterator<String> iterator = names.iterator();
        process(path, iterator.next(), iterator, null, true);
    }

    public void addExternalMod(String path, List<String> names) {
        String name = names.get(0);
        File targetFile = new File(storage.getAbsolutePath() + "/" + name);
        File cacheFile = new File(externalCache.getAbsolutePath() + "/" + name);
        File sourceFile = new File(path + name);
        if (targetFile.exists()) {
            Toast.makeText(context, String.format(context.getString(R.string.import_mod_exists), name), Toast.LENGTH_LONG).show();
        }
        if(cacheFile.exists()){
            try {
                FileUtils.deleteDirectory(cacheFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileUtils.write(targetFile, sourceFile.getAbsolutePath());
            int result = ModUtils.Standardization(sourceFile, cacheFile);
            mods.add(new Mod(name, false, Integer.MAX_VALUE, result, targetFile.getAbsolutePath(), null));
            notifyDataSetChanged();
        } catch (IOException e) {
            e.printStackTrace();
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
        final File file = new File(mod.path);
        holder.name.setText(mod.name);
        holder.file.setText(String.format(context.getString(R.string.mod_list_item_content), mod.fileCount));
        holder.aSwitch.setChecked(mod.enable);
        holder.aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) {
                    enableItemCount += isChecked ? 1 : -1;
                    mod.enable = isChecked;
                    if (listener != null) {
                        listener.onDataChange();
                    }
                    if(file.isFile()){
                        if(mod.enable){
                            try {
                                String externalPath = FileUtils.readFileToString(file);
                                FileAlterationObserver observer = new FileAlterationObserver(externalPath);
                                observer.addListener(ModsAdapter.this);
                                observers.put(mod.name,observer);
                                monitor.addObserver(observer);
                                Toast.makeText(context, context.getString(R.string.external_mods_observer_start, mod.name), Toast.LENGTH_LONG).show();
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            FileAlterationObserver observer = observers.get(mod.name);
                            if(observer != null){
                                monitor.removeObserver(observer);
                                observers.remove(mod.name);
                            }
                            Toast.makeText(context, context.getString(R.string.external_mods_observer_stop, mod.name), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        });
        if(file.isDirectory()){
            final File[] images = new File(mod.path + "/images").listFiles();
            if(images != null && images.length != 0){
                holder.icon.setImageDrawable(Drawable.createFromPath(images[0].getAbsolutePath()));
            } else {
                holder.icon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_mod_icon_default));
            }
            FileInputStream inputStream = null;
            JSONObject info = null;
            try {
                inputStream = new FileInputStream(mod.path + "/info.json");
                byte[] bytes = new byte[inputStream.available()];
                if(inputStream.read(bytes) == -1){
                    throw new IOException("map.json read failed.");
                }
                String json = new String(bytes);
                info = new JSONObject(json);
            } catch (Exception e) {
                Log.d(MainApplication.LOG_TAG, e.getMessage());
            }finally {
                if(inputStream != null){
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            final JSONObject finalInfo = info;
            String author = null;
            if(info != null){
                try {
                    author = info.getString("author");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if(author != null){
                holder.conflict.setVisibility(View.VISIBLE);
                holder.author.setText(author);
            } else {
                holder.author.setVisibility(View.INVISIBLE);
            }
            if(mod.conflict == null || !showConflict){
                holder.conflict.setVisibility(View.INVISIBLE);
            } else {
                holder.conflict.setVisibility(View.VISIBLE);
                holder.conflict.setText(String.format(Locale.getDefault(), "%d", mod.conflict.size()));
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if((images == null || images.length == 0) &&  finalInfo == null && (mod.conflict == null || !showConflict)){
                        Toast.makeText(context, R.string.no_preview_info_in_mod, Toast.LENGTH_LONG).show();
                    } else {
                        new ModInfoDialog(context, images, finalInfo, showConflict?mod.conflict:null).show();
                    }
                }
            });
        } else {
            holder.icon.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_folder_special));
            holder.itemView.setOnClickListener(null);
            holder.itemView.setOnClickListener(null);
            holder.author.setVisibility(View.INVISIBLE);
            holder.conflict.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return mods.size();
    }

    public void removeAllMods(){
        for(Mod mod: mods){
            File targetFile = new File(storage.getAbsolutePath() + "/" + mod.name);
            try {
                if(targetFile.isDirectory()){
                    FileUtils.deleteDirectory(targetFile);
                } else {
                    FileUtils.forceDelete(targetFile);
                    if(mod.enable){
                        FileAlterationObserver observer = observers.get(mod.name);
                        if(observer != null){
                            monitor.removeObserver(observer);
                            observers.remove(mod.name);
                        }
                        Toast.makeText(context, context.getString(R.string.external_mods_observer_stop, mod.name), Toast.LENGTH_LONG).show();
                    }
                }
                mods.remove(mod);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, context.getString(R.string.mod_file_delete_failed, mod.name), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            Mod mod = mods.get(operationPosition);
            File targetFile = new File(storage.getAbsolutePath() + "/" + mod.name);
            try {
                if(targetFile.isDirectory()){
                    FileUtils.deleteDirectory(targetFile);
                } else {
                    FileUtils.forceDelete(targetFile);
                    if(mod.enable){
                        FileAlterationObserver observer = observers.get(mod.name);
                        if(observer != null){
                            monitor.removeObserver(observer);
                            observers.remove(mod.name);
                        }
                        Toast.makeText(context, context.getString(R.string.external_mods_observer_stop, mod.name), Toast.LENGTH_LONG).show();
                    }
                }
                mods.remove(mod);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, context.getString(R.string.mod_file_delete_failed, mod.name), Toast.LENGTH_SHORT).show();
            }
        }
        ModsAdapter.this.notifyDataSetChanged();
    }

    @Override
    public void onStart(FileAlterationObserver fileAlterationObserver) {

    }

    @Override
    public void onDirectoryCreate(File file) {

    }

    @Override
    public void onDirectoryChange(File file) {

    }

    @Override
    public void onDirectoryDelete(File file) {

    }

    @Override
    public void onFileCreate(File file) {
        long current = System.currentTimeMillis();
        if(current - lastExternalChangeTime > 3000){
            listener.onExternalChange();
        }
        lastExternalChangeTime = current;
    }

    @Override
    public void onFileChange(File file) {
        long current = System.currentTimeMillis();
        if(current - lastExternalChangeTime > 3000){
            listener.onExternalChange();
        }
        lastExternalChangeTime = current;
    }

    @Override
    public void onFileDelete(File file) {

    }

    @Override
    public void onStop(FileAlterationObserver fileAlterationObserver) {

    }


    class ViewHolder extends RecyclerView.ViewHolder {
        private Switch aSwitch;
        private ImageView icon;
        private TextView name;
        private TextView file;
        private TextView author;
        private TextView conflict;

        public ViewHolder(View itemView) {
            super(itemView);
            aSwitch = itemView.findViewById(R.id.mod_list_item_switch);
            icon = itemView.findViewById(R.id.mod_list_item_icon);
            name = itemView.findViewById(R.id.mod_list_item_name);
            file = itemView.findViewById(R.id.mod_list_item_file);
            author = itemView.findViewById(R.id.mod_list_item_author);
            conflict = itemView.findViewById(R.id.mod_list_item_conflict);
        }
    }

    class CallBack extends ItemTouchHelper.Callback {

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            int swipeFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder origin, RecyclerView.ViewHolder target) {
            Mod originMod = mods.get(origin.getAdapterPosition());
            Mod targetMod = mods.get(target.getAdapterPosition());
            int order = originMod.order;
            originMod.order = targetMod.order;
            targetMod.order = order;
            Collections.swap(mods, origin.getAdapterPosition(), target.getAdapterPosition());
            ModsAdapter.this.notifyItemMoved(origin.getAdapterPosition(), target.getAdapterPosition());
            if (listener != null) {
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
        void onExternalChange();
    }

}
