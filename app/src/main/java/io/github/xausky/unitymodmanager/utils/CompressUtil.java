package io.github.xausky.unitymodmanager.utils;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.topjohnwu.superuser.Shell;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;

public class CompressUtil {
    public static byte[] compress(byte[] data) {
        byte[] output = new byte[0];
        Deflater compresser = new Deflater();
        compresser.reset();
        compresser.setInput(data);
        compresser.finish();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
        try {
            byte[] buf = new byte[1024];
            while (!compresser.finished()) {
                int i = compresser.deflate(buf);
                bos.write(buf, 0, i);
            }
            output = bos.toByteArray();
        } catch (Exception e) {
            output = data;
            e.printStackTrace();
        } finally {
            try {
                bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        compresser.end();
        return output;
    }

    //解压缩 字节数组
    public static byte[] decompress(byte[] data) {
        byte[] output = new byte[0];

        Inflater decompresser = new Inflater();
        decompresser.reset();
        decompresser.setInput(data);

        ByteArrayOutputStream o = new ByteArrayOutputStream(data.length);
        try {
            byte[] buf = new byte[1024];
            while (!decompresser.finished()) {
                int i = decompresser.inflate(buf);
                o.write(buf, 0, i);
            }
            output = o.toByteArray();
        } catch (Exception e) {
            output = data;
            e.printStackTrace();
        } finally {
            try {
                o.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        decompresser.end();
        return output;
    }

    public static byte[] backupKuroGame(String packageName) {
        String accountsDatabaseFile = "/data/data/" + packageName + "/databases/zz_sdk_db";
        String deviceIdFile = "/data/data/" + packageName + "/shared_prefs/devicesyn.xml";
        try {
            unprotectFilesWithRoot(accountsDatabaseFile, deviceIdFile);
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
            root.put("device", FileUtils.readFileToString(new File(deviceIdFile)));
            return root.toString().getBytes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            protectFilesWithRoot(accountsDatabaseFile, deviceIdFile);
        }
    }

    public static void restoreKuroGame(String packageName, byte[] data) {
        String accountsDatabaseFile = "/data/data/" + packageName + "/databases/zz_sdk_db";
        String deviceIdFile = "/data/data/" + packageName + "/shared_prefs/devicesyn.xml";
        try {
            unprotectFilesWithRoot(accountsDatabaseFile, deviceIdFile);
            JSONObject root = new JSONObject(new String(data));
            String accounts = root.getString("accounts");
            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(accountsDatabaseFile, null, SQLiteDatabase.OPEN_READWRITE)) {
                for (String account : accounts.split("\n")){
                    db.execSQL("INSERT INTO sdkuser(user_id,login_id,login_name,password,auto_login,last_login_time,login_type,local_login_count,user_type) values (?, ?, ?, ?, ?, ?, ?, ?, ?)", account.split(","));
                }
            }
            FileUtils.writeStringToFile(new File(deviceIdFile), root.getString("device"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            protectFilesWithRoot(accountsDatabaseFile, deviceIdFile);
        }
    }

    public static void unprotectFilesWithRoot(String ...files){
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= files.length; i++){
            builder.append(files[i - 1]);
            if (i < files.length){
                builder.append(" ");
            }
        }
        Shell.su("setenforce 0", "chmod 666 " + builder.toString()).exec();
    }

    public static void protectFilesWithRoot(String ...files){
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= files.length; i++){
            builder.append(files[i - 1]);
            if (i < files.length){
                builder.append(" ");
            }
        }
        Shell.su("setenforce 1", "chmod 644 " + builder.toString()).exec();
    }
}
