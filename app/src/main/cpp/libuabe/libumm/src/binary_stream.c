#include "binary_stream.h"

#define STREAM_COPY_BUFFER_SIZE (1024)


union{  
    uint16_t value;
    uint8_t bytes[sizeof(uint16_t)];
} binary_stream_byte_order_test;

static uint8_t stream_copy_buffer[STREAM_COPY_BUFFER_SIZE];

const uint16_t uint16_mask_0 = 0x00FF;
const uint16_t uint16_mask_1 = 0xFF00;

const uint32_t uint32_mask_0 = 0x000000FF;
const uint32_t uint32_mask_1 = 0x0000FF00;
const uint32_t uint32_mask_2 = 0x00FF0000;
const uint32_t uint32_mask_3 = 0xFF000000;

const uint64_t uint64_mask_0 = 0x00000000000000FF;
const uint64_t uint64_mask_1 = 0x000000000000FF00;
const uint64_t uint64_mask_2 = 0x0000000000FF0000;
const uint64_t uint64_mask_3 = 0x00000000FF000000;
const uint64_t uint64_mask_4 = 0x000000FF00000000;
const uint64_t uint64_mask_5 = 0x0000FF0000000000;
const uint64_t uint64_mask_6 = 0x00FF000000000000;
const uint64_t uint64_mask_7 = 0xFF00000000000000;

uint16_t binary_stream_swap_order_uint16(uint16_t v){
    return ((v & uint16_mask_0) << 8) | ((v & uint16_mask_1) >> 8);
}

uint32_t binary_stream_swap_order_uint32(uint32_t v){
    return ((v & uint32_mask_0) << 24) | ((v & uint32_mask_1) << 8) | ((v & uint32_mask_2) >> 8) | ((v & uint32_mask_3) >> 24);
}

uint64_t binary_stream_swap_order_uint64(uint64_t v){
    return ((v & uint64_mask_0) << 56) | ((v & uint64_mask_1) << 40) | ((v & uint64_mask_2) << 24) | ((v & uint64_mask_3) << 8)
        | ((v & uint64_mask_4) >> 8) | ((v & uint64_mask_5) >> 24) | ((v & uint64_mask_6) >> 40) | ((v & uint64_mask_7) >> 56);
}

int8_t binary_stream_create_file(binary_stream_t *stream, const char *pathname, binary_stream_order_type_t order){
    memset(stream, 0, sizeof(binary_stream_t));
    stream->type = BINARY_STREAM_TYPE_FILE;
    stream->fd = open(pathname, O_RDWR|O_CREAT, 0644);
    if(stream->fd == -1){
        return -1;
    }
    binary_stream_set_order(stream, order);
    return 0;
}
int8_t binary_stream_create_memory(binary_stream_t *stream, uint64_t capacity, binary_stream_order_type_t order){
    memset(stream, 0, sizeof(binary_stream_t));
    stream->type = BINARY_STREAM_TYPE_MEMORY;
    stream->capacity = capacity;
    stream->data = malloc(capacity);
    if(stream->data == NULL){
        return -1;
    }
    binary_stream_set_order(stream, order);
    return 0;
}

void binary_stream_destory(binary_stream_t *stream){
    if(stream->type == BINARY_STREAM_TYPE_MEMORY && stream->data != NULL){
        free(stream->data);
    }
    if(stream->type == BINARY_STREAM_TYPE_FILE && stream->fd != -1){
        close(stream->fd);
    }
}

void binary_stream_set_order(binary_stream_t *stream, binary_stream_order_type_t order){
        binary_stream_byte_order_test.value = 0x2233;
        stream->order = order;
        stream->swap_order = 0;
        if((stream->order == BINARY_STREAM_ORDER_BIG_ENDIAN && binary_stream_byte_order_test.bytes[0] == 0x33) ||
         (stream->order == BINARY_STREAM_ORDER_LITTLE_ENDIAN && binary_stream_byte_order_test.bytes[0] == 0x22)){
            //文件字节序和主机字节序不一致，需要交换。
            stream->swap_order = 1;
        }
}

int64_t binary_stream_read(binary_stream_t *stream, void *data, uint64_t size){
    if(stream->type == BINARY_STREAM_TYPE_FILE){
        return read(stream->fd, data, size);
    } else if (stream->type == BINARY_STREAM_TYPE_MEMORY){
        if(stream->size - stream->pos < size){
            return -1;
        }
        memcpy(data, stream->data + stream->pos, size);
        stream->pos += size;
        return size;
    } else {
        return -2;
    }
}

int8_t binary_stream_read_uint8(binary_stream_t *stream, uint8_t *v){
    int64_t result = binary_stream_read(stream, v, sizeof(uint8_t));
    if(result != sizeof(uint8_t)){
        return -3;
    }
    return 0;
}

