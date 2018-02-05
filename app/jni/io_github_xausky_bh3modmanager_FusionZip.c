#include <fcntl.h>
#include <errno.h>
#include <dirent.h>
#include <string.h>
#include <unistd.h>

#ifdef __ANDROID_NDK__
#include <android/log.h>
#include "zip.h"
#include "io_github_xausky_bh3modmanager_FusionZip.h"
#else
#include <zip.h>
#endif

#ifdef __ANDROID_NDK__
#define __FUSION_LOG(str, args...) __android_log_print(ANDROID_LOG_DEBUG, "BH3ModManager", str, ##args)
#else
#define __FUSION_LOG(str, args...) printf(str, ##args)
#endif

#define BACKUP_BUFFER_ZISE (10240)

int fusionZip(const char * backupDirPath, const char * fusionDirPath, const char * prefixString, zip_t* target_zip){
    DIR* dir = opendir(fusionDirPath);
    if(dir == NULL)
    {
        __FUSION_LOG("fusionDir open failed, error: %s， path: %s\n", strerror(errno), fusionDirPath);
        return -1;
    }
    ZIP_ER_EXISTS;
    struct dirent* dirent;
    char archive_path_buffer[1024];
    char source_path_buffer[1024];
    char backup_path_buffer[1024];
    void* backup_buffer[BACKUP_BUFFER_ZISE];
    zip_error_t error;
    while((dirent = readdir(dir)) != NULL){
        if(dirent->d_type != DT_REG){
            continue;
        }
        sprintf(archive_path_buffer, "%s/%s", prefixString, dirent->d_name);
        sprintf(source_path_buffer, "%s/%s", fusionDirPath, dirent->d_name);
        zip_source_t * source = zip_source_file_create(source_path_buffer, 0, 0, &error);
        if(source == NULL){
            __FUSION_LOG("zip source file create failed, error: %s， path: %s\n", strerror(errno), source_path_buffer);
            closedir(dir);
            return -1;
        }
        zip_int64_t idx = 0;
        if ((idx = zip_name_locate(target_zip, archive_path_buffer, ZIP_FL_ENC_GUESS)) >= 0) {
            if(backupDirPath != NULL){
                sprintf(backup_path_buffer, "%s/%s", backupDirPath, dirent->d_name);
                int backup_file = open(backup_path_buffer, O_CREAT|O_WRONLY|O_EXCL, 0600);
                if(backup_file != -1){
                    zip_file_t* file = zip_fopen_index(target_zip, idx, ZIP_FL_UNCHANGED);
                    if(file == NULL){
                        __FUSION_LOG("backup zip file open failed, error: %s， backup path: %s, archive path: %s\n", zip_strerror(target_zip), backup_path_buffer, archive_path_buffer);
                        closedir(dir);
                        zip_source_close(source);
                        close(backup_file);
                        return -1;
                    }
                    int len = 0;
                    while((len = zip_fread(file, backup_buffer, BACKUP_BUFFER_ZISE)) > 0){
                        write(backup_file, backup_buffer, len);
                    }
                    if(len == -1){
                        __FUSION_LOG("backup zip file read failed, error: %s， backup path: %s, archive path: %s\n", zip_strerror(target_zip), backup_path_buffer, archive_path_buffer);
                        zip_fclose(file);
                        closedir(dir);
                        zip_source_close(source);
                        close(backup_file);
                        return -1;
                    }
                    zip_fclose(file);
                    close(backup_file);
                } else {
                    int error = errno;
                    if(error != EEXIST){
                        __FUSION_LOG("backup file open failed, error: %s， backup path: %s, archive path: %s\n", strerror(error), backup_path_buffer, archive_path_buffer);
                        closedir(dir);
                        zip_source_close(source);
                        return -1;
                    }
                }
            }
            if(zip_file_replace(target_zip, (zip_uint64_t)idx, source, ZIP_FL_OVERWRITE) == -1){
                __FUSION_LOG("zip file add failed, error: %s， source path: %s, archive path: %s\n", zip_strerror(target_zip), source_path_buffer, archive_path_buffer);
                closedir(dir);
                zip_source_close(source);
                return -1;
            }
            __FUSION_LOG("zip file replace ok, %s -> %s\n", source_path_buffer, archive_path_buffer);
        }
    }
    closedir(dir);
    return 0;
}

int patchZip(const char * backupDirPath, const char * fusionDirPath, const char * prefixString, const char * targetPath){
    int enumber = 0;
    zip_t* zip = zip_open(targetPath, 0, &enumber);
    if(zip == NULL){
        zip_error_t error;
        zip_error_init_with_code(&error, enumber);
        __FUSION_LOG("dest zip open failed, error: %s， path: %s\n", zip_error_strerror(&error), targetPath);
        zip_error_fini(&error);
        return -1;
    }
    if(fusionZip(NULL, backupDirPath, prefixString, zip) == -1){
        __FUSION_LOG("patch backup, error: %s， path: %s\n", zip_strerror(zip), targetPath);
        zip_close(zip);
        return -1;
    }
    if(fusionZip(backupDirPath,fusionDirPath , prefixString, zip) == -1){
        __FUSION_LOG("patch fusion, error: %s， path: %s\n", zip_strerror(zip), targetPath);
        zip_close(zip);
        return -1;
    }
    zip_close(zip);
    return 0;
}

#ifdef __ANDROID_NDK__
JNIEXPORT jint JNICALL Java_io_github_xausky_bh3modmanager_FusionZip_patchZip
        (JNIEnv *env, jclass cls, jstring backupDir, jstring fusionDir, jstring prefix, jstring target) {
    const char * backupDirPath = (*env)->GetStringUTFChars(env, backupDir,JNI_FALSE);
    const char * fusionDirPath = (*env)->GetStringUTFChars(env, fusionDir,JNI_FALSE);
    const char * prefixString = (*env)->GetStringUTFChars(env, prefix,JNI_FALSE);
    const char * targetPath = (*env)->GetStringUTFChars(env, target,JNI_FALSE);
    return patchZip(backupDirPath, fusionDirPath, prefixString, targetPath);
}
#else
int main(int argc, char* argv[]){
    if(patchZip(argv[1], argv[2], argv[3], argv[4]) != 0){
         __FUSION_LOG("patch zip error\n");
         return -1;
    }
    return 0;
}
#endif