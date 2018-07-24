#include "ZipFile.hh"
#include "miniz/miniz.h"
#include "BinaryStream.hh"
#include "BundleFile.hh"
#include "Utils.hh"
#include "LogUtils.hh"
#include "libumm/wwise_akpk.h"

#include <sys/stat.h>

namespace xausky {
    char pathBuffer[PATH_BUFFER_SIZE];
    char dataBuffer[DATA_BUFFER_SIZE];
    void PatchFile(mz_zip_archive* out, mz_zip_archive* zip, mz_uint i, mz_zip_archive_file_stat* entrityStat, const char *mods){
        struct stat modStat;
        string filename = entrityStat->m_filename;
        size_t pos = filename.find_last_of('_');
        if(pos != string::npos){
            filename = filename.substr(0, pos);
        }
        sprintf(pathBuffer, "%s/%s", mods, filename.c_str());
        if(stat(pathBuffer,&modStat)==0){
            if(S_ISDIR(modStat.st_mode)){
                if(mz_zip_reader_extract_to_mem(zip, i, dataBuffer, DATA_BUFFER_SIZE, 0)){
                    if(memcmp(dataBuffer, "AKPK", 4) == 0){
                        __LIBUABE_LOG("AKPK patch: %s\n", entrityStat->m_filename);
                        binary_stream_t target;
                        if(binary_stream_create_memory(&target, entrityStat->m_uncomp_size, BINARY_STREAM_ORDER_LITTLE_ENDIAN) != 0){
                            __LIBUABE_LOG("binary_stream_create_memory failed.");
                            return;
                        }
                        if(binary_stream_write(&target, dataBuffer, entrityStat->m_uncomp_size) != entrityStat->m_uncomp_size){
                            __LIBUABE_LOG("binary_stream_write failed.");
                            return;
                        }
                        wwise_akpk_t akpk;
                        wwise_akpk_patch_t patch;
                        binary_stream_seek(&target, 0, SEEK_SET);
                        if(wwise_akpk_parser(&akpk, &target, 0) != 0){
                            __LIBUABE_LOG("wwise_akpk_parser failed.");
                            return;
                        }
                        if(wwise_akpk_make_patch(&patch, pathBuffer) != 0){
                            __LIBUABE_LOG("wwise_akpk_make_patch failed.");
                            return;
                        }
                        if(wwise_akpk_patch(&akpk, &patch)){
                            __LIBUABE_LOG("wwise_akpk_patch failed.");
                            return;
                        }
                        if(binary_stream_seek(&target, 0, SEEK_SET) != 0){
                            __LIBUABE_LOG("binary_stream_seek failed.");
                            return;
                        }
                        if(wwise_akpk_save(&akpk, &target) != 0) {
                            __LIBUABE_LOG("wwise_akpk_save failed.");
                            return;
                        }
                        int64_t size;
                        if((size = binary_stream_seek(&target, 0, SEEK_END)) < 0){
                            __LIBUABE_LOG("binary_stream_seek failed.");
                            return;
                        }
                        if((binary_stream_seek(&target, 0, SEEK_SET)) < 0){
                            __LIBUABE_LOG("binary_stream_seek failed.");
                            return;
                        }
                        if(binary_stream_read(&target, dataBuffer, size) != size){
                            __LIBUABE_LOG("binary_stream_read failed.");
                            return;
                        }
                        binary_stream_destory(&target);
                        wwise_akpk_destory_patch(&patch);
                        mz_zip_writer_add_mem(out, entrityStat->m_filename, dataBuffer, size, MZ_NO_COMPRESSION);
                    } else {
                        __LIBUABE_LOG("bundle patch: %s\n", entrityStat->m_filename);
                        BinaryStream bundle(dataBuffer, entrityStat->m_uncomp_size, true);
                        BinaryStream patchedBundle(true);
                        BundleFile bundleFile;
                        bundleFile.open(bundle);
                        map<string, map<int64_t, BinaryStream *> *> *patch = Utils::MakeBundlePatch(pathBuffer);
                        bundleFile.patch(*patch);
                        bundleFile.save(patchedBundle);
                        Utils::FreeBundlePatch(patch);
                        int len = patchedBundle.size();
                        patchedBundle.ReadData(dataBuffer, len);
                        mz_zip_writer_add_mem(out, entrityStat->m_filename, dataBuffer, len, MZ_NO_COMPRESSION);
                    }
                    return;
                } else {
                    __LIBUABE_LOG("mz_zip_reader_extract_to_mem() failed!\n");
                }
            }
            if(S_ISREG(modStat.st_mode)){
                mz_zip_writer_add_file(out, entrityStat->m_filename, pathBuffer, NULL, 0, MZ_NO_COMPRESSION);
                __LIBUABE_LOG("copy patch: %s\n", entrityStat->m_filename);
                return;
            }
        }
        mz_zip_writer_add_from_zip_reader(out, zip,i);
    }
    int ZipFile::Patch(string zipPath, string modsPath, string outPath){
        mz_zip_archive zip;
        mz_zip_archive out;
        memset(&zip, 0, sizeof(mz_zip_archive));
        memset(&out, 0, sizeof(mz_zip_archive));
        const char * modsPathString = modsPath.c_str();
        if (!mz_zip_reader_init_file(&zip, zipPath.c_str(), 0))
        {
            __LIBUABE_LOG("mz_zip_reader_init_file() failed!\n");
            return RESULT_STATE_INTERNAL_ERROR;
        }
        if (!mz_zip_writer_init_file(&out, outPath.c_str(), 0)){
            __LIBUABE_LOG("mz_zip_writer_init_file() failed:%s\n", outPath.c_str());
            mz_zip_reader_end(&zip);
            return RESULT_STATE_INTERNAL_ERROR;
        }
        for (mz_uint i = 0; i < mz_zip_reader_get_num_files(&zip); i++)
        {
            if(mz_zip_reader_is_file_a_directory(&zip, i)){
                mz_zip_writer_add_from_zip_reader(&out, &zip,i);
            } else {
                mz_zip_archive_file_stat stat;
                if (!mz_zip_reader_file_stat(&zip, i, &stat))
                {
                    __LIBUABE_LOG("mz_zip_reader_file_stat() failed!\n");
                    mz_zip_reader_end(&zip);
                    mz_zip_writer_end(&out);
                    return RESULT_STATE_INTERNAL_ERROR;
                }
                PatchFile(&out, &zip, i, &stat, modsPathString);
            }
        }
        mz_zip_reader_end(&zip);
        mz_zip_writer_finalize_archive(&out);
        mz_zip_writer_end(&out);
        return RESULT_STATE_OK;
    }
    int ZipFile::GenerateMapFile(string zipPath, string outPath){
        mz_zip_archive zip;
        memset(&zip, 0, sizeof(mz_zip_archive));
        FILE* out = fopen(outPath.c_str(), "w");
        if(out == NULL){
            __LIBUABE_LOG("map file output open failed!\n");
            return RESULT_STATE_INTERNAL_ERROR;
        }
        if (!mz_zip_reader_init_file(&zip, zipPath.c_str(), 0))
        {
            __LIBUABE_LOG("mz_zip_reader_init_file() failed!\n");
            return RESULT_STATE_INTERNAL_ERROR;
        }
        for (mz_uint i = 0; i < mz_zip_reader_get_num_files(&zip); i++)
        {
            if(!mz_zip_reader_is_file_a_directory(&zip, i)){
                mz_zip_archive_file_stat stat;
                if (!mz_zip_reader_file_stat(&zip, i, &stat))
                {
                    __LIBUABE_LOG("mz_zip_reader_file_stat() failed!\n");
                    mz_zip_reader_end(&zip);
                    fclose(out);
                    return RESULT_STATE_INTERNAL_ERROR;
                }
                string filename = stat.m_filename;
                size_t pos = filename.find_last_of('_');
                if(pos != string::npos){
                    filename = filename.substr(0, pos);
                }
                string key = filename;
                pos = filename.find_last_of('/');
                if(pos != string::npos){
                    pos = filename.find_last_of('/',pos - 1);
                    if(pos != string::npos){
                        key = filename.substr(pos + 1);
                    }
                }
                fprintf(out, "%s:%s\n", key.c_str(), filename.c_str());
            }
        }
        mz_zip_reader_end(&zip);
        fclose(out);
        return RESULT_STATE_OK;
    }
}