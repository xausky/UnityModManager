package io.github.xausky.unitymodmanager.utils;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Set;
import java.util.TreeSet;

import io.github.xausky.unitymodmanager.MainApplication;

/**
 * Created by xausky on 18-2-9.
 */

public class ModUtils {
    public static final int RESULT_STATE_OK = 0;
    public static final int RESULT_STATE_INTERNAL_ERROR = -1;
    public static JSONObject map;
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
            try {
                path = map.getString(input.getName() + "/" + name);
                Log.d(MainApplication.LOG_TAG, "Standardization: " + path);
            } catch (JSONException e) {
                //ignore
            }
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
            } else if(name.length() - 4 > 0 && supportImageType.contains(name.substring(name.length() - 4))){
                try {
                    FileUtils.copyFile(file, new File(output + "/images/" + System.currentTimeMillis() + "-" + name));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }
}
