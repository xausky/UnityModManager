#include "wwise_akpk.h"
#include "wwise_bank.h"
#include "utils.h"

static char path_buffer[PATH_MAX];

int8_t wwise_akpk_parser(wwise_akpk_t *akpk, binary_stream_t *stream, uint8_t parser_event){
    uint32_t toc_size[3], toc_files[3];
    wwise_akpk_key_bank_map_t put;
    memset(akpk, 0, sizeof(wwise_akpk_t));
    akpk->banks[0] = kb_init(wwise_akpk_key_bank_map, KB_DEFAULT_SIZE);
    akpk->banks[1] = kb_init(wwise_akpk_key_bank_map, KB_DEFAULT_SIZE);
    akpk->banks[2] = kb_init(wwise_akpk_key_bank_map, KB_DEFAULT_SIZE);
    if(binary_stream_read(stream, akpk->indentifier, 4) != 4){
        return -1;
    }
    if(strcmp(akpk->indentifier, "AKPK") != 0){
        return -2;
    }
    if(binary_stream_read_uint32(stream, &akpk->info) != 0){
        return -3;
    }
    if(binary_stream_read_uint32(stream, &akpk->version) != 0){
        return -4;
    }
    if(binary_stream_read_uint32(stream, &akpk->name_size) != 0){
        return -5;
    }
    if(binary_stream_read_uint32(stream, &toc_size[0]) != 0){
        return -6;
    }
    if(binary_stream_read_uint32(stream, &toc_size[1]) != 0){
        return -7;
    }
    if(binary_stream_read_uint32(stream, &toc_size[2]) != 0){
        return -8;
    }
    if(binary_stream_create_memory(&akpk->names, akpk->name_size, BINARY_STREAM_ORDER_LITTLE_ENDIAN) != 0){
        return -9;
    }
    if(binary_stream_copy(&akpk->names, stream, akpk->name_size) != 0){
        return -10;
    }
    akpk->pos = 0;
    for(int i = 0; i < 3; ++i){
        if(binary_stream_read_uint32(stream, &toc_files[i]) != 0){
            return -11;
        }
        for(int file_index = 0; file_index < toc_files[i]; ++file_index){
            if(i == 2){
                if(binary_stream_read_uint32(stream, &put.unknown) != 0){
                    return -12;
                }
            }
            if(binary_stream_read_uint32(stream, &put.id) != 0){
                return -12;
            }
            if(binary_stream_read_uint32(stream, &put.align) != 0){
                return -13;
            }
            if(binary_stream_read_uint32(stream, &put.size) != 0){
                return -14;
            }
            if(binary_stream_read_uint32(stream, &put.offset) != 0){
                return -15;
            }
            if(binary_stream_read_uint32(stream, &put.index) != 0){
                return -16;
            }
            put.key = ((uint64_t)put.index) << 32 | ((uint64_t)put.id);
            if(akpk->pos >= WWISE_AKPK_BANK_MAX){
                return -17;
            }
            akpk->sort[akpk->pos] = put.key;
            akpk->pos++;
            kb_put(wwise_akpk_key_bank_map, akpk->banks[i], put);
        }
    }
    if(parser_event){
        akpk->event_wems = kb_init(wwise_akpk_event_wems_map, 16 * sizeof(wwise_akpk_event_wems_map_t));
    }
    kbitr_t wwise_akpk_itr;
    for(int i = 0; i < 3; ++i){
        for (kb_itr_first(wwise_akpk_key_bank_map, akpk->banks[i], &wwise_akpk_itr); kb_itr_valid(&wwise_akpk_itr); kb_itr_next(wwise_akpk_key_bank_map, akpk->banks[i], &wwise_akpk_itr)) {
            wwise_akpk_key_bank_map_t *bank = &kb_itr_key(wwise_akpk_key_bank_map_t, &wwise_akpk_itr);
            if(binary_stream_create_memory(&bank->data, bank->size, BINARY_STREAM_ORDER_LITTLE_ENDIAN)){
                return -17;
            }
            if(binary_stream_seek(stream, bank->offset, SEEK_SET) < 0){
                return -18;
            }
            if(binary_stream_copy(&bank->data, stream, bank->size)){
                return -19;
            }
            if(binary_stream_seek(&bank->data, 0, SEEK_SET) != 0){
                return -20;
            }
            bank->sound.sections = NULL;
            if(parser_event){
                if(wwise_bank_parser(&bank->sound, &bank->data, 1) != 0){
                    return -21;
                }
                kbitr_t wwise_bank_itr;
                if(bank->sound.event_wems_map != NULL){
                    for (kb_itr_first(wwise_bank_key_values_map, bank->sound.event_wems_map, &wwise_bank_itr); kb_itr_valid(&wwise_bank_itr); kb_itr_next(wwise_bank_key_values_map, bank->sound.event_wems_map, &wwise_bank_itr)) {
                        wwise_bank_key_values_t *entity = &kb_itr_key(wwise_bank_key_values_t, &wwise_bank_itr);
                        wwise_akpk_event_wems_map_t find, *result;
                        find.key = entity->key;
                        find.size = 0;
                        result = kb_getp(wwise_akpk_event_wems_map, akpk->event_wems, &find);
                        if(!result){
                            result = kb_putp(wwise_akpk_event_wems_map, akpk->event_wems, &find);
                            if(!result){
                                return -22;
                            }
                        }
                        for(int i = 0; i < entity->size; ++i){
                            if(result->size >= WWISE_AKPK_EVENT_MAX_WEM){
                                break;
                            }
                            result->wems[result->size].bank_id = bank->id;
                            result->wems[result->size].index = bank->index;
                            result->wems[result->size].wem_id = entity->values[i];
                            ++result->size;
                        }
                    }
                }
            }
        }
    }
    return 0;
}

