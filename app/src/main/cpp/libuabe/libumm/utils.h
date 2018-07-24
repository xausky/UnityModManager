#if defined (__cplusplus)
extern "C" {
#endif
#ifndef UTILS_H
#define UTILS_H

#include <dirent.h>
#include <string.h>
#include <climits>

#include "klist.h"

#define __unuse_free(x)

typedef struct {
    char name[NAME_MAX + 1];
} utils_file_t;

KLIST_INIT(utils_file_list, utils_file_t, __unuse_free)

klist_t(utils_file_list) *utils_file_list_make(const char *path);

void utils_file_list_destory(klist_t(utils_file_list) *list);

#endif
#if defined (__cplusplus)
}
#endif