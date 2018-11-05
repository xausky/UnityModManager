#include <getopt.h>

#include "wwise_akpk.h"

int main(int argc, char* argv[]){
    int opt=0;
    char buffer[2048];
    const char * bankPath = NULL;
    const char * pckPath = NULL;
    const char * outputPath = NULL;
    wwise_akpk_t akpk;
    binary_stream_t stream;
    while((opt=getopt(argc,argv,"b:p:o:"))!=-1)
	{
		switch(opt)
		{
			case 'b':
            bankPath = optarg;
				break;
			case 'p':
            pckPath = optarg;
				break;
            case 'o':
            outputPath = optarg;
            break;
		}
	}
    if(outputPath != NULL && pckPath != NULL){
        sprintf(buffer, "%s.map.txt", outputPath);
        FILE* map = fopen(buffer, "w+");
        if(binary_stream_create_file(&stream, pckPath, BINARY_STREAM_ORDER_LITTLE_ENDIAN) < 0){
            fclose(map);
            printf("binary_stream_create_file error: %s\n", pckPath);
            return -1;
        }
        if(wwise_akpk_parser(&akpk, &stream, 1) < 0){
            fclose(map);
            binary_stream_destory(&stream);
            printf("wwise_akpk_parser error: %s\n", pckPath);
            return -1;
        }
        kbitr_t wwise_akpk_itr;
        kbitr_t wem_stream_itr;
        for(int i = 0; i < 3; ++i){
            for (kb_itr_first(wwise_akpk_key_bank_map, akpk.banks[i], &wwise_akpk_itr); kb_itr_valid(&wwise_akpk_itr); kb_itr_next(wwise_akpk_key_bank_map, akpk.banks[i], &wwise_akpk_itr)) {
                wwise_akpk_key_bank_map_t *bank = &kb_itr_key(wwise_akpk_key_bank_map_t, &wwise_akpk_itr);
                if(bank->sound.wem_stream_map != NULL){
                    for(kb_itr_first(wwise_bank_key_stream_map, bank->sound.wem_stream_map, &wem_stream_itr);kb_itr_valid(&wem_stream_itr);kb_itr_next(wwise_bank_key_stream_map, bank->sound.wem_stream_map, &wem_stream_itr)){
                        wwise_bank_key_stream_t *wem = &kb_itr_key(wwise_bank_key_stream_t, &wem_stream_itr);
                        fprintf(map, "%u-%u-%u.wem %u\n", bank->id, bank->index, wem->key, wem->length);
                        sprintf(buffer, "%s/%u-%u-%u.wem", outputPath, bank->id, bank->index, wem->key);
                        binary_stream_t output;
                        if(binary_stream_create_file(&output, buffer, BINARY_STREAM_ORDER_LITTLE_ENDIAN) < 0){
                            printf("create file error: %s\n", buffer);
                            continue;
                        }
                        if(binary_stream_seek(&wem->stream, 0, SEEK_SET) < 0){
                            binary_stream_destory(&output);
                            printf("wem seek error: %u\n", wem->key);
                            continue;
                        }
                        if(binary_stream_copy(&output, &wem->stream, wem->length) < 0){
                            binary_stream_destory(&output);
                            printf("wem copy error: %u\n", wem->key);
                            continue;
                        }
                        binary_stream_destory(&output);
                    }
                }
                if(bank->sound.event_wems_map != NULL){
                    for(kb_itr_first(wwise_bank_key_values_map, bank->sound.event_wems_map, &wem_stream_itr);kb_itr_valid(&wem_stream_itr);kb_itr_next(wwise_bank_key_values_map, bank->sound.event_wems_map, &wem_stream_itr)){
                        wwise_bank_key_values_t *wems = &kb_itr_key(wwise_bank_key_values_t, &wem_stream_itr);
                        fprintf(map, "Event[%u]:\n", wems->key);
                        for(int p = 0; p < wems->size; ++p){
                            fprintf(map, "\t%u-%u-%u.wem\n", bank->id, bank->index, wems->values[p]);
                        }
                    }
                }
                if(bank->sound.sections == NULL){
                    fprintf(map, "bank-%u-%u.wem %u\n", bank->id, bank->index);
                    sprintf(buffer, "%s/bank-%u-%u.wem", outputPath, bank->id, bank->index);
                    binary_stream_t output;
                    if(binary_stream_create_file(&output, buffer, BINARY_STREAM_ORDER_LITTLE_ENDIAN) < 0){
                        printf("create file error: %s\n", buffer);
                        continue;
                    }
                    if(binary_stream_seek(&bank->data, 0, SEEK_SET) < 0){
                        binary_stream_destory(&output);
                        printf("bank seek error: %u\n", bank->id);
                        continue;
                    }
                    if(binary_stream_copy(&output, &bank->data, bank->size) < 0){
                        binary_stream_destory(&output);
                        printf("bank copy error: %u\n", bank->id);
                        continue;
                    }
                    binary_stream_destory(&output);
                }
            }
        }
        wwise_akpk_destory(&akpk);
        binary_stream_destory(&stream);
        fclose(map);
    }
    if(outputPath != NULL && bankPath != NULL){
        wwise_bank_t bank;
        if(binary_stream_create_file(&stream, bankPath, BINARY_STREAM_ORDER_LITTLE_ENDIAN) < 0){
            printf("binary_stream_create_file error: %s\n", pckPath);
            return -1;
        }
        if(wwise_bank_parser(&bank, &stream, 1) < 0){
            binary_stream_destory(&stream);
            printf("wwise_bank_parser error: %s\n", bankPath);
            return -1;
        }
        kbitr_t wem_stream_itr;
        if(bank.wem_stream_map != NULL){
            for(kb_itr_first(wwise_bank_key_stream_map, bank.wem_stream_map, &wem_stream_itr);kb_itr_valid(&wem_stream_itr);kb_itr_next(wwise_bank_key_stream_map, bank.wem_stream_map, &wem_stream_itr)){
                wwise_bank_key_stream_t *wem = &kb_itr_key(wwise_bank_key_stream_t, &wem_stream_itr);
                sprintf(buffer, "%s/%u.wem", outputPath, wem->key);
                binary_stream_t output;
                if(binary_stream_create_file(&output, buffer, BINARY_STREAM_ORDER_LITTLE_ENDIAN) < 0){
                    printf("create file error: %s\n", buffer);
                    continue;
                }
                if(binary_stream_seek(&wem->stream, 0, SEEK_SET) < 0){
                    binary_stream_destory(&output);
                    printf("wem seek error: %u\n", wem->key);
                    continue;
                }
                if(binary_stream_copy(&output, &wem->stream, wem->length) < 0){
                    binary_stream_destory(&output);
                    printf("wem copy error: %u\n", wem->key);
                    continue;
                }
                binary_stream_destory(&output);
            }
        }
        wwise_bank_destory(&bank);
        binary_stream_destory(&stream);
    }
    return 0;
}