void wwise_akpk_destory(wwise_akpk_t *akpk){
    kbitr_t itr;
    binary_stream_destory(&akpk->names);
    if(akpk->event_wems != NULL){
        kb_destroy(wwise_akpk_event_wems_map, akpk->event_wems);
    }
    for(int i = 0; i < 3; ++i){
        kbtree_t(wwise_akpk_key_bank_map) * bank = akpk->banks[i];
        if(bank != NULL){
            for (kb_itr_first(wwise_akpk_key_bank_map, bank, &itr); kb_itr_valid(&itr); kb_itr_next(wwise_akpk_key_bank_map, bank, &itr)) {
                wwise_akpk_key_bank_map_t *entity = &kb_itr_key(wwise_akpk_key_bank_map_t, &itr);
                binary_stream_destory(&entity->data);
                if(entity->sound.sections != NULL){
                    wwise_bank_destory(&entity->sound);
                }
            }
            kb_destroy(wwise_akpk_key_bank_map, bank);
        }
    }
}

int8_t wwise_akpk_make_patch(wwise_akpk_patch_t *patch, const char *path){
    klist_t(utils_file_list) *files;
    kliter_t(utils_file_list) *p;
    uint32_t bank_id, index, wem_id;
    wwise_akpk_patch_element_t find, *result;
    wwise_bank_key_stream_t put;
    memset(patch, 0, sizeof(wwise_akpk_patch_t));
    patch->patch = kb_init(wwise_akpk_patch_map, KB_DEFAULT_SIZE);
    if(patch->patch == NULL){
        return -1;
    }
    files = utils_file_list_make(path);
    if(files == NULL){
        return -1;
    }
    for (p = kl_begin(files); p != kl_end(files); p = kl_next(p)){
        if(sscanf(kl_val(p).name, "%u-%u-%u.wem", &bank_id, &index, &wem_id) != 3){
            continue;
        }
        find.key = ((uint64_t)index) << 32 | ((uint64_t)bank_id);
        result = kb_getp(wwise_akpk_patch_map, patch->patch, &find);
        if(result == NULL){
            result = kb_putp(wwise_akpk_patch_map, patch->patch, &find);
            result->patch = kb_init(wwise_bank_key_stream_map, KB_DEFAULT_SIZE);
        }
        put.key = wem_id;
        sprintf(path_buffer, "%s/%s", path, kl_val(p).name);
        if(binary_stream_create_file(&put.stream, path_buffer, BINARY_STREAM_ORDER_LITTLE_ENDIAN) != 0){
            return -4;
        }
        kb_put(wwise_bank_key_stream_map, result->patch, put);
    }
    utils_file_list_destory(files);
    return 0;
}

void wwise_akpk_destory_patch(wwise_akpk_patch_t *patch){
    kbitr_t bank, wem;
    for(kb_itr_first(wwise_akpk_patch_map, patch->patch, &bank); kb_itr_valid(&bank); kb_itr_next(wwise_akpk_patch_map, patch->patch, &bank)){
        wwise_akpk_patch_element_t *entity = &kb_itr_key(wwise_akpk_patch_element_t, &bank);
        for(kb_itr_first(wwise_bank_key_stream_map, entity->patch, &wem); kb_itr_valid(&wem); kb_itr_next(wwise_bank_key_stream_map, entity->patch, &wem)){
            wwise_bank_key_stream_t *wem_entity = &kb_itr_key(wwise_bank_key_stream_t, &wem);
            binary_stream_destory(&wem_entity->stream);
        }
        kb_destroy(wwise_bank_key_stream_map, entity->patch);
    }
    kb_destroy(wwise_akpk_patch_map, patch->patch);
}

