#include "wwise_bank.h"

int8_t wwise_bank_parser_event_voice(kbtree_t(wwise_bank_key_value_map) *map, binary_stream_t *stream){
    uint32_t id, wem;
    uint8_t type;
    wwise_bank_key_value_t entity;
    if(binary_stream_read_uint32(stream, &id) != 0){
        return -1;
    }
    if(binary_stream_seek(stream, 4, SEEK_CUR) == -1){
        return -2;
    }
    if(binary_stream_read_uint8(stream, &type) != 0){
        return -1;
    }
    if(binary_stream_read_uint32(stream, &wem) != 0){
        return -3;
    }
    entity.key = id;
    entity.value = wem;
    if(type == 0){
        kb_put(wwise_bank_key_value_map, map, entity);
    }
    return 0;
}

int8_t wwise_bank_parser_event_action(kbtree_t(wwise_bank_key_value_map) *map, binary_stream_t *stream){
    uint32_t id, voice;
    uint8_t scope, type;
    wwise_bank_key_value_t entity;
    if(binary_stream_read_uint32(stream, &id) != 0){
        return -1;
    }
    if(binary_stream_read_uint8(stream, &scope) != 0){
        return -2;
    }
    if(binary_stream_read_uint8(stream, &type) != 0){
        return -3;
    }
    if(binary_stream_read_uint32(stream, &voice) != 0){
        return -4;
    }
    entity.key = id;
    entity.value = voice;
    kb_put(wwise_bank_key_value_map, map, entity);
    return 0;
}

int8_t wwise_bank_parser_event_event(kbtree_t(wwise_bank_key_values_map) *map, binary_stream_t *stream){
    uint32_t id, number, action;
    wwise_bank_key_values_t entity;
    if(binary_stream_read_uint32(stream, &id) != 0){
        return -1;
    }
    if(binary_stream_read_uint32(stream, &number) != 0){
        return -2;
    }
    entity.key = id;
    entity.size = number;
    for(int i = 0; i < number; ++i){
        if(i >= EVENT_MAX_ACTION_BUMBER){
            puts("EVENT_MAX_ACTION_BUMBER error.\n");
            break;
        }
        if(binary_stream_read_uint32(stream, &action) != 0){
            return -3;
        }
        entity.values[i] = action;
    }
    kb_put(wwise_bank_key_values_map, map, entity);
    return 0;
}

int8_t wwise_bank_parser_event_container(kbtree_t(wwise_bank_key_values_map) *map, binary_stream_t *stream){
    uint32_t id, number, voice;
    wwise_bank_key_values_t entity;
    uint64_t start = binary_stream_seek(stream, 0, SEEK_CUR);
    if(binary_stream_read_uint32(stream, &id) != 0){
        return -1;
    }
    if(binary_stream_seek(stream, 0x34, SEEK_CUR) == -1){
        return -2;
    }
    if(binary_stream_read_uint32(stream, &number) != 0){
        return -2;
    }
    if(number > EVENT_MAX_ACTION_BUMBER){
        return 0;
    }
    entity.key = id;
    entity.size = number;
    for(int i = 0; i < number; ++i){
        if(binary_stream_read_uint32(stream, &voice) != 0){
            return -3;
        }
        entity.values[i] = voice;
    }
    if(number > 0){
        kb_put(wwise_bank_key_values_map, map, entity);
    }
    return 0;
}

