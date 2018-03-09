package io.github.xausky.unitymodmanager.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.lody.virtual.client.core.InstallStrategy;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.remote.InstallResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import io.github.xausky.unitymodmanager.MainApplication;
import io.github.xausky.unitymodmanager.R;
import io.github.xausky.unitymodmanager.dialog.ApplicationChooseDialog;
import io.github.xausky.unitymodmanager.dialog.ProgressDialog;

/**
 * Created by xausky on 18-3-3.
 */

public class HomeFragment extends BaseFragment implements View.OnClickListener, ApplicationChooseDialog.OnApplicationChooseDialogResultListener{
    private static final String PACKAGE_PREFERENCE_KEY = "PACKAGE_PREFERENCE_KEY";
    private static final String ALL_APPLICATION_PACKAGE_REGEX = "^.*$";
    public String packageName;
    private String apkPath;
    private View view;
    private TextView summary;
    private TextView clientState;
    private TextView currentVersion;
    private TextView latestVersion;
    private CardView clientStateCardView;
    private AttachFragment attachFragment;
    private Context context;
    private SharedPreferences settings;
    private ApplicationChooseDialog dialog;
    private VirtualCore va;

    @Override
    public BaseFragment setBase(Context base) {
        settings = base.getSharedPreferences(SettingFragment.SETTINGS_PREFERENCE_NAME, Context.MODE_PRIVATE);
        packageName = settings.getString(PACKAGE_PREFERENCE_KEY, null);
        return super.setBase(base);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        if(view == null){
            view = inflater.inflate(R.layout.home_fragment, container, false);
            attachFragment = (AttachFragment) BaseFragment.fragment(R.id.nav_attach);
            context = inflater.getContext();
            summary = view.findViewById(R.id.home_summary);
            va = VirtualCore.get();
            currentVersion = view.findViewById(R.id.home_current_version);
            latestVersion = view.findViewById(R.id.home_latest_version);
            clientState = view.findViewById(R.id.home_client_state);
            clientStateCardView = view.findViewById(R.id.home_client_state_card_view);
            clientStateCardView.setOnClickListener(this);
            dialog = new ApplicationChooseDialog(context, this, ALL_APPLICATION_PACKAGE_REGEX);
            dialog.setListener(this);
            String versionName = "unknown";
            try {
                versionName = "v" + context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            currentVersion.setText(String.format(getText(R.string.home_current_version).toString(), versionName));
            checkVersion();
        }
        clientUpdate();
        return view;
    }

    private void checkVersion(){
        new Thread(){
            @Override
            public void run() {
                try {
                    URL url = new URL("https://api.github.com/repos/xausky/UnityModManager/releases");
                    HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
                    connection.setRequestMethod("GET");
                    InputStream in=connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();
                    String line=null;
                    while((line=reader.readLine())!=null){
                        response.append(line);
                    }
                    JSONArray array = new JSONArray(response.toString());
                    JSONObject latestRelease = array.getJSONObject(0);
                    String latestVersion = latestRelease.getString("tag_name");
                    final String textViewString = String.format(context.getString(R.string.home_latest_version), latestVersion);
                    HomeFragment.this.latestVersion.post(new Runnable() {
                        @Override
                        public void run() {
                            HomeFragment.this.latestVersion.setText(textViewString);
                        }
                    });
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                va.addVisibleOutsidePackage("com.eg.android.AlipayGphone");
                va.addVisibleOutsidePackage("com.tencent.mm");
            }
        }.start();
    }

    private void clientUpdate(){
        try {
            PackageInfo packageInfo = va.getPackageManager().getPackageInfo(packageName, 0);
            String versionName = packageInfo.versionName;
            clientState.setText(String.format(getText(R.string.home_client_installed).toString(), versionName));
            clientState.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(context,R.drawable.ic_check),null, null, null);
        } catch (PackageManager.NameNotFoundException e) {
            clientState.setText(getText(R.string.home_client_uninstalled));
            clientState.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(context,R.drawable.ic_clear),null, null, null);
        }
        String summaryString = String.format(getString(R.string.home_summary_context),
                0,
                0,
                attachFragment.getItemCount(),
                0,
                va.getInstalledAppCount());
        summary.setText(summaryString);
    }

    @Override
    public void onClick(View v) {
        dialog.show();
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

    private void clientInstall(final String apkPath){
        final ProgressDialog progressDialog = new ProgressDialog(context, view);
        progressDialog.show();
        new Thread(){
            @Override
            public void run() {
                Log.d(MainApplication.LOG_TAG, apkPath);
                final InstallResult result = VirtualCore.get().installPackage(apkPath, InstallStrategy.UPDATE_IF_EXIST);
                final String resultString;
                if(result.isSuccess){
                    HomeFragment.this.packageName = result.packageName;
                    HomeFragment.this.apkPath = VirtualCore.get().getInstalledAppInfo(HomeFragment.this.packageName, 0).apkPath;
                    if(!settings.edit().putString(PACKAGE_PREFERENCE_KEY, HomeFragment.this.packageName).commit()){
                        resultString = "SharedPreferences commit failed";
                    } else {
                        resultString = "Success";
                    }
                } else {
                    resultString = result.error;
                }
                progressDialog.dismiss();
                HomeFragment.this.view.post(new Runnable() {
                    @Override
                    public void run() {
                        clientUpdate();
                        Toast.makeText(context, resultString, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }.start();
    }

    @Override
    public void OnApplicationChooseDialogResult(String packageName, String apkPath) {
        clientInstall(apkPath);
        dialog.hide();
    }
}
