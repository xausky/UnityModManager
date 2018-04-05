#ifndef XAUSKY_LOG_UTILS_HH
#define XAUSKY_LOG_UTILS_HH
#ifdef __ANDROID__
#include <android/log.h>
#define __LIBUABE_LOG(str, args...) __android_log_print(ANDROID_LOG_DEBUG, "NativeUtils", str, ##args)
#define __LIBUABE_DEBUG(str, args...)
#else
#include <stdio.h>
#define __LIBUABE_LOG(str, args...) printf(str, ##args)
#define __LIBUABE_DEBUG(str, args...) printf(str, ##args)
#endif
#endif