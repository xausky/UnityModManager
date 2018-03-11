package io.github.xausky.unitymodmanager.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import io.github.xausky.unitymodmanager.MainApplication;
import io.github.xausky.unitymodmanager.R;
import io.github.xausky.unitymodmanager.adapter.ModsAdapter;
import io.github.xausky.unitymodmanager.domain.Mod;
import io.github.xausky.unitymodmanager.utils.FileUtils;
import io.github.xausky.unitymodmanager.utils.ZipUtils;
import ru.bartwell.exfilepicker.ExFilePicker;
import ru.bartwell.exfilepicker.data.ExFilePickerResult;

/**
 * Created by xausky on 18-3-3.
 */

public class ModFragment extends BaseFragment implements ModsAdapter.OnDataChangeListener {
    private static final int MOD_FILE_PICKER_RESULT = 1;
    private View view;
    private RecyclerView recyclerView;
    private ModsAdapter adapter;
    public boolean needPatch;
    private Context context;
    private File storeFile;
    private File backupFile;

    @Override
    public BaseFragment setBase(Context base) {
        backupFile = new File(base.getFilesDir().getAbsolutePath() + "/backup");
        storeFile = new File(base.getExternalFilesDir("mods").getAbsolutePath());
        if(!storeFile.exists()){
            if(storeFile.mkdir()){
                Toast.makeText(base, R.string.store_mkdir_failed, Toast.LENGTH_LONG).show();
            }
        }
        if(!backupFile.exists()){
            if(backupFile.mkdir()){
                Toast.makeText(base, R.string.store_mkdir_failed, Toast.LENGTH_LONG).show();
            }
        }
        adapter = new ModsAdapter(storeFile, base);
        adapter.setListener(this);
        return super.setBase(base);
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
        if(view == null){
            view = inflater.inflate(R.layout.mod_fragment, container, false);
            recyclerView = view.findViewById(R.id.mod_list);
            adapter.setRecyclerView(recyclerView);
        }
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
        filePicker.setShowOnlyExtensions("zip");
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

    public int patch(String apkPath){
        List<Mod> mods = adapter.getMods();
        File fusionFile = new File(context.getCacheDir().getAbsolutePath() + "/fusion");
        if(!FileUtils.deleteFile(fusionFile)){
            Log.d(MainApplication.LOG_TAG, "deleteFile Failed: " + fusionFile);
            return FileUtils.RESULT_STATE_INTERNAL_ERROR;
        }
        if(!fusionFile.mkdir()){
            Log.d(MainApplication.LOG_TAG, "mkdir Failed: " + fusionFile);
            return FileUtils.RESULT_STATE_INTERNAL_ERROR;
        }
        for(Mod mod : mods){
            if(mod.enable){
                int result = FileUtils.copyModDirectoryFile(new File(storeFile.getAbsolutePath() + "/" + mod.name),
                        fusionFile.getAbsolutePath(), adapter.forceMode);
                if(result != FileUtils.RESULT_STATE_OK){
                    Log.d(MainApplication.LOG_TAG, "Copy Mod Directory File Failed: " + result);
                    return result;
                }
            }
        }
        int result = ZipUtils.patchZip(backupFile.getAbsolutePath(), fusionFile.getAbsolutePath(), "assets/bin/Data", apkPath);
        if(result != ZipUtils.RESULT_STATE_OK){
            Log.d(MainApplication.LOG_TAG, "Patch APK File Failed: " + result);
            return result;
        }
        adapter.notifyApply();
        return ZipUtils.RESULT_STATE_OK;
    }
}
