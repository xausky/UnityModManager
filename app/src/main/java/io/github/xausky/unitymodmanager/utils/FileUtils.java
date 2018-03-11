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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import io.github.xausky.unitymodmanager.MainApplication;

/**
 * Created by xausky on 18-2-9.
 */

public class FileUtils {
    public static final int RESULT_STATE_OK = 0;
    public static final int RESULT_STATE_INTERNAL_ERROR = -1;
    public static final int RESULT_STATE_FILE_CONFLICT = -3;
    /**
     * 通过递归调用删除一个文件夹及下面的所有文件
     * @param file
     */
    public static boolean deleteFile(File file){
        if(!file.exists()){
            return true;
        }
        if(file.isFile()){//表示该文件不是文件夹
            if(!file.delete()){
                Log.d(MainApplication.LOG_TAG, "delete failed: " + file.getAbsolutePath());
                return false;
            }
        }else{
            //首先得到当前的路径
            File[] childFiles = file.listFiles();
            for(File childFile : childFiles){
                if(!deleteFile(childFile)){
                    return false;
                }
            }
            if(!file.delete()){
                Log.d(MainApplication.LOG_TAG, "delete failed: " + file.getAbsolutePath());
                return false;
            }
        }
        return true;
    }

    public static int copyModDirectoryFile(File source, String target, boolean force) {
        byte[] buffer = new byte[10240];
        File[] files = source.listFiles();
        for(File file: files){
            if(file.isFile() && file.getName().indexOf('.') == -1){
                File targetFile = new File(target + '/' + file.getName());
                if(targetFile.exists()){
                    if(!force){
                        return RESULT_STATE_FILE_CONFLICT;
                    }
                }
                FileInputStream inputStream = null;
                FileOutputStream outputStream = null;
                try{
                    inputStream = new FileInputStream(file);
                    outputStream = new FileOutputStream(targetFile);
                    int len = 0;
                    while ((len = inputStream.read(buffer)) != -1){
                        outputStream.write(buffer, 0, len);
                    }
                }catch (FileNotFoundException e){
                    e.printStackTrace();
                    return RESULT_STATE_INTERNAL_ERROR;
                } catch (IOException e) {
                    e.printStackTrace();
                    return RESULT_STATE_INTERNAL_ERROR;
                }finally {
                    if (inputStream != null){
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if(outputStream != null){
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else if(file.isDirectory()){
                for(File child: file.listFiles()){
                    int result = copyModDirectoryFile(child, target, force);
                    if(result != RESULT_STATE_OK){
                        return result;
                    }
                }
            }
        }
        return RESULT_STATE_OK;
    }
}