int8_t wwise_akpk_patch(wwise_akpk_t *akpk, wwise_akpk_patch_t *patch){
    kbitr_t itr;
    wwise_akpk_key_bank_map_t find, *result;
    for(kb_itr_first(wwise_akpk_patch_map, patch->patch, &itr); kb_itr_valid(&itr); kb_itr_next(wwise_akpk_patch_map, patch->patch, &itr)){
        wwise_akpk_patch_element_t *entity = &kb_itr_key(wwise_akpk_patch_element_t, &itr);
        wwise_akpk_key_bank_map_t *result;
        find.key = entity->key;
        for(int i = 0; i < 3; ++i){
            result = kb_getp(wwise_akpk_key_bank_map, akpk->banks[i], &find);
            if(result != NULL){
                break;
            }
        }
        if(result != NULL){
            if(wwise_bank_parser(&result->sound, &result->data, 0) != 0){
                return -1;
            }
            if(wwise_bank_patch(&result->sound, entity->patch) != 0){
                return -1;
            }
            binary_stream_destory(&result->data);
            if(binary_stream_create_memory(&result->data, 1024, BINARY_STREAM_ORDER_LITTLE_ENDIAN) != 0){
                return -2;
            }
            if(wwise_bank_save(&result->sound, &result->data) != 0){
                return -3;
            }
            wwise_bank_destory(&result->sound);
        }
    }
    return 0;
}

int8_t wwise_akpk_save(wwise_akpk_t *akpk, binary_stream_t *stream){
    int32_t start;
    kbitr_t itr;
    if(binary_stream_write(stream, "AKPK", 4) != 4){
        return -1;
    }
    if(binary_stream_write_uint32(stream, akpk->info) != 0){
        return -2;
    }
    if(binary_stream_write_uint32(stream, akpk->version) != 0){
        return -2;
    }
    if(binary_stream_write_uint32(stream, akpk->name_size) != 0){
        return -2;
    }
    if(binary_stream_write_uint32(stream, kb_size(akpk->banks[0]) * 20 + 4) != 0){
        return -2;
    }
    if(binary_stream_write_uint32(stream, kb_size(akpk->banks[1]) * 20 + 4) != 0){
        return -2;
    }
    if(binary_stream_write_uint32(stream, kb_size(akpk->banks[2]) * 24 + 4) != 0){
        return -2;
    }
    if(binary_stream_seek(&akpk->names, 0, SEEK_SET) != 0){
        return -1;
    }
    if(binary_stream_copy(stream, &akpk->names, akpk->name_size) != 0){
        return -1;
    }
    if((start = binary_stream_seek(stream, 0, SEEK_CUR)) < 0){
            return -3;
    }
    start += 12 + kb_size(akpk->banks[0]) * 20 + kb_size(akpk->banks[1]) * 20 + kb_size(akpk->banks[2]) * 24;
    int i = 0;
    for(int pos = 0; pos <= akpk->pos; ++pos){
        wwise_akpk_key_bank_map_t find, *entity;
        if(pos == 0){
            if(binary_stream_write_uint32(stream, kb_size(akpk->banks[i])) != 0){
                return -2;
            }
        } else if(pos == kb_size(akpk->banks[0])){
            i++;
            if(binary_stream_write_uint32(stream, kb_size(akpk->banks[i])) != 0){
                return -2;
            }
        } else if(pos == (kb_size(akpk->banks[0]) + kb_size(akpk->banks[1]))){
            i++;
            if(binary_stream_write_uint32(stream, kb_size(akpk->banks[2])) != 0){
                return -2;
            }
        }
        if(pos == akpk->pos){
            break;
        }
        find.key = akpk->sort[pos];
        entity = kb_getp(wwise_akpk_key_bank_map, akpk->banks[i], &find);
        int64_t size;
        if(i == 2){
            if(binary_stream_write_uint32(stream, entity->unknown) != 0){
                return -2;
            }
        }
        if(binary_stream_write_uint32(stream, entity->id) != 0){
            return -2;
        }
        if(binary_stream_write_uint32(stream, entity->align) != 0){
            return -2;
        }
        if((size = binary_stream_seek(&entity->data, 0, SEEK_END)) < 0){
            return -3;
        }
        entity->size = size;
        if(binary_stream_write_uint32(stream, entity->size) != 0){
            return -2;
        }
        if(binary_stream_write_uint32(stream, start) != 0){
            return -2;
        }
        if(binary_stream_write_uint32(stream, entity->index) != 0){
            return -2;
        }
        start += entity->size;
    }
    for(int pos = 0; pos < akpk->pos; ++pos){
        wwise_akpk_key_bank_map_t find, *entity;
        int i = 0;
        if(pos >= kb_size(akpk->banks[0])){
            i = 1;
        }
        if(pos >= kb_size(akpk->banks[0]) + kb_size(akpk->banks[1])){
            i = 2;
        }
        find.key = akpk->sort[pos];
        entity = kb_getp(wwise_akpk_key_bank_map, akpk->banks[i], &find);
        if(binary_stream_seek(&entity->data, 0, SEEK_SET) != 0){
            return -3;
        }
        if(binary_stream_copy(stream, &entity->data, entity->size) != 0){
            return -2;
        }
    }
    return 0;
}