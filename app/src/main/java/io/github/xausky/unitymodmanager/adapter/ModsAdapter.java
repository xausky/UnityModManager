package io.github.xausky.unitymodmanager.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import io.github.xausky.unitymodmanager.R;
import io.github.xausky.unitymodmanager.dialog.ConfirmDialog;
import io.github.xausky.unitymodmanager.dialog.PasswordDialog;
import io.github.xausky.unitymodmanager.domain.Mod;
import io.github.xausky.unitymodmanager.fragment.SettingFragment;
import io.github.xausky.unitymodmanager.utils.ModUtils;
import io.github.xausky.unitymodmanager.utils.NativeUtils;

import static io.github.xausky.unitymodmanager.utils.NativeUtils.RESULT_STATE_INTERNAL_ERROR;

/**
 * Created by xausky on 18-3-9.
 */

public class ModsAdapter extends RecyclerView.Adapter<ModsAdapter.ViewHolder> implements DialogInterface.OnClickListener {
    public static final String MODS_SHARED_PREFERENCES_KEY = "mods";

    private List<Mod> mods = new ArrayList<>();
    private SharedPreferences preferences;
    private SharedPreferences settingPreferences;
    private Context context;
    private File storage;
    private OnDataChangeListener listener;
    private ConfirmDialog dialog;
    private int operationPosition;
    private int enableItemCount;
    private String defaultPassword;
    private PasswordDialog passwordDialog;

    public ModsAdapter(File storage, Context context) {
        this.storage = storage;
        this.preferences = context.getSharedPreferences(MODS_SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        this.settingPreferences = context.getSharedPreferences(SettingFragment.SETTINGS_PREFERENCE_NAME, Context.MODE_PRIVATE);
        this.enableItemCount = 0;
        File[] modFiles = storage.listFiles();
        for (File file : modFiles) {
            if (file.isDirectory()) {
                String name = file.getName();
                Mod mod = new Mod(name,
                        preferences.getBoolean(name + ":enable", false),
                        preferences.getInt(name + ":order", Integer.MAX_VALUE),
                        preferences.getInt(name + ":fileCount", -1));
                if (mod.enable) {
                    ++enableItemCount;
                }
                mods.add(mod);
            }
        }
        Collections.sort(mods);
        updateSetting();
    }

    public void updateSetting(){
        this.defaultPassword = settingPreferences.getString("setting_default_password", null);
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
                int p7zip = P7ZipApi.executeCommand(String.format("7z x -aoa -o%s -p%s %s", temp.getAbsolutePath(), password, sourceFile.getAbsolutePath()));
                if(p7zip == 0){
                    result = ModUtils.Standardization(temp, targetFile);
                }
                FileUtils.deleteDirectory(temp);
                if(p7zip == 2){
                    result = NativeUtils.RESULT_STATE_PASSWORD_ERROR;
                }
            } catch (IOException e) {
                e.printStackTrace();
                result = ModUtils.RESULT_STATE_INTERNAL_ERROR;
            }
        }else if(sourceFile.isDirectory()){
            result = ModUtils.Standardization(sourceFile, targetFile);
        }
        if(result >= 0){
            mods.add(new Mod(name, false, Integer.MAX_VALUE, result));
            notifyDataSetChanged();
            if (iterator.hasNext()) {
                process(path, iterator.next(), iterator, null, true);
            } else {
                notifyApply();
            }
        }
        switch (result) {
            case NativeUtils.RESULT_STATE_FILE_CONFLICT:
                Toast.makeText(context, String.format("模组[%s]内部存在文件冲突，如要强制安装请到设置开启强制安装选项。", name), Toast.LENGTH_LONG).show();
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
                break;
            case RESULT_STATE_INTERNAL_ERROR:
                Toast.makeText(context, String.format("模组[%s]解压错误，请确定模组文件是Zip文件。", name), Toast.LENGTH_LONG).show();
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
            case NativeUtils.RESULT_STATE_PASSWORD_ERROR:
                if(password == null && this.defaultPassword != null){
                    process(path, name, iterator, this.defaultPassword, false);
                } else {
                    this.passwordDialog.show();
                    this.passwordDialog.setTitle(String.format("请输入模组[%s]的解压密码", name));
                    if(password != null){
                        this.passwordDialog.setTitle(String.format("请输入模组[%s]的解压密码，尝试过的错误密码:%s", name, password));
                    }
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
                }
                break;
        }
    }

    public void addMods(String path, List<String> names) {
        Iterator<String> iterator = names.iterator();
        process(path, iterator.next(), iterator, null, true);
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
        holder.content.setText(String.format(context.getString(R.string.mod_list_item_content), mod.fileCount));
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
            Mod mod = mods.get(operationPosition);
            try {
                FileUtils.deleteDirectory(new File(storage.getAbsolutePath() + "/" + mod.name));
                mods.remove(mod);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, context.getString(R.string.mod_file_delete_failed) + mod.name, Toast.LENGTH_SHORT).show();
            }
        }
        ModsAdapter.this.notifyDataSetChanged();
    }


    class ViewHolder extends RecyclerView.ViewHolder {
        private Switch aSwitch;
        private ImageView icon;
        private TextView name;
        private TextView content;

        public ViewHolder(View itemView) {
            super(itemView);
            aSwitch = itemView.findViewById(R.id.mod_list_item_switch);
            icon = itemView.findViewById(R.id.mod_list_item_icon);
            name = itemView.findViewById(R.id.mod_list_item_name);
            content = itemView.findViewById(R.id.mod_list_item_content);
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
    }

}
