package io.github.xausky.unitymodmanager.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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
import com.lody.virtual.client.ipc.VActivityManager;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import io.github.xausky.unitymodmanager.MainActivity;
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
            new ExportApkTask(dialog).execute();
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
            if (modFragment.needPatch) {
                result = modFragment.patch(homeFragment.apkPath, homeFragment.baseApkPath);
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
                modFragment.needPatch = false;
                String exportPath = Environment.getExternalStorageDirectory() + "/out.apk";
                Toast.makeText(modFragment.getBase(), "导出整合包成功=>" + exportPath, Toast.LENGTH_LONG).show();
            } else if (result == RESULT_STATE_INTERNAL_ERROR) {
                Toast.makeText(modFragment.getBase(), "导出整合包失败，请确保程序可以读写外部存储。", Toast.LENGTH_LONG).show();
            }
        }
    }
}