int8_t wwise_bank_parser_event(wwise_bank_t *bank, binary_stream_t *stream){
    bank->voice_wem_map = kb_init(wwise_bank_key_value_map, KB_DEFAULT_SIZE);
    bank->event_wems_map = kb_init(wwise_bank_key_values_map, KB_DEFAULT_SIZE);
    bank->action_object_map = kb_init(wwise_bank_key_value_map, KB_DEFAULT_SIZE);
    bank->event_actions_map = kb_init(wwise_bank_key_values_map, KB_DEFAULT_SIZE);
    bank->object_voices_map = kb_init(wwise_bank_key_values_map, KB_DEFAULT_SIZE);
    uint8_t type;
    uint32_t number, length, id;
    int64_t end;
    kbitr_t itr;
    if(binary_stream_read_uint32(stream, &number) != 0){
        return -1;
    }
    for(int i = 0; i < number; ++i){
        if(binary_stream_read_uint8(stream, &type) != 0){
            return -2;
        }
        if(binary_stream_read_uint32(stream, &length) != 0){
            return -2;
        }
        end = binary_stream_seek(stream, 0, SEEK_CUR);
        if(end < 0){
            return -3;
        }
        switch(type){
            case 4: //Event
                if(wwise_bank_parser_event_event(bank->event_actions_map, stream)!=0){
                    return -4;
                }
                break;
            case 3: //Action
                if(wwise_bank_parser_event_action(bank->action_object_map, stream)!=0){
                    return -5;
                }
                break;
            case 2: //Voice
                if(wwise_bank_parser_event_voice(bank->voice_wem_map, stream)!=0){
                    return -6;
                }
                break;
            case 5: //Random Container or Sequence Container
                if(wwise_bank_parser_event_container(bank->object_voices_map, stream)!=0){
                    return -7;
                }
                break;
        }
        end += length;
        binary_stream_seek(stream, end, SEEK_SET);
    }
    for (kb_itr_first(wwise_bank_key_values_map, bank->event_actions_map, &itr); kb_itr_valid(&itr); kb_itr_next(wwise_bank_key_values_map, bank->event_actions_map, &itr)) { // move on
        wwise_bank_key_value_t find, *result;
        wwise_bank_key_values_t finds, *results;
        wwise_bank_key_values_t put;
        wwise_bank_key_values_t *entity = &kb_itr_key(wwise_bank_key_values_t, &itr);
        put.key = entity->key;
        put.size = 0;
        for(int i = 0; i < entity->size; ++i){
            find.key = entity->values[i];
            result = kb_getp(wwise_bank_key_value_map, bank->action_object_map, &find);
            if(result){
                find.key = result->value;
                result = kb_getp(wwise_bank_key_value_map, bank->voice_wem_map, &find);
                if(result){
                    put.values[put.size] = result->value;
                    ++put.size;
                }
                finds.key = find.key;
                results = kb_getp(wwise_bank_key_values_map, bank->object_voices_map, &finds);
                if(results){
                    for(int voice_index = 0; voice_index < results->size; ++voice_index){
                        find.key = results->values[voice_index];
                        result = kb_getp(wwise_bank_key_value_map, bank->voice_wem_map, &find);
                        if(result){
                            put.values[put.size] = result->value;
                            ++put.size;
                        }
                    }
                }
            }
        }
        kb_put(wwise_bank_key_values_map, bank->event_wems_map, put);
    }
    return 0;
}

int8_t wwise_bank_parser_didx(wwise_bank_t *bank, binary_stream_t *stream, uint32_t length){
    int32_t current, start;
    wwise_bank_key_stream_t put;
    if(bank->wem_stream_map == 0){
        bank->wem_stream_map = kb_init(wwise_bank_key_stream_map, KB_DEFAULT_SIZE);
    }
    start = binary_stream_seek(stream, 0, SEEK_CUR);
    if(start < 0){
        return -2;
    }
    current = start;
    while(current - start < length){
        if(binary_stream_read_uint32(stream, &put.key) != 0){
            return -3;
        }
        if(binary_stream_read_uint32(stream, &put.offset) != 0){
            return -4;
        }
        if(binary_stream_read_uint32(stream, &put.length) != 0){
            return -5;
        }
        kb_put(wwise_bank_key_stream_map, bank->wem_stream_map, put);
        current = binary_stream_seek(stream, 0, SEEK_CUR);
        if(current < 0){
            return -6;
        }
    }
    return 0;
}

int8_t wwise_bank_parser_data(wwise_bank_t *bank, binary_stream_t *stream){
    kbitr_t itr;
    int32_t start;
    wwise_bank_key_stream_t put;
    if(bank->wem_stream_map == 0){
        printf("wwise_bank_parser_data not didx.");
        return 0;
    }
    start = binary_stream_seek(stream, 0, SEEK_CUR);
    if(start < 0){
        return -2;
    }
    for (kb_itr_first(wwise_bank_key_stream_map, bank->wem_stream_map, &itr); kb_itr_valid(&itr); kb_itr_next(wwise_bank_key_stream_map, bank->wem_stream_map, &itr)) {
        wwise_bank_key_stream_t *entity = &kb_itr_key(wwise_bank_key_stream_t, &itr);
        if(binary_stream_create_memory(&entity->stream, entity->length, BINARY_STREAM_ORDER_LITTLE_ENDIAN) != 0){
            return -3;
        }
        if(binary_stream_seek(stream, start + entity->offset, SEEK_SET) < 0){
            return -4;
        }
        if(binary_stream_copy(&entity->stream, stream, entity->length) < 0){
            return -5;
        }
    }
    return 0;
}