int8_t binary_stream_read_uint16(binary_stream_t *stream, uint16_t *v){
    int64_t result = binary_stream_read(stream, v, sizeof(uint16_t));
    if(result != sizeof(uint16_t)){
        return -3;
    }
    if(stream->swap_order){
        *v = binary_stream_swap_order_uint16(*v);
    }
    return 0;
}

int8_t binary_stream_read_uint32(binary_stream_t *stream, uint32_t *v){
    int64_t result = binary_stream_read(stream, v, sizeof(uint32_t));
    if(result != sizeof(uint32_t)){
        return -3;
    }
    if(stream->swap_order){
        *v = binary_stream_swap_order_uint32(*v);
    }
    return 0;
}

int8_t binary_stream_read_uint64(binary_stream_t *stream, uint64_t *v){
    int64_t result = binary_stream_read(stream, v, sizeof(uint64_t));
    if(result != sizeof(uint64_t)){
        return -3;
    }
    if(stream->swap_order){
        *v = binary_stream_swap_order_uint64(*v);
    }
    return 0;
}

int64_t binary_stream_read_string(binary_stream_t *stream, char* s){
    uint8_t c;
    int8_t result;
    uint64_t pos = 0;
    do {
        result = binary_stream_read_uint8(stream, &c);
        s[pos] = c;
        pos++;
    }while(result == 0 && c != '\0');
    if(result != 0){
        return -1;
    } else {
        return pos;
    }
}

int64_t binary_stream_write(binary_stream_t *stream, const void *data, uint64_t size){
    if(stream->type == BINARY_STREAM_TYPE_FILE){
        return write(stream->fd, data, size);
    } else if (stream->type == BINARY_STREAM_TYPE_MEMORY){
        uint64_t new_size = stream->pos + size;
        if(stream->capacity < new_size){
            uint64_t new_capacity = new_size > stream->capacity * 1.5 ? new_size : stream->capacity * 1.5;
            void* result = realloc(stream->data, new_capacity);
            if(result == NULL){
                return -1;
            }
            stream->capacity = new_capacity;
            stream->data = result;
        }
        memcpy(stream->data + stream->pos, data, size);
        stream->pos += size;
        if(stream->pos > stream->size){
            stream->size = stream->pos;
        }
        return size;
    } else {
        return -2;
    }
}

int8_t binary_stream_write_uint8(binary_stream_t *stream, uint8_t v){
    if(binary_stream_write(stream, &v, sizeof(uint8_t)) != sizeof(uint8_t)){
        return -3;
    }
    return 0;
}

int8_t binary_stream_write_uint16(binary_stream_t *stream, uint16_t v){
    if(stream->swap_order){
        v = binary_stream_swap_order_uint16(v);
    }
    if(binary_stream_write(stream, &v, sizeof(uint16_t)) != sizeof(uint16_t)){
        return -3;
    }
    return 0;
}

int8_t binary_stream_write_uint32(binary_stream_t *stream, uint32_t v){
    if(stream->swap_order){
        v = binary_stream_swap_order_uint32(v);
    }
    if(binary_stream_write(stream, &v, sizeof(uint32_t)) != sizeof(uint32_t)){
        return -3;
    }
    return 0;
}

int8_t binary_stream_write_uint64(binary_stream_t *stream, uint64_t v){
    if(stream->swap_order){
        v = binary_stream_swap_order_uint64(v);
    }
    if(binary_stream_write(stream, &v, sizeof(uint64_t)) != sizeof(uint64_t)){
        return -3;
    }
    return 0;
}

int64_t binary_stream_write_string(binary_stream_t *stream, const char *s){
    size_t len = strlen(s);
    if(binary_stream_write(stream, s, len + 1) != len + 1 ){
        return -3;
    }
    return len + 1;
}

int8_t binary_stream_copy(binary_stream_t *det, binary_stream_t *src, uint64_t len){
    uint64_t size, result;
    while(len > 0){
        size = len > STREAM_COPY_BUFFER_SIZE ? STREAM_COPY_BUFFER_SIZE : len;
        if((result = binary_stream_read(src, stream_copy_buffer, size)) <= 0){
            return -1;
        }
        if(binary_stream_write(det, stream_copy_buffer, result) != result){
            return -2;
        }
        len -= result;
    }
    return 0;
}

int64_t binary_stream_seek(binary_stream_t *stream, off_t offset, int whence){
    if(stream->type == BINARY_STREAM_TYPE_FILE){
        return lseek(stream->fd, offset, whence);
    } else if (stream->type == BINARY_STREAM_TYPE_MEMORY){
        switch(whence){
            case SEEK_SET:
                stream->pos = 0 + offset;
                break;
            case SEEK_CUR:
                stream->pos = stream->pos + offset;
                break;
            case SEEK_END:
                stream->pos = stream->size + offset;
                break;
        }
        return stream->pos;
    } else {
        return -1;
    }
}