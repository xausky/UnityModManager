#include "io_github_xausky_unitymodmanager_utils_NativeUtils.h"
#include "ZipFile.hh"
using namespace xausky;
JNIEXPORT jint JNICALL Java_io_github_xausky_unitymodmanager_utils_NativeUtils_patch
        (JNIEnv *env, jclass cls, jstring input, jstring output, jstring mods){
    const char * inputPath = env->GetStringUTFChars(input,JNI_FALSE);
    const char * outputPath = env->GetStringUTFChars(output,JNI_FALSE);
    const char * modsPath = env->GetStringUTFChars(mods,JNI_FALSE);
    return ZipFile::patch(inputPath, modsPath, outputPath);
}

JNIEXPORT jint JNICALL Java_io_github_xausky_unitymodmanager_utils_NativeUtils_GenerateMapFile
        (JNIEnv *env, jclass cls, jstring input, jstring output){
    const char * inputPath = env->GetStringUTFChars(input,JNI_FALSE);
    const char * outputPath = env->GetStringUTFChars(output,JNI_FALSE);
    return ZipFile::GenerateMapFile(inputPath, outputPath);
}