#if defined (__cplusplus)
extern "C" {
#endif
#ifndef BINARY_STREAM_H
#define BINARY_STREAM_H
#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>

typedef enum {
    BINARY_STREAM_TYPE_FILE = 1, BINARY_STREAM_TYPE_MEMORY = 2
} binary_stream_type_t;

typedef enum {
    BINARY_STREAM_ORDER_LITTLE_ENDIAN = 1, BINARY_STREAM_ORDER_BIG_ENDIAN = 2
} binary_stream_order_type_t;

typedef struct {
    int fd;
    uint8_t *data;
    uint64_t pos;
    uint64_t size;
    uint64_t capacity;
    uint8_t swap_order;
    binary_stream_type_t type;
    binary_stream_order_type_t order;
} binary_stream_t;

static uint16_t binary_stream_swap_order_uint16(uint16_t v);
static uint32_t binary_stream_swap_order_uint32(uint32_t v);
static uint64_t binary_stream_swap_order_uint64(uint64_t v);

int8_t binary_stream_create_file(binary_stream_t *stream, const char *pathname,
                                 binary_stream_order_type_t order);
int8_t binary_stream_create_memory(binary_stream_t *stream, uint64_t capacity,
                                   binary_stream_order_type_t order);

void binary_stream_destory(binary_stream_t *stream);

void binary_stream_set_order(binary_stream_t *stream, binary_stream_order_type_t order);

int8_t binary_stream_read_uint8(binary_stream_t *stream, uint8_t *v);
int8_t binary_stream_read_uint16(binary_stream_t *stream, uint16_t *v);
int8_t binary_stream_read_uint32(binary_stream_t *stream, uint32_t *v);
int8_t binary_stream_read_uint64(binary_stream_t *stream, uint64_t *v);
int64_t binary_stream_read(binary_stream_t *stream, void *data, uint64_t size);
int64_t binary_stream_read_string(binary_stream_t *stream, char *s);

int8_t binary_stream_write_uint8(binary_stream_t *stream, uint8_t v);
int8_t binary_stream_write_uint16(binary_stream_t *stream, uint16_t v);
int8_t binary_stream_write_uint32(binary_stream_t *stream, uint32_t v);
int8_t binary_stream_write_uint64(binary_stream_t *stream, uint64_t v);
int64_t binary_stream_write(binary_stream_t *stream, const void *data, uint64_t size);
int64_t binary_stream_write_string(binary_stream_t *stream, const char *s);

int8_t binary_stream_copy(binary_stream_t *det, binary_stream_t *src, uint64_t len);
int64_t binary_stream_seek(binary_stream_t *stream, off_t offset, int whence);

#endif
#if defined (__cplusplus)
}
#endif