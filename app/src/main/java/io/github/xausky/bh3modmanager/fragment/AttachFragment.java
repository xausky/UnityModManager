package io.github.xausky.bh3modmanager.fragment;

import android.content.Context;
import android.content.Intent;
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
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.remote.InstallResult;

import io.github.xausky.bh3modmanager.MainApplication;
import io.github.xausky.bh3modmanager.R;
import io.github.xausky.bh3modmanager.adapter.AttachesAdapter;
import io.github.xausky.bh3modmanager.dialog.ApplicationChooseDialog;

/**
 * Created by xausky on 18-3-3.
 */

public class AttachFragment extends BaseFragment implements ApplicationChooseDialog.OnApplicationChooseDialogResultListener, AdapterView.OnItemClickListener{
    private static final String ALL_APPLICATION_PACKAGE_REGEX = "^.*$";
    private Context context;
    private ApplicationChooseDialog dialog;
    private View view;
    private ListView attaches;
    private AttachesAdapter adapter;
    private HomeFragment homeFragment;
    private VirtualCore va;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        this.context = inflater.getContext();
        this.view = inflater.inflate(R.layout.attach_fragment, container, false);
        this.dialog = new ApplicationChooseDialog(context, this, ALL_APPLICATION_PACKAGE_REGEX);
        this.dialog.setListener(this);
        this.attaches = view.findViewById(R.id.attach_list);
        this.homeFragment = (HomeFragment) BaseFragment.fragment(R.id.nav_home);
        this.va = VirtualCore.get();
        this.adapter = new AttachesAdapter(context, va, homeFragment.packageName);
        this.attaches.setAdapter(adapter);
        this.attaches.setOnItemClickListener(this);
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
                        adapter.update(homeFragment.packageName);
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
        AttachFragment.this.dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        dialog.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void OnApplicationChooseDialogResult(String packageName, String apkPath) {
        appInstall(apkPath);
        dialog.hide();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ApplicationInfo info = (ApplicationInfo) parent.getAdapter().getItem(position);
        Intent intent = VirtualCore.get().getLaunchIntent(info.packageName, 0);
        VActivityManager.get().startActivity(intent, 0);
    }
}
