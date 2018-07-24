#if defined (__cplusplus)
extern "C" {
#endif
#ifndef WWISE_AKPK_H
#define WWISE_AKPK_H
#include "kbtree.h"
#include "wwise_bank.h"
#include "binary_stream.h"

#define WWISE_AKPK_EVENT_MAX_WEM (32)

typedef struct {
    uint32_t index;
    uint32_t wem_id;
    uint32_t bank_id;
} wwise_akpk_wem_match_t;

typedef struct {
    uint32_t key;
    uint32_t size;
    wwise_akpk_wem_match_t wems[WWISE_AKPK_EVENT_MAX_WEM];
} wwise_akpk_event_wems_map_t;

#define wwise_akpk_key_map_cmp(a, b) (((b).key < (a).key) - ((a).key < (b).key))

KBTREE_INIT(wwise_akpk_event_wems_map, wwise_akpk_event_wems_map_t, wwise_akpk_key_map_cmp)

typedef struct {
    uint64_t key;
    uint32_t id;
    uint32_t align;
    uint32_t size;
    uint32_t offset;
    uint32_t index;
    uint32_t unknown;
    wwise_bank_t sound;
    binary_stream_t data;
} wwise_akpk_key_bank_map_t;

KBTREE_INIT(wwise_akpk_key_bank_map, wwise_akpk_key_bank_map_t, wwise_akpk_key_map_cmp)

#define WWISE_AKPK_BANK_MAX (1024)

typedef struct {
    uint32_t info;
    uint32_t version;
    uint32_t name_size;
    char indentifier[5];
    binary_stream_t names;
    uint32_t pos;
    uint64_t sort[WWISE_AKPK_BANK_MAX];
    kbtree_t(wwise_akpk_key_bank_map) *banks[3];
    kbtree_t(wwise_akpk_event_wems_map) *event_wems;
} wwise_akpk_t;

typedef struct {
    uint64_t key;
    kbtree_t(wwise_bank_key_stream_map) *patch;
} wwise_akpk_patch_element_t;

KBTREE_INIT(wwise_akpk_patch_map, wwise_akpk_patch_element_t, wwise_akpk_key_map_cmp) ;

typedef struct {
    kbtree_t(wwise_akpk_patch_map) *patch;
} wwise_akpk_patch_t;

int8_t wwise_akpk_parser(wwise_akpk_t *akpk, binary_stream_t *stream, uint8_t parser_event);

int8_t wwise_akpk_patch(wwise_akpk_t *akpk, wwise_akpk_patch_t *patch);

int8_t wwise_akpk_save(wwise_akpk_t *akpk, binary_stream_t *stream);

int8_t wwise_akpk_make_patch(wwise_akpk_patch_t *patch, const char *path);

void wwise_akpk_destory(wwise_akpk_t *akpk);

void wwise_akpk_destory_patch(wwise_akpk_patch_t *patch);

#endif
#if defined (__cplusplus)
}
#endif