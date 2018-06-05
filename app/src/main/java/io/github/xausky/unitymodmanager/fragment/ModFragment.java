package io.github.xausky.unitymodmanager.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.lody.virtual.client.core.VirtualCore;
import com.topjohnwu.superuser.Shell;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.io.IOException;
import java.util.List;

import io.github.xausky.unitymodmanager.MainActivity;
import io.github.xausky.unitymodmanager.MainApplication;
import io.github.xausky.unitymodmanager.R;
import io.github.xausky.unitymodmanager.adapter.ModsAdapter;
import io.github.xausky.unitymodmanager.domain.Mod;
import io.github.xausky.unitymodmanager.utils.ModUtils;
import io.github.xausky.unitymodmanager.utils.NativeUtils;
import ru.bartwell.exfilepicker.ExFilePicker;
import ru.bartwell.exfilepicker.data.ExFilePickerResult;

/**
 * Created by xausky on 18-3-3.
 */

public class ModFragment extends BaseFragment implements ModsAdapter.OnDataChangeListener {
    private static final int MOD_FILE_PICKER_RESULT = 1;
    private static final int EXTERNAL_MOD_FILE_PICKER_RESULT = 2;
    private static final String NEED_PATCH_PREFERENCES_KEY = "NEED_PATCH_PREFERENCES_KEY";
    private View view;
    private RecyclerView recyclerView;
    private ModsAdapter adapter;
    private boolean needPatch;
    private Context context;
    private File storage;
    private File externalCache;
    private SharedPreferences settingsPreferences;
    private Handler handler;

    @Override
    public BaseFragment setBase(Context base) {
        handler = new Handler(Looper.getMainLooper());
        storage = base.getExternalFilesDir("mods");
        if(!storage.exists()){
            if(!storage.mkdir()){
                Toast.makeText(base, R.string.store_mkdir_failed, Toast.LENGTH_LONG).show();
            }
        }
        externalCache = base.getExternalFilesDir("caches");
        if(!externalCache.exists()){
            if(!externalCache.mkdir()){
                Toast.makeText(base, R.string.store_mkdir_failed, Toast.LENGTH_LONG).show();
            }
        }
        this.settingsPreferences = base.getSharedPreferences(SettingFragment.SETTINGS_PREFERENCE_NAME, Context.MODE_PRIVATE);
        needPatch = settingsPreferences.getBoolean(NEED_PATCH_PREFERENCES_KEY, false);
        adapter = new ModsAdapter(storage, externalCache, base);
        adapter.setListener(this);
        return super.setBase(base);
    }

    public boolean isNeedPatch() {
        return needPatch;
    }

    public void setNeedPatch(boolean needPatch) {
        this.needPatch = needPatch;
        settingsPreferences.edit().putBoolean(NEED_PATCH_PREFERENCES_KEY, needPatch).apply();
    }

    public int getItemCount(){
        return adapter.getItemCount();
    }

    public int getEnableItemCount(){
        return adapter.getEnableItemCount();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        this.context = inflater.getContext();
        view = inflater.inflate(R.layout.mod_fragment, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.mod_list);
        adapter.setRecyclerView(recyclerView);
        return view;
    }

    @Override
    public int actionButtonVisibility() {
        return View.VISIBLE;
    }

    @Override
    public void OnActionButtonClick() {
        if(ModUtils.map == null){
            Toast.makeText(context, "请先安装客户端以生成索引。", Toast.LENGTH_LONG).show();
            return;
        }
        ExFilePicker filePicker = new ExFilePicker();
        filePicker.setShowOnlyExtensions("zip", "rar", "7z");
        filePicker.setCanChooseOnlyOneItem(false);
        filePicker.start(this, MOD_FILE_PICKER_RESULT);
    }

