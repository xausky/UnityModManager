package io.github.xausky.unitymodmanager.utils;

import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import io.github.xausky.unitymodmanager.MainApplication;

/**
 * Created by xausky on 18-2-9.
 */

public class ModUtils {
    public static final int RESULT_STATE_OK = 0;
    public static final int RESULT_STATE_INTERNAL_ERROR = -1;
    public static final int RESULT_STATE_PASSWORD_ERROR = -2;
    public static Map<String, String> map;
    public static Set<String> supportImageType = new TreeSet<>();

    static {
        supportImageType.add(".jpg");
        supportImageType.add(".png");
        supportImageType.add(".bmp");
    }

    public static int Standardization(File input, File output){
        int result = 0;
        if(!input.isDirectory()){
            return RESULT_STATE_INTERNAL_ERROR;
        }
        File[] files = input.listFiles();
        for(File file : files){
            String path = null;
            String name = file.getName();
            path = map.get(input.getName() + "/" + name);
            if(path != null){
                try {
                    if(file.isDirectory()){
                        FileUtils.copyDirectory(file, new File(output + "/" + path));
                    } else {
                        FileUtils.copyFile(file, new File(output + "/" + path));
                    }
                    result++;
                } catch (IOException e) {
                    e.printStackTrace();
                    return RESULT_STATE_INTERNAL_ERROR;
                }
            } else if(file.isDirectory()){
                int r = Standardization(new File(input + "/" + name), output);
                if(r == RESULT_STATE_INTERNAL_ERROR){
                    return RESULT_STATE_INTERNAL_ERROR;
                }
                result += r;
            } else if(name.length() > 4 && supportImageType.contains(name.substring(name.length() - 4))){
                try {
                    FileUtils.copyFile(file, new File(output + "/images/" + System.currentTimeMillis() + "-" + name));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if(name.equals("info.json")){
                try {
                    FileUtils.copyFile(file, new File(output + "/info.json"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    public static String checkSum(String path) {
        try {
            MessageDigest localMessageDigest = MessageDigest.getInstance("MD5");
            FileInputStream localFileInputStream = new FileInputStream(path);
            long lenght = new File(path).length();
            localFileInputStream.skip(lenght - Math.min(lenght, 65558L));
            byte[] arrayOfByte = new byte[1024];
            for (int i2 = 0; i2 != -1; i2 = localFileInputStream
                    .read(arrayOfByte)) {
                localMessageDigest.update(arrayOfByte, 0, i2);
            }
            BigInteger bi = new BigInteger(1, localMessageDigest.digest());
            return bi.toString(16);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
