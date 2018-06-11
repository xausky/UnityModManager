package io.github.xausky.unitymodmanager.fragment;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.lody.virtual.client.core.VirtualCore;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import io.github.xausky.unitymodmanager.R;
import io.github.xausky.unitymodmanager.utils.ModUtils;

import static io.github.xausky.unitymodmanager.utils.ModUtils.RESULT_STATE_INTERNAL_ERROR;

/**
 * Created by xausky on 18-3-3.
 */

public class SettingFragment extends PreferenceFragment {
    public static final String SETTINGS_PREFERENCE_NAME = "settings";
    private ProgressDialog dialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getPreferenceManager().setSharedPreferencesName(SETTINGS_PREFERENCE_NAME);
        this.addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        dialog = new ProgressDialog(inflater.getContext());
        dialog.setTitle(R.string.progress_dialog_title);
        dialog.setMessage(getString(R.string.progress_dialog_message));
        dialog.setCancelable(false);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if(preference.getKey().equals("setting_export_apk")){
            HomeFragment homeFragment = (HomeFragment) BaseFragment.fragment(R.id.nav_home);
            if(homeFragment.apkModifyModel == HomeFragment.APK_MODIFY_MODEL_NONE){
                Toast.makeText(this.getActivity(), R.string.none_modify_export, Toast.LENGTH_LONG).show();
                return super.onPreferenceTreeClick(preferenceScreen, preference);
            }
            if(homeFragment.apkPath == null || homeFragment.baseApkPath == null || !new File(homeFragment.baseApkPath).exists()){
                Toast.makeText(this.getActivity(), R.string.install_source_not_found, Toast.LENGTH_LONG).show();
            } else {
                new ExportApkTask(dialog).execute();
            }
        } else if(preference.getKey().equals("create_shortcut")){
            HomeFragment homeFragment = (HomeFragment) BaseFragment.fragment(R.id.nav_home);
            if(homeFragment.apkModifyModel != HomeFragment.APK_MODIFY_MODEL_VIRTUAL){
                Toast.makeText(this.getActivity(), R.string.no_virtual_model_shortcut, Toast.LENGTH_LONG).show();
                return super.onPreferenceTreeClick(preferenceScreen, preference);
            }
            if(homeFragment.apkPath == null || homeFragment.baseApkPath == null || !new File(homeFragment.baseApkPath).exists()){
                Toast.makeText(this.getActivity(), R.string.install_source_not_found, Toast.LENGTH_LONG).show();
            } else {
                homeFragment.crateShortcut(VirtualCore.get().getInstalledAppInfo(homeFragment.packageName,0));
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    static class ExportApkTask extends AsyncTask<Object, Object, Integer> {
        private ProgressDialog dialog;

        public ExportApkTask(ProgressDialog dialog) {
            super();
            this.dialog = dialog;
        }

        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @Override
        protected Integer doInBackground(Object... params) {
            int result = ModUtils.RESULT_STATE_OK;
            ModFragment modFragment = (ModFragment) BaseFragment.fragment(R.id.nav_mod);
            HomeFragment homeFragment = (HomeFragment) BaseFragment.fragment(R.id.nav_home);
            if (modFragment.isNeedPatch()) {
                result = modFragment.patch(homeFragment.apkPath, homeFragment.baseApkPath, homeFragment.persistentPath, homeFragment.obbPath, homeFragment.backupPath, homeFragment.apkModifyModel, homeFragment.persistentSupport, homeFragment.obbSupport);
            }
            if (result == ModUtils.RESULT_STATE_OK) {
                String exportPath = Environment.getExternalStorageDirectory() + "/out.apk";
                try {
                    FileUtils.copyFile(new File(homeFragment.apkPath), new File(exportPath));
                } catch (IOException e) {
                    e.printStackTrace();
                    result = RESULT_STATE_INTERNAL_ERROR;
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(Integer result) {
            dialog.hide();
            ModFragment modFragment = (ModFragment) BaseFragment.fragment(R.id.nav_mod);
            if (result == ModUtils.RESULT_STATE_OK) {
                modFragment.setNeedPatch(false);
                String exportPath = Environment.getExternalStorageDirectory() + "/out.apk";
                Toast.makeText(modFragment.getBase(), modFragment.getBase().getString(R.string.package_export_success,exportPath), Toast.LENGTH_LONG).show();
            } else if (result == RESULT_STATE_INTERNAL_ERROR) {
                Toast.makeText(modFragment.getBase(), R.string.package_export_failed, Toast.LENGTH_LONG).show();
            }
        }
    }
}