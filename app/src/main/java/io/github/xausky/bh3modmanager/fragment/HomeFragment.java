package io.github.xausky.bh3modmanager.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lody.virtual.client.core.InstallStrategy;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.remote.InstallResult;

import io.github.xausky.bh3modmanager.MainApplication;
import io.github.xausky.bh3modmanager.R;
import io.github.xausky.bh3modmanager.dialog.ApplicationChooseDialog;

/**
 * Created by xausky on 18-3-3.
 */

public class HomeFragment extends BaseFragment implements View.OnClickListener, ApplicationChooseDialog.OnApplicationChooseDialogResultListener{
    private static final String PACKAGE_PREFERENCE_KEY = "PACKAGE_PREFERENCE_KEY";
    private static final String BH3_CLIENT_PACKAGE_REGEX = "^com\\.miHoYo\\..*$";
    public String packageName;
    private String apkPath;
    private View view;
    private TextView summary;
    private TextView clientState;
    private CardView clientStateCardView;
    private Context context;
    private SharedPreferences settings;
    private ApplicationChooseDialog dialog;
    private VirtualCore va;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        context = inflater.getContext();
        settings = context.getSharedPreferences(SettingFragment.SETTINGS_PREFERENCE_NAME, Context.MODE_PRIVATE);
        packageName = settings.getString(PACKAGE_PREFERENCE_KEY, null);
        view = inflater.inflate(R.layout.home_fragment, container, false);
        summary = view.findViewById(R.id.home_summary);
        va = VirtualCore.get();
        clientState = view.findViewById(R.id.home_client_state);
        clientStateCardView = view.findViewById(R.id.home_client_state_card_view);
        clientStateCardView.setOnClickListener(this);
        dialog = new ApplicationChooseDialog(context, this, BH3_CLIENT_PACKAGE_REGEX);
        dialog.setListener(this);
        clientUpdate();
        return view;
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
        String versionName = "unknown";
        try {
            versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        String summaryString = String.format(getString(R.string.home_summary_context),
                versionName,
                0,
                0,
                0,
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
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View bottomSheetDialogView = LayoutInflater.from(context).inflate(R.layout.progress_dialog, null);
        bottomSheetDialog.setContentView(bottomSheetDialogView);
        bottomSheetDialog.setCancelable(false);
        bottomSheetDialog.show();
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
                bottomSheetDialog.dismiss();
                HomeFragment.this.view.post(new Runnable() {
                    @Override
                    public void run() {
                        clientUpdate();
                        Snackbar.make(HomeFragment.this.view, resultString, Snackbar.LENGTH_LONG).show();
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
