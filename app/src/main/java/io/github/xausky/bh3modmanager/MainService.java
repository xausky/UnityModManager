package io.github.xausky.bh3modmanager;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Environment;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.remote.InstallResult;
import com.lody.virtual.remote.InstalledAppInfo;
import com.topjohnwu.superuser.Shell;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by xausky on 2018/2/1.
 */

public class MainService implements CompoundButton.OnCheckedChangeListener, AdapterView.OnItemLongClickListener {
    private static final String PACKAGE_NAME_PREFERENCES_KEY = "__PACKAGE_NAME_PREFERENCES_KEY";
    private static final String ROOT_MODE_PREFERENCES_KEY = "__ROOT_MODE_PREFERENCES_KEY";
    private static final String FORCE_INSTALL_PREFERENCES_KEY = "__FORCE_INSTALL_PREFERENCES_KEY";
    public static final String LOG_TAG = "BH3ModManager";
    private static final int COPY_BUFFER_SIZE = 10240;
    private String packageName = null;
    private MainActivity context = null;
    private ModsAdapter adapter = null;
    private String appPath = null;
    private Button launch = null;
    private boolean isLaunch = true;
    private boolean root = false;
    private boolean force = false;
    private String storagePath = null;
    private SharedPreferences preferences;
    private String title = null;

