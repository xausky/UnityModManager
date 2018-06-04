#include "io_github_xausky_unitymodmanager_utils_NativeUtils.h"
#include "ZipFile.hh"
#include "FolderFile.hh"
using namespace xausky;
JNIEXPORT jint JNICALL Java_io_github_xausky_unitymodmanager_utils_NativeUtils_PatchApk
        (JNIEnv *env, jclass cls, jstring input, jstring output, jstring mods){
    const char * inputPath = env->GetStringUTFChars(input,JNI_FALSE);
    const char * outputPath = env->GetStringUTFChars(output,JNI_FALSE);
    const char * modsPath = env->GetStringUTFChars(mods,JNI_FALSE);
    return ZipFile::Patch(inputPath, modsPath, outputPath);
}

JNIEXPORT jint JNICALL Java_io_github_xausky_unitymodmanager_utils_NativeUtils_GenerateApkMapFile
        (JNIEnv *env, jclass cls, jstring input, jstring output){
    const char * inputPath = env->GetStringUTFChars(input,JNI_FALSE);
    const char * outputPath = env->GetStringUTFChars(output,JNI_FALSE);
    return ZipFile::GenerateMapFile(inputPath, outputPath);
}

JNIEXPORT jint JNICALL Java_io_github_xausky_unitymodmanager_utils_NativeUtils_PatchFolder
        (JNIEnv *env, jclass cls, jstring target, jstring mods, jstring backup){
    const char * targetPath = env->GetStringUTFChars(target,JNI_FALSE);
    const char * backupPath = env->GetStringUTFChars(backup,JNI_FALSE);
    const char * modsPath = env->GetStringUTFChars(mods,JNI_FALSE);
    return FolderFile::Patch(targetPath, modsPath, backupPath);
}

JNIEXPORT jint JNICALL Java_io_github_xausky_unitymodmanager_utils_NativeUtils_GenerateFolderMapFile
        (JNIEnv *env, jclass cls, jstring input, jstring output){
    const char * inputPath = env->GetStringUTFChars(input,JNI_FALSE);
    const char * outputPath = env->GetStringUTFChars(output,JNI_FALSE);
    return FolderFile::GenerateMapFile(inputPath, outputPath);
}