int8_t wwise_bank_parser(wwise_bank_t *bank, binary_stream_t *stream, uint8_t parser_event){
    char indentifier[4];
    uint32_t length;
    int8_t result;
    memset(bank, 0, sizeof(wwise_bank_t));
    wwise_bank_section_t put;
    bank->sections = kb_init(wwise_bank_section_map, KB_DEFAULT_SIZE);
    while(binary_stream_read(stream, indentifier, 4) == 4){
        if(binary_stream_read_uint32(stream, &length) != 0){
            return -1;
        }
        memset(&put, 0, sizeof(wwise_bank_section_t));
        memcpy(put.key, indentifier, 4);
        if(binary_stream_create_memory(&put.stream, length, BINARY_STREAM_ORDER_LITTLE_ENDIAN) != 0){
            return -2;
        }
        if(binary_stream_copy(&put.stream, stream, length) != 0){
            return -3;
        }
        binary_stream_seek(&put.stream, 0, SEEK_SET);
        if(strcmp(put.key, "HIRC") == 0 && parser_event){
            if((result = wwise_bank_parser_event(bank, &put.stream)) != 0){
                return -4;
            }
        } else if(strcmp(put.key, "DIDX") == 0){
            if((result = wwise_bank_parser_didx(bank, &put.stream, length)) != 0){
                return -5;
            }
        } else if(strcmp(put.key, "DATA") == 0){
            if((result = wwise_bank_parser_data(bank, &put.stream)) != 0){
                return -6;
            }
        }
        kb_put(wwise_bank_section_map, bank->sections, put);
    }
    return 0;
}

int8_t wwise_bank_patch(wwise_bank_t *bank, kbtree_t(wwise_bank_key_stream_map) *patch){
    kbitr_t wem;
    wwise_bank_key_stream_t find, *result;
    if(bank->wem_stream_map == NULL){
        return 0;
    }
    for(kb_itr_first(wwise_bank_key_stream_map, patch, &wem); kb_itr_valid(&wem); kb_itr_next(wwise_bank_key_stream_map, patch, &wem)){
        wwise_bank_key_stream_t *wem_entity = &kb_itr_key(wwise_bank_key_stream_t, &wem);
        find.key = wem_entity->key;
        result = kb_getp(wwise_bank_key_stream_map, bank->wem_stream_map, &find);
        if(result != NULL){
            binary_stream_destory(&result->stream);
            result->stream = wem_entity->stream;
        }
    }
    return 0;
}

