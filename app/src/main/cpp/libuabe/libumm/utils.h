#if defined (__cplusplus)
extern "C" {
#endif
#ifndef UTILS_H
#define UTILS_H

#include <dirent.h>
#include <string.h>
#include <limits.h>

#include "klist.h"
#include "binary_stream.h"

#ifdef __ANDROID__
#include <android/log.h>
#define __LIBUABE_LOG(str, args...) __android_log_print(ANDROID_LOG_DEBUG, "NativeUtils", str, ##args)
#define __LIBUABE_DEBUG(str, args...)
#else
#include <stdio.h>
#define __LIBUABE_LOG(str, args...) printf(str, ##args)
#ifdef DEBUG
#define __LIBUABE_DEBUG(str, args...) printf(str, ##args)
#else
#define __LIBUABE_DEBUG(str, args...)
#endif
#endif

#define __unuse_free(x)

typedef struct {
    char name[NAME_MAX + 1];
} utils_file_t;

KLIST_INIT(utils_file_list, utils_file_t, __unuse_free)

klist_t(utils_file_list) *utils_file_list_make(const char *path);

void utils_file_list_destory(klist_t(utils_file_list) *list);

void utils_file_stream_dump(binary_stream_t *stream, int len, uint32_t id);

#endif
#if defined (__cplusplus)
}
#endif