    public MainService(MainActivity context) {
        this.context = context;
        preferences = context.getSharedPreferences("default", MODE_PRIVATE);
        packageName = preferences.getString(PACKAGE_NAME_PREFERENCES_KEY, null);
        root = preferences.getBoolean(ROOT_MODE_PREFERENCES_KEY, false);
        force = preferences.getBoolean(FORCE_INSTALL_PREFERENCES_KEY, false);
        context.setForce(force);
        title = context.getTitle().toString();
        if(root){
            context.setTitle(title + "[Root Mode]");
        } else {
            context.setTitle(title + "[Virtual Mode]");
        }
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
            String name = file.getName();
            adapter.getMods().add(new Mod(name, preferences.getBoolean(name, false), preferences.getString(name + ":password", null)));
        }
        adapter.setCheckedChangeListener(this);
        adapter.setItemLongClickListener(this);
    }

    public Button getLaunch() {
        return launch;
    }

    public void setLaunch(Button launch) {
        this.launch = launch;
    }

    public void chooseInstall(boolean cancelable){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.choose_client_dialog, null);
        ListView listView = view.findViewById(R.id.choose_client_dialog_clients);
        listView.setAdapter(new ClientsAdapter(context));
        builder.setTitle("请选择客户端");
        builder.setView(view);
        builder.setCancelable(cancelable);
        if(!root){
            builder.setPositiveButton("浏览", null);
        }
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                context.chooseFile(dialog, MainActivity.CHOOSE_APK_REQUEST_CODE);
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ApplicationInfo info = (ApplicationInfo) parent.getAdapter().getItem(position);
                install(info.sourceDir, info.packageName);
                try {
                    dialog.dismiss();
                }catch (Exception e){

                }
            }
        });
    }

    public void install(final String path, final String name){
        final ProgressDialog dialog = ProgressDialog.show(context, "请稍等", "正在安装崩坏3到虚拟环境，该过程大概需要2到3分钟", false, false);
        Thread installThread = new Thread(){
            @Override
            public void run() {
                if(!root){
                    final InstallResult result = VirtualCore.get().installPackage(path, 0);
                    if(result.isSuccess) {
                        InstalledAppInfo virtualAppInfo = VirtualCore.get().getInstalledAppInfo(result.packageName, 0);
                        appPath = virtualAppInfo.apkPath;
                        if (packageName != null && !packageName.equals(result.packageName)) {
                            VirtualCore.get().uninstallPackage(packageName);
                        }
                        packageName = result.packageName;
                    } else {
                        launch.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "安装失败:" + result.error, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                    Log.d("BH3ModManager", "Install APP: " + result.isSuccess + ", Error: " + result.error);
                } else {
                    packageName = name;
                    appPath = path;
                }
                preferences.edit().putString(PACKAGE_NAME_PREFERENCES_KEY, packageName).apply();
                String backupDirPath = context.getFilesDir().getAbsolutePath() + "/ModsBackup";
                deleteFile(new File(backupDirPath));
                SharedPreferences.Editor editor = preferences.edit();
                for(Mod mod: adapter.getMods()){
                    mod.enable = false;
                    editor.putBoolean(mod.name, false);
                }
                editor.apply();
                launch.post(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
                try {
                    dialog.dismiss();
                }catch (Exception e){

                }
            }
        };
        installThread.start();
    }

    public void start(){
        if(packageName != null) {
            if(root){
                try {
                    ApplicationInfo info = context.getPackageManager().getApplicationInfo(packageName, 0);
                    appPath = info.sourceDir;
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                InstalledAppInfo info = VirtualCore.get().getInstalledAppInfo(packageName, 0);
                if(info != null){
                    appPath = info.apkPath;
                }
            }
            if(appPath != null){
                Log.d("BH3ModManager", "app path: " + appPath);
            }else {
                chooseInstall(false);
            }
        } else {
            final AlertDialog.Builder builder=new AlertDialog.Builder(context);
            builder.setTitle("是否使用Root模式");
            builder.setMessage("Root模式具有更强的稳定性与兼容性，但是需要赋予Root权限，本程序保证不会滥用此权限。");
            builder.setCancelable(true);
            builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Shell.setFlags(Shell.FLAG_REDIRECT_STDERR);
                    root = Shell.rootAccess();
                    Log.d(LOG_TAG, "root:" + root);
                    preferences.edit().putBoolean(ROOT_MODE_PREFERENCES_KEY,root).apply();
                    chooseInstall(false);
                    try {
                        dialog.dismiss();
                    }catch (Exception e){

                    }
                    if(root){
                        context.setTitle(title + "[Root Mode]");
                    } else {
                        context.setTitle(title + "[Virtual Mode]");
                    }
                }
            });
            builder.setNegativeButton("否", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    root = false;
                    preferences.edit().putBoolean(ROOT_MODE_PREFERENCES_KEY,root).apply();
                    chooseInstall(false);
                    try {
                        dialog.dismiss();
                    }catch (Exception e){

                    }
                    if(root){
                        context.setTitle(title + "[Root Mode]");
                    } else {
                        context.setTitle(title + "[Virtual Mode]");
                    }
                }
            });
            builder.show();
        }
    }

    public void launch(){
        if(isLaunch){
            if(!root){
                final ProgressDialog dialog = ProgressDialog.show(context, "请稍等", "正在启动崩坏3", false, false);
                final Intent intent = VirtualCore.get().getLaunchIntent(packageName, 0);
                VirtualCore.get().setUiCallback(intent, new VirtualCore.UiCallback(){
                    @Override
                    public void onAppOpened(String s, int i) throws RemoteException {
                        try {
                            dialog.dismiss();
                        }catch (Exception e){

                        }
                    }
                });
                Thread startThread = new Thread() {
                    @Override
                    public void run() {
                        VActivityManager.get().startActivity(intent, 0);
                    }
                };
                startThread.start();
            } else {
                startApplicationWithPackageName(packageName);
            }
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
                            int result = ZipUtils.unzipFile(storagePath + "/" + mod.name, fusionDirPath, mod.password, force);
                            switch (result){
                                case -1:
                                    launch.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(context,"解压模组错误："+ mod.name , Toast.LENGTH_LONG).show();
                                        }
                                    });
                                    try {
                                        dialog.dismiss();
                                    }catch (Exception e){

                                    }
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
                                                            try {
                                                                dialog.dismiss();
                                                            }catch (Exception e){

                                                            }
                                                        }
                                                    }).show();
                                        }
                                    });
                                    try {
                                        dialog.dismiss();
                                    }catch (Exception e){

                                    }
                                    return;
                                case -3:
                                    launch.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(context,"模组文件冲突："+ mod.name , Toast.LENGTH_LONG).show();
                                        }
                                    });
                                    try {
                                        dialog.dismiss();
                                    }catch (Exception e){

                                    }
                                    return;
                            }
                        }
                    }
                    editor.apply();
                    if(ZipUtils.patchZip(root, context, backupDirPath, fusionDirPath, "assets/bin/Data", appPath) != 0){
                        launch.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context,"安装模组失败", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        if(root){
                            Shell.setFlags(Shell.FLAG_REDIRECT_STDERR);
                            Shell.Sync.sh("chown system:system " + appPath);
                            Shell.Sync.sh("chmod 644 " + appPath);
                        }
                        launch.post(new Runnable() {
                            @Override
                            public void run() {
                                launch.setText(context.getText(R.string.btn_start_content));
                            }
                        });
                        isLaunch = true;
                    }
                    try {
                        dialog.dismiss();
                    }catch (Exception e){

                    }
                }
            };
            installThread.start();
        }
    }

    public void importMod(final String path){
        final ProgressDialog dialog = ProgressDialog.show(context, "请稍等", "正在导入模组文件到管理器", false, false);
        Thread thread = new Thread(){
            @Override
            public void run() {
                byte[] buffer = new byte[COPY_BUFFER_SIZE];
                int len = 0;
                final File inputFile = new File(path);
                File outputFile = new File(storagePath + "/" + inputFile.getName());
                if(!outputFile.exists()) {
                    try(FileInputStream inputStream = new FileInputStream(inputFile);
                        FileOutputStream outputStream = new FileOutputStream(storagePath + "/" + inputFile.getName())) {
                        while ( (len = inputStream.read(buffer)) != -1){
                            outputStream.write(buffer, 0, len);
                        }
                        adapter.getMods().add(new Mod(inputFile.getName(), false, null));
                        launch.post(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                            }
                        });
                    } catch (IOException e) {
                        Log.d(MainService.LOG_TAG, "import mod exception", e);
                    }
                } else {
                    launch.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context,"模组文件已存在："+ inputFile.getName() , Toast.LENGTH_LONG).show();
                        }
                    });
                }
                try {
                    dialog.dismiss();
                }catch (Exception e){

                }
            }
        };
        thread.start();
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

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
        final List<Mod> mods = adapter.getMods();
        final Mod mod = mods.get(position);
        if(mod.enable){
            Toast.makeText(context,"无法删除已启用的模组，请先禁用：" + mod.name , Toast.LENGTH_SHORT).show();
            return true;
        }
        final AlertDialog.Builder builder=new AlertDialog.Builder(context);
        builder.setTitle("确认");
        builder.setMessage("删除模组:" + mod.name);
        builder.setCancelable(true);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new File(storagePath + "/" + mod.name).delete();
                mods.remove(position);
                adapter.notifyDataSetChanged();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
        return true;
    }

    private void startApplicationWithPackageName(String name) {

        // 通过包名获取此APP详细信息，包括Activities、services、versioncode、name等等
        PackageInfo packageinfo = null;
        try {
            packageinfo = context.getPackageManager().getPackageInfo(name, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageinfo == null) {
            return;
        }

        // 创建一个类别为CATEGORY_LAUNCHER的该包名的Intent
        Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
        resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        resolveIntent.setPackage(packageinfo.packageName);

        // 通过getPackageManager()的queryIntentActivities方法遍历
        List<ResolveInfo> resolveinfoList = context.getPackageManager()
                .queryIntentActivities(resolveIntent, 0);

        ResolveInfo resolveinfo = resolveinfoList.iterator().next();
        if (resolveinfo != null) {
            // packagename = 参数packname
            String packageName = resolveinfo.activityInfo.packageName;
            // 这个就是我们要找的该APP的LAUNCHER的Activity[组织形式：packagename.mainActivityname]
            String className = resolveinfo.activityInfo.name;
            // LAUNCHER Intent
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);

            // 设置ComponentName参数1:packagename参数2:MainActivity路径
            ComponentName cn = new ComponentName(packageName, className);

            intent.setComponent(cn);
            context.startActivity(intent);
        }
    }

    public void forceChange(boolean force){
        this.force = force;
        preferences.edit().putBoolean(FORCE_INSTALL_PREFERENCES_KEY, force).apply();
    }
}