    @Override
    public void OnActionButtonLongClick() {
        if(ModUtils.map == null){
            Toast.makeText(context, "请先安装客户端以生成索引。", Toast.LENGTH_LONG).show();
            return;
        }
        ExFilePicker filePicker = new ExFilePicker();
        filePicker.setShowOnlyExtensions();
        filePicker.setCanChooseOnlyOneItem(true);
        filePicker.start(this, EXTERNAL_MOD_FILE_PICKER_RESULT);
        Toast.makeText(context, "外部模组目录导入模式，非模组开发者不推荐使用。", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == MOD_FILE_PICKER_RESULT) {
            ExFilePickerResult result = ExFilePickerResult.getFromIntent(data);
            if(result != null) {
                adapter.addMods(result.getPath(), result.getNames());
            }
        } else if(requestCode == EXTERNAL_MOD_FILE_PICKER_RESULT) {
            ExFilePickerResult result = ExFilePickerResult.getFromIntent(data);
            if(result != null) {
                adapter.addExternalMod(result.getPath(), result.getNames());
            }
        }
    }

    @Override
    public void onDataChange() {
        needPatch = true;
    }

    @Override
    public void onExternalChange() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "检测到外部模组改变，重新启动游戏。", Toast.LENGTH_LONG).show();
                HomeFragment fragment = (HomeFragment)BaseFragment.fragment(R.id.nav_home);
                VirtualCore.get().killApp(fragment.packageName, 0);
                MainActivity activity = (MainActivity)context;
                needPatch = true;
                activity.launch();
            }
        });
    }

    public int patch(String apkPath, String baseApkPath, String persistentPath, String backupPath,  int apkModifyModel, boolean persistentSupport){
        if(apkModifyModel == HomeFragment.APK_MODIFY_MODEL_ROOT){
            //暂时禁用SELinux，并且修改目标APK权限为666。
            Shell.Sync.su("setenforce 0", "chmod 666 " + apkPath);
        }
        try {
            Log.d(MainApplication.LOG_TAG, "patch: apkPath=" + apkPath + ", baseApkPath=" + baseApkPath + ", apkModifyModel=" + apkModifyModel);
            List<Mod> mods = adapter.getMods();
            File fusionFile = new File(getBase().getCacheDir().getAbsolutePath() + "/fusion");
            try {
                FileUtils.deleteDirectory(fusionFile);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(MainApplication.LOG_TAG, "deleteFile Failed: " + fusionFile);
                return ModUtils.RESULT_STATE_INTERNAL_ERROR;
            }
            if(!fusionFile.mkdir()){
                Log.d(MainApplication.LOG_TAG, "mkdir Failed: " + fusionFile);
                return ModUtils.RESULT_STATE_INTERNAL_ERROR;
            }
            for(Mod mod : mods){
                if(mod.enable){
                    File modFile = new File(storage.getAbsolutePath() + "/" + mod.name);
                    try {
                        if(modFile.isFile()){
                            File externalFile = new File(FileUtils.readFileToString(modFile));
                            int result = ModUtils.Standardization(externalFile, fusionFile);
                            mod.fileCount = result;
                        } else {
                            FileUtils.copyDirectory(modFile, fusionFile);
                        }
                    } catch (IOException e) {
                        Log.d(MainApplication.LOG_TAG, "Copy Mod Directory File Failed: " + e.getMessage());
                        return ModUtils.RESULT_STATE_INTERNAL_ERROR;
                    }
                }
            }
            if(apkModifyModel != HomeFragment.APK_MODIFY_MODEL_NONE){
                int result = NativeUtils.PatchApk(baseApkPath, apkPath, fusionFile.getAbsolutePath());
                if(result != NativeUtils.RESULT_STATE_OK){
                    Log.d(MainApplication.LOG_TAG, "Patch APK File Failed: " + result + ",apkPath:" + apkPath + ",baseApkPath:" + baseApkPath);
                    return result;
                }
            }
            if(persistentSupport){
                int result = NativeUtils.PatchFolder(persistentPath,fusionFile.getAbsolutePath(), backupPath);
                if(result != NativeUtils.RESULT_STATE_OK){
                    Log.d(MainApplication.LOG_TAG, "Patch Persistent Folder Failed: " + result + ",persistentPath=" + persistentPath + ",backupPath=" + backupPath);
                    return result;
                }
            }
            adapter.notifyApply();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
            return NativeUtils.RESULT_STATE_OK;
        }finally {
            if(apkModifyModel == HomeFragment.APK_MODIFY_MODEL_ROOT){
                //修改目标APK权限回644，并且重新启用SELinux。
                Shell.Sync.su("chmod 644 " + apkPath, "setenforce 0");
            }
        }
    }
}
