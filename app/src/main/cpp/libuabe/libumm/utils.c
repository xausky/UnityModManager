#include "utils.h"


klist_t(utils_file_list)* utils_file_list_make(const char *path){
    struct dirent * entity;
    klist_t(utils_file_list) *result = kl_init(utils_file_list);
    DIR *dir = opendir(path);
    if(dir == NULL){
        return NULL;
    }
    while((entity = readdir(dir)) != NULL){
        if(entity->d_type == DT_REG){
            utils_file_t *file = kl_pushp(utils_file_list, result);
            strcpy(file->name, entity->d_name);
        }
    }
    closedir(dir);
    return result;
}

void utils_file_list_destory(klist_t(utils_file_list)* list){
    kl_destroy(utils_file_list, list);
}