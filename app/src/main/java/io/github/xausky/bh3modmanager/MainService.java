package io.github.xausky.bh3modmanager;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Environment;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.remote.InstallResult;
import com.lody.virtual.remote.InstalledAppInfo;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by xausky on 2018/2/1.
 */

public class MainService implements CompoundButton.OnCheckedChangeListener {
    private static final String BH3_PACKAGE_NAME = "com.miHoYo.enterprise.NGHSoD";
    private MainActivity context = null;
    private ModsAdapter adapter = null;
    private String appPath = null;
    private String virtualAppPath = null;
    private Button launch = null;
    private boolean isLaunch = true;
    private String storagePath = null;
    private SharedPreferences preferences;

    public MainService(MainActivity context) {
        this.context = context;
        preferences = context.getSharedPreferences("mods_install_state", MODE_PRIVATE);
    }

    public ModsAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(ModsAdapter adapter) {
        this.adapter = adapter;
        storagePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/BH3Mods";
        File storage = new File(storagePath);
        if(!storage.exists()){
            if(!storage.mkdir()){
                Toast.makeText(context, "无法创建模组目录，程序退出。", Toast.LENGTH_LONG).show();
                context.finish();
            }
        }
        File[] mods = storage.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(".zip");
            }
        });
        for(File file: mods){
            String name = file.getName().substring(0,file.getName().length() - 4 );
            adapter.getMods().add(new Mod(name, preferences.getBoolean(name, false), preferences.getString(name + ":password", null)));
        }
        adapter.setListener(this);
    }

    public Button getLaunch() {
        return launch;
    }

    public void setLaunch(Button launch) {
        this.launch = launch;
    }

    private String getAppPath(){
        List<ApplicationInfo> applications = context.getPackageManager().getInstalledApplications(0);
        for (ApplicationInfo info : applications) {
            if (BH3_PACKAGE_NAME.equals(info.packageName)) {
                return info.sourceDir;
            }
        }
        return null;
    }

    public void start(){
        appPath = getAppPath();
        if (appPath == null){
            Toast.makeText(context, "未安装崩坏3rd客户端，程序退出。", Toast.LENGTH_LONG).show();
            context.finish();
        }
        InstalledAppInfo virtualAppInfo = VirtualCore.get().getInstalledAppInfo(BH3_PACKAGE_NAME, 0);
        if(virtualAppInfo != null){
            virtualAppPath = virtualAppInfo.apkPath;
            Log.d("BH3ModManager", "V APP Installed: " + virtualAppInfo.apkPath);
        }else {
            Toast.makeText(context, "未安装底包，正在安装。", Toast.LENGTH_LONG).show();
            final ProgressDialog dialog = ProgressDialog.show(context, "请稍等", "正在安装崩坏3到虚拟环境，该过程大概需要2到3分钟，只需要运行一次。", false, false);
            Thread installThread = new Thread(){
                @Override
                public void run() {
                    InstallResult result = VirtualCore.get().installPackage(appPath, 0);
                    if(result.isSuccess){
                        InstalledAppInfo virtualAppInfo = VirtualCore.get().getInstalledAppInfo(BH3_PACKAGE_NAME, 0);
                        virtualAppPath = virtualAppInfo.apkPath;
                    }
                    Log.d("BH3ModManager", "Install APP: " + result.isSuccess + ", Error: " + result.error);
                    dialog.dismiss();
                }
            };
            installThread.start();
        }
    }

    public void launch(){
        if(isLaunch){
            final ProgressDialog dialog = ProgressDialog.show(context, "请稍等", "正在启动崩坏3", false, false);
            final Intent intent = VirtualCore.get().getLaunchIntent(BH3_PACKAGE_NAME, 0);
            VirtualCore.get().setUiCallback(intent, new VirtualCore.UiCallback(){
                @Override
                public void onAppOpened(String s, int i) throws RemoteException {
                    dialog.dismiss();
                }
            });
            Thread startThread = new Thread() {
                @Override
                public void run() {
                    VActivityManager.get().startActivity(intent, 0);
                }
            };
            startThread.start();
        }else {
            final ProgressDialog dialog = ProgressDialog.show(context, "请稍等", "正在安装模组补丁到崩坏3", false, false);
            Thread installThread = new Thread(){
                @Override
                public void run() {
                    String fusionDirPath = context.getCacheDir().getAbsolutePath() + "/ModsFusion";
                    String backupDirPath = context.getFilesDir().getAbsolutePath() + "/ModsBackup";
                    File fusionDir = new File(fusionDirPath);
                    deleteFile(fusionDir);
                    fusionDir.mkdir();
                    File backupDir = new File(backupDirPath);
                    if(!backupDir.exists()){
                        backupDir.mkdir();
                    }
                    //开始解压模组文件
                    final SharedPreferences.Editor editor = preferences.edit();
                    for(final Mod mod : adapter.getMods()){
                        editor.putBoolean(mod.name, mod.enable);
                        editor.putString(mod.name + ":password", mod.password);
                        if(mod.enable){
                            int result = ZipUtils.unzipFile(storagePath + "/" + mod.name + ".zip", fusionDirPath, mod.password);
                            switch (result){
                                case -1:
                                    launch.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(context,"解压模组错误："+ mod.name , Toast.LENGTH_LONG).show();
                                        }
                                    });
                                    dialog.dismiss();
                                    return;
                                case -2:
                                    launch.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            final EditText editText = new EditText(context);
                                            AlertDialog.Builder inputDialog =
                                                    new AlertDialog.Builder(context);
                                            inputDialog.setTitle("请输入模组密码:" + mod.name).setView(editText);
                                            inputDialog.setPositiveButton("确定",
                                                    new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            mod.password = editText.getText().toString();
                                                            dialog.dismiss();
                                                        }
                                                    }).show();
                                        }
                                    });
                                    dialog.dismiss();
                                    return;
                                case -3:
                                    launch.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(context,"模组文件冲突："+ mod.name , Toast.LENGTH_LONG).show();
                                        }
                                    });
                                    dialog.dismiss();
                                    return;
                            }
                        }
                    }
                    editor.commit();
                    if(ZipUtils.patchZip(backupDirPath, fusionDirPath, "assets/bin/Data", virtualAppPath) != 0){
                        launch.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context,"安装模组失败", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        launch.post(new Runnable() {
                            @Override
                            public void run() {
                                launch.setText(context.getText(R.string.btn_start_content));
                            }
                        });
                        isLaunch = true;
                    }
                    dialog.dismiss();
                }
            };
            installThread.start();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        launch.setText(context.getText(R.string.btn_apply_content));
        isLaunch = false;
    }

    private static String endSubstring(String str, int len){
        if(str.length() < len){
            return "";
        }
        return str.substring(str.length() - 32, str.length());
    }

    /**
     * 通过递归调用删除一个文件夹及下面的所有文件
     * @param file
     */
    private static void deleteFile(File file){
        if(!file.exists()){
            return;
        }
        if(file.isFile()){//表示该文件不是文件夹
            if(!file.delete()){
                Log.d("BH3ModManager", "delete failed: " + file.getAbsolutePath());
            }
        }else{
            //首先得到当前的路径
            File[] childFiles = file.listFiles();
            for(File childFile : childFiles){
                deleteFile(childFile);
            }
            if(!file.delete()){
                Log.d("BH3ModManager", "delete failed: " + file.getAbsolutePath());
            }
        }
    }
}
