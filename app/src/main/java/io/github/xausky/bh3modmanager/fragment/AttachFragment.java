package io.github.xausky.bh3modmanager.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.lody.virtual.client.core.InstallStrategy;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.remote.InstallResult;

import io.github.xausky.bh3modmanager.MainApplication;
import io.github.xausky.bh3modmanager.R;
import io.github.xausky.bh3modmanager.adapter.ClientsAdapter;
import ru.bartwell.exfilepicker.ExFilePicker;

/**
 * Created by xausky on 18-3-3.
 */

public class AttachFragment extends BaseFragment {
    private static final String ALL_CLIENT_PACKAGE_REGEX = "^.*$";
    private static final int APP_FILE_PICKER_RESULT = 8849;
    private Context context;
    private AlertDialog appDialog;
    private View view;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        this.context = inflater.getContext();
        this.view = inflater.inflate(R.layout.attach_fragment, container, false);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.choose_client_dialog, null);
        ListView listView = dialogView.findViewById(R.id.choose_client_dialog_clients);
        listView.setAdapter(new ClientsAdapter(context, ALL_CLIENT_PACKAGE_REGEX));
        dialogBuilder.setTitle("请选择客户端");
        dialogBuilder.setView(dialogView);
        dialogBuilder.setPositiveButton("浏览", null);
        appDialog = dialogBuilder.create();
        appDialog.show();
        appDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ExFilePicker filePicker = new ExFilePicker();
                filePicker.setShowOnlyExtensions("apk");
                filePicker.setCanChooseOnlyOneItem(true);
                filePicker.start(AttachFragment.this, APP_FILE_PICKER_RESULT);
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ApplicationInfo info = (ApplicationInfo) parent.getAdapter().getItem(position);
                appInstall(info.sourceDir);
                appDialog.hide();
            }
        });
        appDialog.hide();
        return this.view;
    }

    private void appInstall(final String apkPath){
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
                    resultString = "Success";
                } else {
                    resultString = result.error;
                }
                bottomSheetDialog.dismiss();
                AttachFragment.this.view.post(new Runnable() {
                    @Override
                    public void run() {
                        Snackbar.make(AttachFragment.this.view, resultString, Snackbar.LENGTH_LONG).show();
                    }
                });
            }
        }.start();
    }

    @Override
    public int actionButtonVisibility() {
        return View.VISIBLE;
    }

    @Override
    public void OnActionButtonClick() {
        AttachFragment.this.appDialog.show();
    }
}
