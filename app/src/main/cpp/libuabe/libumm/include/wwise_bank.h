#if defined (__cplusplus)
extern "C" {
#endif
#ifndef WWISE_BANK_H
#define WWISE_BANK_H
#include "kbtree.h"
#include "binary_stream.h"

typedef struct {
    char key[5];
    binary_stream_t stream;
} wwise_bank_section_t;

#define wwise_bank_section_map_cmp(a, b) (strcmp((a).key, (b).key))
KBTREE_INIT(wwise_bank_section_map, wwise_bank_section_t, wwise_bank_section_map_cmp)

typedef struct {
    uint32_t key;
    uint32_t value;
} wwise_bank_key_value_t;

#define wwise_bank_key_value_cmp(a, b) (((b).key < (a).key) - ((a).key < (b).key))

KBTREE_INIT(wwise_bank_key_value_map, wwise_bank_key_value_t, wwise_bank_key_value_cmp)

#define EVENT_MAX_ACTION_BUMBER (16)

typedef struct {
    uint32_t key;
    uint32_t size;
    uint32_t values[EVENT_MAX_ACTION_BUMBER];
} wwise_bank_key_values_t;

KBTREE_INIT(wwise_bank_key_values_map, wwise_bank_key_values_t, wwise_bank_key_value_cmp)

typedef struct {
    uint32_t key;
    uint32_t offset;
    uint32_t length;
    binary_stream_t stream;
} wwise_bank_key_stream_t;

KBTREE_INIT(wwise_bank_key_stream_map, wwise_bank_key_stream_t, wwise_bank_key_value_cmp)

typedef struct {
    kbtree_t(wwise_bank_section_map) *sections;
    kbtree_t(wwise_bank_key_value_map) *voice_wem_map;
    kbtree_t(wwise_bank_key_stream_map) *wem_stream_map;
    kbtree_t(wwise_bank_key_values_map) *event_wems_map;
    kbtree_t(wwise_bank_key_values_map) *object_voices_map;
    kbtree_t(wwise_bank_key_value_map) *action_object_map;
    kbtree_t(wwise_bank_key_values_map) *event_actions_map;
} wwise_bank_t;

int8_t wwise_bank_parser(wwise_bank_t *bank, binary_stream_t *stream, uint8_t parser_event);

int8_t wwise_bank_patch(wwise_bank_t *bank, kbtree_t(wwise_bank_key_stream_map) *patch);

int8_t wwise_bank_save(wwise_bank_t *bank, binary_stream_t *stream);

void wwise_bank_destory(wwise_bank_t *bank);

#endif
#if defined (__cplusplus)
}
#endif