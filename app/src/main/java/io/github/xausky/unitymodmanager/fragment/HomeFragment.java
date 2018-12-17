package io.github.xausky.unitymodmanager.fragment;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.sip.SipRegistrationListener;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.pm.ShortcutInfoCompat;
import android.support.v4.content.pm.ShortcutManagerCompat;
import android.support.v4.graphics.drawable.IconCompat;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.allenliu.versionchecklib.v2.AllenVersionChecker;
import com.allenliu.versionchecklib.v2.builder.UIData;
import com.allenliu.versionchecklib.v2.callback.RequestVersionListener;
import com.lody.virtual.client.core.InstallStrategy;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.remote.InstallResult;
import com.lody.virtual.remote.InstalledAppInfo;

import io.github.xausky.unitymodmanager.MainActivity;
import io.github.xausky.unitymodmanager.dialog.ConfirmDialog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;

import io.github.xausky.unitymodmanager.MainApplication;
import io.github.xausky.unitymodmanager.R;
import io.github.xausky.unitymodmanager.ShortcutActivity;
import io.github.xausky.unitymodmanager.dialog.ApplicationChooseDialog;
import io.github.xausky.unitymodmanager.utils.ModUtils;
import io.github.xausky.unitymodmanager.utils.NativeUtils;
import ru.bartwell.exfilepicker.utils.Utils;

/**
 * Created by xausky on 18-3-3.
 */

public class HomeFragment extends BaseFragment implements View.OnClickListener, ApplicationChooseDialog.OnApplicationChooseDialogResultListener, DialogInterface.OnClickListener {
    public static final String PACKAGE_PREFERENCE_KEY = "PACKAGE_PREFERENCE_KEY";
    public static final String BASE_APK_PATH_KEY = "BASE_APK_PATH_KEY";
    public static final String ALL_APPLICATION_PACKAGE_REGEX = "^.*$";
    public static final int APK_MODIFY_MODEL_NONE = 0;
    public static final int APK_MODIFY_MODEL_VIRTUAL = 1;
    public static final int APK_MODIFY_MODEL_ROOT = 2;
    public String packageName;
    public String apkPath;
    public String baseApkPath;
    public String backupPath;
    public String persistentPath;
    public String obbPath;
    public String baseObbPath;
    public int apkModifyModel;
    public boolean persistentSupport;
    public boolean obbSupport;
    private ConfirmDialog confirmDialog;
    private View view;
    private TextView summary;
    private TextView clientState;
    private TextView currentVersion;
    private String currentVersionString;
    private TextView latestVersion;
    private CardView clientStateCardView;
    private CardView mapFileCardView;
    private TextView mapFile;
    private AttachFragment attachFragment;
    private VisibilityFragment visibilityFragment;
    private ModFragment modFragment;
    private Context context;
    private SharedPreferences settings;
    private ApplicationChooseDialog dialog;
    private ProgressDialog progressDialog;

