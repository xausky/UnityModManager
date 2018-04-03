package io.github.xausky.unitymodmanager.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
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

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
    private static final String NEED_PATCH_PREFERENCES_KEY = "NEED_PATCH_PREFERENCES_KEY";
    private View view;
    private RecyclerView recyclerView;
    private ModsAdapter adapter;
    private boolean needPatch;
    private Context context;
    private File storeFile;
    private SharedPreferences settingsPreferences;

    @Override
    public BaseFragment setBase(Context base) {
        storeFile = new File(base.getExternalFilesDir("mods").getAbsolutePath());
        if(!storeFile.exists()){
            if(storeFile.mkdir()){
                Toast.makeText(base, R.string.store_mkdir_failed, Toast.LENGTH_LONG).show();
            }
        }
        this.settingsPreferences = base.getSharedPreferences(SettingFragment.SETTINGS_PREFERENCE_NAME, Context.MODE_PRIVATE);
        needPatch = settingsPreferences.getBoolean(NEED_PATCH_PREFERENCES_KEY, false);
        adapter = new ModsAdapter(storeFile, base);
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
        recyclerView = view.findViewById(R.id.mod_list);
        adapter.setRecyclerView(recyclerView);
        adapter.updateSetting();
        return view;
    }

    @Override
    public int actionButtonVisibility() {
        return View.VISIBLE;
    }

    @Override
    public void OnActionButtonClick() {
        ExFilePicker filePicker = new ExFilePicker();
        filePicker.setShowOnlyExtensions("zip", "rar", "7z");
        filePicker.setHideHiddenFilesEnabled(true);
        filePicker.setCanChooseOnlyOneItem(false);
        filePicker.start(this, MOD_FILE_PICKER_RESULT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == MOD_FILE_PICKER_RESULT) {
            ExFilePickerResult result = ExFilePickerResult.getFromIntent(data);
            if(result != null) {
                adapter.addMods(result.getPath(), result.getNames());
            }
        }
    }

    @Override
    public void onDataChange() {
        needPatch = true;
    }

    public int patch(String apkPath, String baseApkPath){
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
                try {
                    FileUtils.copyDirectory(new File(storeFile.getAbsolutePath() + "/" + mod.name), fusionFile);
                } catch (IOException e) {
                    Log.d(MainApplication.LOG_TAG, "Copy Mod Directory File Failed: " + e.getMessage());
                    return ModUtils.RESULT_STATE_INTERNAL_ERROR;
                }
            }
        }
        int result = NativeUtils.patch(baseApkPath, apkPath, fusionFile.getAbsolutePath());
        if(result != NativeUtils.RESULT_STATE_OK){
            Log.d(MainApplication.LOG_TAG, "Patch APK File Failed: " + result + ",apkPath:" + apkPath + ",baseApkPath:" + baseApkPath);
            return result;
        }
        adapter.notifyApply();
        return NativeUtils.RESULT_STATE_OK;
    }
}
