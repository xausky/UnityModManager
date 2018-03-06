package io.github.xausky.bh3modmanager.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.lody.virtual.client.core.InstallStrategy;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.remote.InstallResult;

import io.github.xausky.bh3modmanager.MainActivity;
import io.github.xausky.bh3modmanager.MainApplication;
import io.github.xausky.bh3modmanager.R;
import io.github.xausky.bh3modmanager.adapter.ClientsAdapter;
import ru.bartwell.exfilepicker.ExFilePicker;
import ru.bartwell.exfilepicker.data.ExFilePickerResult;

/**
 * Created by xausky on 18-3-3.
 */

public class HomeFragment extends BaseFragment implements View.OnClickListener {
    private static final String PACKAGE_PREFERENCE_KEY = "PACKAGE_PREFERENCE_KEY";
    private static final String BH3_CLIENT_PACKAGE_REGEX = "^com\\.miHoYo\\..*$";
    private static final int CLIENT_FILE_PICKER_RESULT = 8848;
    public String packageName;
    private String apkPath;
    private View view;
    private TextView summary;
    private TextView clientState;
    private CardView clientStateCardView;
    private Context context;
    private SharedPreferences settings;
    private AlertDialog clientDialog;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        context = inflater.getContext();
        settings = context.getSharedPreferences(SettingFragment.SETTINGS_PREFERENCE_NAME, Context.MODE_PRIVATE);
        packageName = settings.getString(PACKAGE_PREFERENCE_KEY, null);
        view = inflater.inflate(R.layout.home_fragment, container, false);
        summary = view.findViewById(R.id.home_summary);
        clientState = view.findViewById(R.id.home_client_state);
        clientStateCardView = view.findViewById(R.id.home_client_state_card_view);
        clientStateCardView.setOnClickListener(this);
        try {
            PackageInfo packageInfo = VirtualCore.get().getPackageManager().getPackageInfo(packageName, 0);
            String versionName = packageInfo.versionName;
            clientState.setText(String.format(getText(R.string.home_client_installed).toString(), versionName));
            clientState.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(context,R.drawable.ic_check),null, null, null);
        } catch (PackageManager.NameNotFoundException e) {
            clientState.setText(getText(R.string.home_client_uninstalled));
            clientState.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(context,R.drawable.ic_clear),null, null, null);
        }
        StringBuilder builder = new StringBuilder();
        try {
            String versionName = inflater.getContext().getPackageManager().getPackageInfo(inflater.getContext().getPackageName(), 0).versionName;
            builder.append("Software Version Name: ");
            builder.append(versionName);
            builder.append('\n');
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        builder.append("Imported Mods Count: ");
        builder.append(0);
        builder.append('\n');
        builder.append("Enabled Mods Count: ");
        builder.append(0);
        builder.append('\n');
        builder.append("Virtual Core Installed App Count: ");
        builder.append(VirtualCore.get().getInstalledAppCount());
        builder.append('\n');
        summary.setText(builder.toString());

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.choose_client_dialog, null);
        ListView listView = dialogView.findViewById(R.id.choose_client_dialog_clients);
        listView.setAdapter(new ClientsAdapter(context, BH3_CLIENT_PACKAGE_REGEX));
        dialogBuilder.setTitle("请选择客户端");
        dialogBuilder.setView(dialogView);
        dialogBuilder.setPositiveButton("浏览", null);
        clientDialog = dialogBuilder.create();
        clientDialog.show();
        clientDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ExFilePicker filePicker = new ExFilePicker();
                filePicker.setShowOnlyExtensions("apk");
                filePicker.setCanChooseOnlyOneItem(true);
                filePicker.start(HomeFragment.this, CLIENT_FILE_PICKER_RESULT);
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ApplicationInfo info = (ApplicationInfo) parent.getAdapter().getItem(position);
                clientInstall(null, info.sourceDir);
                clientDialog.hide();
            }
        });
        clientDialog.hide();
        return view;
    }

    @Override
    public void onClick(View v) {
        clientDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CLIENT_FILE_PICKER_RESULT){
            ExFilePickerResult result = ExFilePickerResult.getFromIntent(data);
            if(result!=null){
                clientInstall(null, result.getPath() + result.getNames().get(0));
                clientDialog.hide();
            }
        }
    }

    @Override
    public void onDestroyView() {
        if(clientDialog != null){
            clientDialog.dismiss();
        }
        super.onDestroyView();
    }

    private void clientInstall(String packageName, final String apkPath){
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
                        Snackbar.make(HomeFragment.this.view, resultString, Snackbar.LENGTH_LONG).show();
                    }
                });
            }
        }.start();
    }
}