int8_t wwise_bank_save(wwise_bank_t *bank, binary_stream_t *stream){
    kbitr_t wem, section_itr;
    int64_t size;
    uint32_t offset = 0;
    wwise_bank_section_t find, *didx_section, *data_section;
    strcpy(find.key, "DIDX");
    didx_section = kb_getp(wwise_bank_section_map, bank->sections, &find);
    strcpy(find.key, "DATA");
    data_section = kb_getp(wwise_bank_section_map, bank->sections, &find);
    if(didx_section == NULL || data_section == NULL){
        return -1;
    }
    binary_stream_destory(&didx_section->stream);
    if(binary_stream_create_memory(&didx_section->stream, 1024, BINARY_STREAM_ORDER_LITTLE_ENDIAN) != 0){
        return -2;
    }
    binary_stream_destory(&data_section->stream);
    if(binary_stream_create_memory(&data_section->stream, 1024, BINARY_STREAM_ORDER_LITTLE_ENDIAN) != 0){
        return -3;
    }
    for(kb_itr_first(wwise_bank_key_stream_map, bank->wem_stream_map, &wem); kb_itr_valid(&wem); kb_itr_next(wwise_bank_key_stream_map, bank->wem_stream_map, &wem)){
        wwise_bank_key_stream_t *wem_entity = &kb_itr_key(wwise_bank_key_stream_t, &wem);
        if((size = binary_stream_seek(&wem_entity->stream, 0, SEEK_END)) < 0){
            return -2;
        }
        if(binary_stream_write_uint32(&didx_section->stream, wem_entity->key) != 0){
            return -3;
        }
        if(binary_stream_write_uint32(&didx_section->stream, offset)){
            return -4;
        }
        if(binary_stream_write_uint32(&didx_section->stream, size)){
            return -5;
        }
        if(binary_stream_seek(&wem_entity->stream, 0, SEEK_SET) != 0){
            return -6;
        }
        if(binary_stream_copy(&data_section->stream, &wem_entity->stream, size)){
            return -7;
        }
        offset += size;
    }
    for(kb_itr_first(wwise_bank_section_map, bank->sections, &section_itr); kb_itr_valid(&section_itr); kb_itr_next(wwise_bank_section_map, bank->sections, &section_itr)){
        wwise_bank_section_t *section_entity = &kb_itr_key(wwise_bank_section_t, &section_itr);
        if(strcmp(section_entity->key, "DATA") == 0){
            continue;
        }
        if((size = binary_stream_seek(&section_entity->stream, 0, SEEK_END)) < 0){
            return -8;
        }
        if(binary_stream_write(stream, section_entity->key, 4) != 4){
            return -9;
        }
        if(binary_stream_write_uint32(stream, size) != 0){
            return -10;
        }
        if(binary_stream_seek(&section_entity->stream, 0, SEEK_SET) < 0){
            return -11;
        }
        if(binary_stream_copy(stream, &section_entity->stream, size)){
            return -12;
        }
    }
    wwise_bank_section_t *section_entity;
    strcpy(find.key, "DATA");
    section_entity = kb_getp(wwise_bank_section_map, bank->sections, &find);
    if(section_entity != NULL){
        if((size = binary_stream_seek(&section_entity->stream, 0, SEEK_END)) < 0){
            return -8;
        }
        if(binary_stream_write(stream, section_entity->key, 4) != 4){
            return -9;
        }
        if(binary_stream_write_uint32(stream, size) != 0){
            return -10;
        }
        if(binary_stream_seek(&section_entity->stream, 0, SEEK_SET) < 0){
            return -11;
        }
        if(binary_stream_copy(stream, &section_entity->stream, size)){
            return -12;
        }
    }
    return 0;
}

void wwise_bank_destory(wwise_bank_t *bank){
    kbitr_t itr;
    if(bank->voice_wem_map != NULL){
        kb_destroy(wwise_bank_key_value_map, bank->voice_wem_map);
    }
    if(bank->action_object_map != NULL){
        kb_destroy(wwise_bank_key_value_map, bank->action_object_map);
    }
    if(bank->event_wems_map != NULL){
        kb_destroy(wwise_bank_key_values_map, bank->event_wems_map);
    }
    if(bank->event_actions_map != NULL){
        kb_destroy(wwise_bank_key_values_map, bank->event_actions_map);
    }
    if(bank->object_voices_map != NULL){
        kb_destroy(wwise_bank_key_values_map, bank->object_voices_map);
    }
    if(bank->sections != NULL){
        for (kb_itr_first(wwise_bank_section_map, bank->sections, &itr); kb_itr_valid(&itr); kb_itr_next(wwise_bank_section_map, bank->sections, &itr)) {
            wwise_bank_section_t *entity = &kb_itr_key(wwise_bank_section_t, &itr);
            binary_stream_destory(&entity->stream);
        }
        kb_destroy(wwise_bank_section_map, bank->sections);
        bank->sections = NULL;
    }
    if(bank->wem_stream_map != NULL){
        for (kb_itr_first(wwise_bank_key_stream_map, bank->wem_stream_map, &itr); kb_itr_valid(&itr); kb_itr_next(wwise_bank_key_stream_map, bank->wem_stream_map, &itr)) {
            wwise_bank_key_stream_t *entity = &kb_itr_key(wwise_bank_key_stream_t, &itr);
            binary_stream_destory(&entity->stream);
        }
        kb_destroy(wwise_bank_key_stream_map, bank->wem_stream_map);
    }
}