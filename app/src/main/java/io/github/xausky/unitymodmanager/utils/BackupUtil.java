package io.github.xausky.unitymodmanager.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.topjohnwu.superuser.Shell;

import org.json.JSONObject;

import static android.content.Context.MODE_PRIVATE;

public class BackupUtil {

    public static byte[] backupKuroGame(Context context) {
        String accountsDatabaseFile = context.getDatabasePath("zz_sdk_db").getAbsolutePath();
        String deviceIdFile = context.getFilesDir().getAbsolutePath() + "/shared_prefs/devicesyn.xml";
        try {
            unprotectFilesWithRoot(accountsDatabaseFile + "*", deviceIdFile);
            JSONObject root = new JSONObject();
            StringBuilder builder = new StringBuilder();
            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(accountsDatabaseFile, null, SQLiteDatabase.OPEN_READONLY);
                 Cursor cursor = db.rawQuery("SELECT user_id,login_id,login_name,password,auto_login,last_login_time,login_type,local_login_count,user_type FROM sdkuser;", null)) {
                while (cursor.moveToNext()) {
                    for (int i = 0; i < cursor.getColumnCount(); i++) {
                        builder.append(cursor.getString(i));
                        if (i + 1 < cursor.getColumnCount()) {
                            builder.append(',');
                        }
                    }
                    if (!cursor.isLast()) {
                        builder.append('\n');
                    }
                }
            }
            root.put("accounts", builder.toString());
            SharedPreferences sharedPreferences = context.getSharedPreferences("devicesyn", MODE_PRIVATE);
            String deviceId = sharedPreferences.getString("device_id", null);
            root.put("device_id", deviceId);
            return root.toString().getBytes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            protectFilesWithRoot(accountsDatabaseFile + "*", deviceIdFile);
        }
    }

    public static void restoreKuroGame(Context context, byte[] data) {
        String accountsDatabaseFile = context.getDatabasePath("zz_sdk_db").getAbsolutePath();
        String deviceIdFile = context.getFilesDir().getAbsolutePath() + "/shared_prefs/devicesyn.xml";
        try {
            unprotectFilesWithRoot(accountsDatabaseFile + "*", deviceIdFile);
            JSONObject root = new JSONObject(new String(data));
            String accounts = root.getString("accounts");
            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(accountsDatabaseFile, null, SQLiteDatabase.OPEN_READWRITE)) {
                for (String account : accounts.split("\n")){
                    db.execSQL("INSERT INTO sdkuser(user_id,login_id,login_name,password,auto_login,last_login_time,login_type,local_login_count,user_type) values (?, ?, ?, ?, ?, ?, ?, ?, ?)", account.split(","));
                }
            }
            SharedPreferences sharedPreferences = context.getSharedPreferences("devicesyn", MODE_PRIVATE);
            sharedPreferences.edit().putString("device_id", root.getString("device_id")).commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            protectFilesWithRoot(accountsDatabaseFile + "*", deviceIdFile);
        }
    }

    private static void unprotectFilesWithRoot(String ...files){
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= files.length; i++){
            builder.append(files[i - 1]);
            if (i < files.length){
                builder.append(" ");
            }
        }
        Shell.su("setenforce 0", "chmod 666 " + builder.toString()).exec();
    }

    private static void protectFilesWithRoot(String ...files){
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= files.length; i++){
            builder.append(files[i - 1]);
            if (i < files.length){
                builder.append(" ");
            }
        }
        Shell.su("setenforce 1", "chmod 600 " + builder.toString()).exec();
    }
}