    @Override
    public BaseFragment setBase(Context base) {
        settings = base.getSharedPreferences(SettingFragment.SETTINGS_PREFERENCE_NAME, Context.MODE_PRIVATE);
        packageName = settings.getString(PACKAGE_PREFERENCE_KEY, null);
        apkModifyModel = Integer.valueOf(settings.getString("apk_modify_model", "1"));
        persistentSupport = settings.getBoolean("persistent_support", false);
        obbSupport = settings.getBoolean("obb_support", false);
        return super.setBase(base);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        context = inflater.getContext();
        settings = context.getSharedPreferences(SettingFragment.SETTINGS_PREFERENCE_NAME, Context.MODE_PRIVATE);
        apkModifyModel = Integer.valueOf(settings.getString("apk_modify_model", "1"));
        persistentSupport = settings.getBoolean("persistent_support", false);
        obbSupport = settings.getBoolean("obb_support", false);
        dialog = new ApplicationChooseDialog(context, this, ALL_APPLICATION_PACKAGE_REGEX, apkModifyModel == APK_MODIFY_MODEL_VIRTUAL, true);
        dialog.setListener(this);
        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle(R.string.progress_dialog_title);
        progressDialog.setMessage(getString(R.string.progress_dialog_message));
        progressDialog.setCancelable(false);
        if (view == null) {
            confirmDialog = new ConfirmDialog(this.getActivity(), this);
            confirmDialog.setMessage(context.getString(R.string.install_client_remove_all_mod_confirm));
            view = inflater.inflate(R.layout.home_fragment, container, false);
            attachFragment = (AttachFragment) BaseFragment.fragment(R.id.nav_attach, this.getActivity().getApplication());
            visibilityFragment = (VisibilityFragment) BaseFragment.fragment(R.id.nav_visibility, this.getActivity().getApplication());
            modFragment = (ModFragment) BaseFragment.fragment(R.id.nav_mod, this.getActivity().getApplication());
            summary = (TextView) view.findViewById(R.id.home_summary);
            currentVersion = (TextView) view.findViewById(R.id.home_current_version);
            latestVersion = (TextView) view.findViewById(R.id.home_latest_version);
            clientState = (TextView) view.findViewById(R.id.home_client_state);
            clientStateCardView = (CardView) view.findViewById(R.id.home_client_state_card_view);
            clientStateCardView.setOnClickListener(this);
            mapFileCardView = view.findViewById(R.id.home_map_file_card_view);
            mapFile = view.findViewById(R.id.home_map_file);
            mapFileCardView.setOnClickListener(this);
            currentVersionString = "unknown";
            try {
                currentVersionString = "v" + context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            currentVersion.setText(String.format(getText(R.string.home_current_version).toString(), currentVersionString));
            checkVersion();
        }
        clientUpdate();
        return view;
    }

    private void checkVersion() {
        AllenVersionChecker
                .getInstance()
                .requestVersion()
                .setRequestUrl("https://api.github.com/repos/xausky/UnityModManager/releases")
                .request(new RequestVersionListener() {
                    @Nullable
                    @Override
                    public UIData onRequestVersionSuccess(String result) {
                        try {
                            JSONArray array = new JSONArray(result);
                            JSONObject latestRelease = null;
                            for (int i = 0; i < array.length(); ++i) {
                                JSONObject release = array.getJSONObject(i);
                                if (!release.getBoolean("prerelease")) {
                                    latestRelease = release;
                                    break;
                                }
                            }
                            if (latestRelease != null) {
                                String latestVersion = latestRelease.getString("tag_name");
                                final String textViewString = String.format(context.getString(R.string.home_latest_version), latestVersion);
                                HomeFragment.this.latestVersion.setText(textViewString);
                                if (currentVersionString.indexOf('-') > 0) {
                                    return null;
                                }
                                if (!currentVersionString.equals(latestVersion)) {
                                    UIData data = UIData.create();
                                    data.setTitle(getString(R.string.new_version_release, latestVersion));
                                    data.setContent(getString(R.string.update_logs, latestRelease.getString("body")));
                                    data.setDownloadUrl(latestRelease.getJSONArray("assets").getJSONObject(0).getString("browser_download_url"));
                                    return data;
                                } else {
                                    Drawable check = context.getResources().getDrawable(R.drawable.ic_check_circle);
                                    check.setBounds(0, 0, check.getMinimumWidth(), check.getMinimumHeight());
                                    HomeFragment.this.latestVersion.setCompoundDrawables(check, null, null, null);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    public void onRequestVersionFailure(String message) {

                    }
                }).executeMission(getActivity().getBaseContext());
    }

    public void ImportMapFile() {
        ModUtils.map = new HashMap<>();
        File persistentMap = new File(getBase().getFilesDir() + "/persistent.map");
        if (persistentMap.exists() && persistentSupport) {
            FileReader reader = null;
            BufferedReader bufferedReader = null;
            try {
                reader = new FileReader(persistentMap);
                bufferedReader = new BufferedReader(reader);
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] column = line.split(":");
                    if (column.length == 2) {
                        ModUtils.map.put(column[0], column[1]);
                    }
                }
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
        File obbMap = new File(getBase().getFilesDir() + "/obb.map");
        if (obbMap.exists() && obbSupport) {
            FileReader reader = null;
            BufferedReader bufferedReader = null;
            try {
                reader = new FileReader(obbMap);
                bufferedReader = new BufferedReader(reader);
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] column = line.split(":");
                    if (column.length == 2) {
                        ModUtils.map.put(column[0], column[1]);
                    }
                }
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
        File apkMap = new File(this.getBase().getFilesDir() + "/apk.map");
        if (apkMap.exists() && apkModifyModel != APK_MODIFY_MODEL_NONE) {
            FileReader reader = null;
            BufferedReader bufferedReader = null;
            try {
                reader = new FileReader(apkMap);
                bufferedReader = new BufferedReader(reader);
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] column = line.split(":");
                    if (column.length == 2) {
                        ModUtils.map.put(column[0], column[1]);
                    }
                }
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }

    private void clientUpdate() {
        String versionName = null;
        int versionCode = 0;
        if (apkModifyModel == APK_MODIFY_MODEL_ROOT || apkModifyModel == APK_MODIFY_MODEL_NONE) {
            try {
                versionName = context.getPackageManager().getPackageInfo(packageName, 0).versionName;
                versionCode = context.getPackageManager().getPackageInfo(packageName, 0).versionCode;
                apkPath = context.getPackageManager().getApplicationInfo(packageName, 0).sourceDir;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            InstalledAppInfo installedAppInfo = VirtualCore.get().getInstalledAppInfo(packageName, 0);
            if (installedAppInfo != null) {
                versionName = installedAppInfo.getPackageInfo(0).versionName;
                versionCode = installedAppInfo.getPackageInfo(0).versionCode;
                apkPath = installedAppInfo.apkPath;
            }
        }
        obbPath = context.getObbDir().getParentFile().getAbsolutePath() + "/" + packageName + "/main." + versionCode + '.' + packageName + ".obb";
        persistentPath = context.getExternalFilesDir(null).getParentFile().getParentFile().getAbsolutePath() + "/" + packageName + "/files";
        baseApkPath = context.getFilesDir().getAbsolutePath() + "/base.apk";
        if (apkModifyModel == APK_MODIFY_MODEL_VIRTUAL) {
            baseApkPath = settings.getString(BASE_APK_PATH_KEY, null);
        }
        baseObbPath = context.getFilesDir().getAbsolutePath() + "/base.obb";
        backupPath = context.getFilesDir().getAbsolutePath() + "/backup";
        File backup = new File(backupPath);
        if (!backup.exists()) {
            if (!backup.mkdirs()) {
                Toast.makeText(context, R.string.create_backup_folder_failed, Toast.LENGTH_LONG).show();
            }
        }
        if (versionName != null) {
            clientState.setText(String.format(getText(R.string.home_client_installed).toString(), versionName));
            clientState.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(context, R.drawable.ic_check), null, null, null);
        } else {
            clientState.setText(getText(R.string.home_client_uninstalled));
            clientState.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(context, R.drawable.ic_clear), null, null, null);
        }
        mapFile.setText(String.format(context.getString(R.string.map_file_size), ModUtils.map.size()));
        if (ModUtils.map.size() > 0) {
            mapFile.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(context, R.drawable.ic_check), null, null, null);
        }
        String summaryString = String.format(getString(R.string.home_summary_context),
                modFragment.getEnableItemCount(),
                modFragment.getItemCount(),
                attachFragment.getItemCount(),
                visibilityFragment.getItemCount(),
                VirtualCore.get().isStartup() ? VirtualCore.get().getInstalledAppCount() : -1);
        summary.setText(summaryString);
    }

    @Override
    public void onClick(View v) {
        if (v.equals(clientStateCardView)) {
            dialog.show();
        } else {
            if (apkPath == null) {
                Toast.makeText(context, R.string.install_client_download_resource, Toast.LENGTH_LONG).show();
                return;
            }
            if (apkModifyModel != APK_MODIFY_MODEL_NONE) {
                NativeUtils.GenerateApkMapFile(apkPath, HomeFragment.this.context.getFilesDir().getAbsolutePath() + "/apk.map");
            }
            if (persistentSupport) {
                Log.d(MainApplication.LOG_TAG, "persistentSupport:" + persistentPath);
                NativeUtils.GenerateFolderMapFile(persistentPath, HomeFragment.this.context.getFilesDir().getAbsolutePath() + "/persistent.map");
            }
            if (obbSupport) {
                Log.d(MainApplication.LOG_TAG, "obbSupport:" + obbPath);
                NativeUtils.GenerateApkMapFile(obbPath, HomeFragment.this.context.getFilesDir().getAbsolutePath() + "/obb.map");
            }
            ImportMapFile();
            clientUpdate();
            if (persistentSupport || apkModifyModel != APK_MODIFY_MODEL_NONE || obbSupport) {
                Toast.makeText(context, R.string.map_file_generate_success, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, R.string.confirm_modify_source, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        dialog.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroyView() {
        dialog.dismiss();
        super.onDestroyView();
    }

    private void clientInstall(final String apkPath, final String packageName) {
        progressDialog.show();
        new Thread() {
            @Override
            public void run() {
                Log.d(MainApplication.LOG_TAG, apkPath);
                final String resultString;
                String result = getString(R.string.install_failed);
                int versionCode = 0;
                if (apkModifyModel == APK_MODIFY_MODEL_ROOT || apkModifyModel == APK_MODIFY_MODEL_NONE) {
                    try {
                        String basePath = HomeFragment.this.context.getFilesDir().getAbsolutePath() + "/base.apk";
                        if (apkModifyModel == APK_MODIFY_MODEL_ROOT) {
                            FileUtils.copyFile(new File(apkPath), new File(basePath));
                            HomeFragment.this.baseApkPath = basePath;
                        }
                        HomeFragment.this.packageName = packageName;
                        HomeFragment.this.apkPath = apkPath;
                        versionCode = context.getPackageManager().getPackageInfo(packageName, 0).versionCode;
                        result = getString(R.string.install_success);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                } else {
                    final InstallResult installResult = VirtualCore.get().installPackage(apkPath, InstallStrategy.UPDATE_IF_EXIST);
                    if (installResult.isSuccess) {
                        String basePath = HomeFragment.this.context.getFilesDir().getAbsolutePath() + "/base.apk";
                        HomeFragment.this.packageName = installResult.packageName;
                        HomeFragment.this.baseApkPath = apkPath;
                        settings.edit().putString(BASE_APK_PATH_KEY, apkPath).apply();
                        InstalledAppInfo info = VirtualCore.get().getInstalledAppInfo(HomeFragment.this.packageName, 0);
                        HomeFragment.this.apkPath = info.apkPath;
                        result = getString(R.string.install_success_create_shortcut);
                        versionCode = info.getPackageInfo(0).versionCode;
                    } else {
                        result = installResult.error;
                    }
                }
                if (obbSupport && versionCode != 0) {
                    try {
                        String basePath = HomeFragment.this.context.getFilesDir().getAbsolutePath() + "/base.obb";
                        obbPath = context.getObbDir().getParentFile().getAbsolutePath() + "/" + packageName + "/main." + versionCode + '.' + packageName + ".obb";
                        FileUtils.copyFile(new File(obbPath), new File(basePath));
                        baseObbPath = basePath;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                settings.edit()
                        .putString(PACKAGE_PREFERENCE_KEY, HomeFragment.this.packageName)
                        .apply();
                progressDialog.dismiss();
                resultString = result;
                HomeFragment.this.view.post(new Runnable() {
                    @Override
                    public void run() {
                        clientUpdate();
                        Toast.makeText(context, resultString, Toast.LENGTH_LONG).show();
                        if (modFragment.getEnableItemCount() > 0) {
                            modFragment.setNeedPatch(true);
                        }
                        if (modFragment.getItemCount() > 0) {
                            confirmDialog.show();
                        }
                    }
                });
            }
        }.start();
    }

    public void crateShortcut(InstalledAppInfo info) {
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            PackageManager manager = VirtualCore.get().getPackageManager();
            String name = manager.getApplicationLabel(info.getApplicationInfo(0)) + context.getString(R.string.shortcut_postfix);
            BitmapDrawable icon = (BitmapDrawable) manager.getApplicationIcon(info.getApplicationInfo(0));
            Intent shortcutInfoIntent = new Intent(Intent.ACTION_VIEW);
            shortcutInfoIntent.setClass(context, ShortcutActivity.class);
            shortcutInfoIntent.putExtra("io.github.xausky.unitymodmanager.launchPackage", info.packageName);
            ShortcutInfoCompat shortcutInfo = new ShortcutInfoCompat.Builder(context, name)
                    .setIcon(IconCompat.createWithBitmap(icon.getBitmap()))
                    .setShortLabel(name)
                    .setIntent(shortcutInfoIntent)
                    .build();
            ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null);
        }
    }

    @Override
    public void OnApplicationChooseDialogResult(String packageName, String apkPath) {
        clientInstall(apkPath, packageName);
        dialog.hide();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            modFragment.removeAllMods();
        }
    }
}
