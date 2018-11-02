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

void utils_file_stream_dump(binary_stream_t *stream, int len, uint32_t id){
    char name[1024];
    binary_stream_t out;
    int64_t pos = binary_stream_seek(stream, 0, SEEK_CUR);
    sprintf(name, "%u-%u.dump", id, rand());
    binary_stream_create_file(&out, name, BINARY_STREAM_ORDER_LITTLE_ENDIAN);
    binary_stream_copy(&out, stream, len);
    binary_stream_destory(&out);
    binary_stream_seek(stream, pos, SEEK_SET);
}