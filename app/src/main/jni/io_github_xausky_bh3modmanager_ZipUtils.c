#include <fcntl.h>
#include <errno.h>
#include <dirent.h>
#include <string.h>
#include <unistd.h>

#ifdef __ANDROID_NDK__
#include <android/log.h>
#include "zip.h"
#include "io_github_xausky_bh3modmanager_ZipUtils.h"
#else
#include <zip.h>
#endif

#ifdef __ANDROID_NDK__
#define __FUSION_LOG(str, args...) __android_log_print(ANDROID_LOG_DEBUG, "BH3ModManager", str, ##args)
#else
#define __FUSION_LOG(str, args...) printf(str, ##args)
#endif

#define CPOY_BUFFER_ZISE (10240)

const char * resolveName(const char * name){
    const char * file_name = strrchr(name, '/');
    if(file_name == NULL){
        file_name = name;
    } else {
        ++file_name;
    }
    return strchr(file_name, '.') == NULL && strlen(file_name) == 32 ? file_name: NULL;
}

int unzipFile(const char * zipFile, const char * targetDir, const char * password, uint8_t force){
    int enumber = 0;
    char target_path_buffer[1024];
    uint8_t copy_buffer[CPOY_BUFFER_ZISE];
    zip_t* zip = zip_open(zipFile, 0, &enumber);
    if(zip == NULL){
        zip_error_t error;
        zip_error_init_with_code(&error, enumber);
        __FUSION_LOG("dest zip open failed, error: %s， path: %s\n", zip_error_strerror(&error), zipFile);
        zip_error_fini(&error);
        return -1;
    }
    if(zip_set_default_password(zip, password) == -1){
        __FUSION_LOG("set password failed, error: %s， path: %s\n", zip_strerror(zip), zipFile);
        zip_close(zip);
        return -1;
    }
    zip_int64_t entries_num = zip_get_num_entries(zip, ZIP_FL_UNCHANGED);
    for (zip_uint64_t i = 0; i < entries_num; ++i) {
        const char * name = zip_get_name(zip, i, ZIP_FL_ENC_GUESS);
        if(name == NULL){
            zip_close(zip);
            __FUSION_LOG("get zip entry name, error: %s， path: %s, index: %lld\n", zip_strerror(zip), zipFile, i);
        }
        const char * file_name = resolveName(name);
        if(file_name != NULL){
            zip_file_t* file = zip_fopen_index(zip, i, ZIP_FL_UNCHANGED);
            if(file == NULL){
                zip_close(zip);
                int error_code = zip_error_code_zip(zip_get_error(zip));
                if(error_code == ZIP_ER_NOPASSWD || error_code == ZIP_ER_WRONGPASSWD){
                    __FUSION_LOG("zip fopen failed, password error: %s， path: %s, index: %lld, password: %s\n", zip_strerror(zip), zipFile, i, password);
                    return -2;
                }
                __FUSION_LOG("zip fopen failed, error: %s， path: %s, index: %lld\n", zip_strerror(zip), zipFile, i);
                return -1;
            }
            sprintf(target_path_buffer, "%s/%s", targetDir, file_name);
            int target_file = open(target_path_buffer, O_CREAT|O_WRONLY|O_EXCL, 0600);
            if(target_file == -1){
                zip_fclose(file);
                int error = errno;
                if(error == EEXIST){
                    if(force){
                        continue;
                    }
                    zip_close(zip);
                    return -3;
                } else {
                    __FUSION_LOG("target file open failed, error: %s， target file path: %s\n", strerror(error), target_path_buffer);
                    return -1;
                }
            }
            int len = 0;
            while((len = zip_fread(file, copy_buffer, CPOY_BUFFER_ZISE)) > 0){
                write(target_file, copy_buffer, len);
            }
            zip_fclose(file);
            close(target_file);
        }
    }
    zip_close(zip);
    return 0;
}

#ifdef __ANDROID_NDK__
JNIEXPORT jint JNICALL Java_io_github_xausky_bh3modmanager_ZipUtils_unzipFile
        (JNIEnv *env, jclass cls, jstring zipFile, jstring targetDir, jstring password, jboolean force){
    const char * zipFilePath = (*env)->GetStringUTFChars(env, zipFile,JNI_FALSE);
    const char * targetDirPath = (*env)->GetStringUTFChars(env, targetDir,JNI_FALSE);
    const char * passwordString = NULL;
    if(password != NULL){
        passwordString = (*env)->GetStringUTFChars(env, password,JNI_FALSE);
    }
    return unzipFile(zipFilePath, targetDirPath, passwordString, force);
}
#else
int main(int argc, char* argv[]){
    printf(resolveName(argv[1]));
    return 0;
}
#endif