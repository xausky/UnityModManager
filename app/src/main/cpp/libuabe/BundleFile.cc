#include "BundleFile.hh"
#include "lz4/lz4hc.h"
#include "Utils.hh"
#include "AssertFile.hh"
#include <iostream>
namespace xausky {
    void BundleFile::open(BinaryStream& bundleReader){
        fileType = bundleReader.ReadStringToNull();
        if(fileType.compare("UnityFS") != 0){
            UnsupprtBundleFileType e;
            throw e;
        }
        fileVersion = bundleReader.ReadInt32();
        versionPlayer = bundleReader.ReadStringToNull();
        versionEngine = bundleReader.ReadStringToNull();
        if(fileVersion != 6){
            UnsupprtBundleFileType e;
            throw e;
        }
        int64_t bundleSize = bundleReader.ReadInt64();
        int32_t compressedSize = bundleReader.ReadInt32();
        int32_t uncompressedSize = bundleReader.ReadInt32();
        int32_t flag = bundleReader.ReadInt32();
        if(getCompressMethod(flag) != CompressMethod::LZ4HC){
            UnsupprtCompressMethod e;
            throw e;
        }
        char compressedData[compressedSize];
        char uncompressedData[uncompressedSize];
        if(infoBlocksIsAtOfFileEnd(flag)){
            uint64_t pos = bundleReader.position();
            bundleReader.position(-compressedSize, ios_base::end);
            bundleReader.ReadData(compressedData, compressedSize);
            bundleReader.position(pos, ios_base::beg);
        } else {
            bundleReader.ReadData(compressedData, compressedSize);
        }
        if((LZ4_decompress_safe(compressedData, uncompressedData, compressedSize, uncompressedSize)) != uncompressedSize){
            DecompressDataException e;
            throw e;
        }
        BinaryStream blocksReader(uncompressedData, uncompressedSize, true);
        BinaryStream assertDataStream(true);
        blocksReader.position(0x10, ios_base::beg);
        int32_t blockCount = blocksReader.ReadInt32();
        for(int i=0; i<blockCount; ++i){
            uncompressedSize = blocksReader.ReadInt32();
            compressedSize = blocksReader.ReadInt32();
            int16_t flag = blocksReader.ReadInt16();
            char compressedData[compressedSize];
            char uncompressedData[uncompressedSize];
            bundleReader.ReadData(compressedData, compressedSize);
            int result = 0;
            if((result = LZ4_decompress_safe(compressedData, uncompressedData, compressedSize, uncompressedSize)) != uncompressedSize){
                DecompressDataException e;
                throw e;
            }
            assertDataStream.WriteData(uncompressedData, uncompressedSize);            
        }
        int32_t entryCount = blocksReader.ReadInt32();
        for(int i=0; i<entryCount; ++i){
            int64_t offset = blocksReader.ReadInt64();
            int64_t size = blocksReader.ReadInt64();
            int32_t flag = blocksReader.ReadInt32();
            string name = blocksReader.ReadStringToNull();
            char* data = new char[size];
            assertDataStream.position(offset, ios_base::beg);
            assertDataStream.ReadData(data, size);
            BinaryStream* fileStream = new BinaryStream(data, size, true);
            delete[] data;
            MemoryFile* file = new MemoryFile(name, fileStream);
            files.push_back(file);
        }
    }
    void BundleFile::save(BinaryStream& bundleStream, int32_t maxBlockSize, int32_t bundleFlag, int16_t blocksFlag, int32_t fileFlag){
        char uncompressedData[maxBlockSize], compressedData[maxBlockSize];
        bundleStream.WriteStringToNull(fileType);
        bundleStream.WriteInt32(fileVersion);
        bundleStream.WriteStringToNull(versionPlayer);
        bundleStream.WriteStringToNull(versionEngine);
        BinaryStream blocksStream(true);
        BinaryStream assertStream(true);
        blocksStream.WriteConst(0, 0x10);
        int sizeCount = 0;
        for(std::list<MemoryFile*>::iterator it = files.begin(); it != files.end(); it++){
            sizeCount+=(*it)->stream->size();
        }
        int blockCount = (sizeCount - 1)/maxBlockSize + 1;
        blocksStream.WriteInt32(blockCount);
        int uncompressedSize = 0;
        int compressedSize = 0;
        int position = 0;
        for(std::list<MemoryFile*>::iterator it = files.begin(); it != files.end(); it++){
            MemoryFile* file = (*it);
            file->offset = position;
            do{
                file->stream->ReadData(uncompressedData, maxBlockSize);
                uncompressedSize = file->stream->count();
                if(uncompressedSize > 0){
                    blocksStream.WriteInt32(uncompressedSize);
                    compressedSize =  LZ4_compress_HC(uncompressedData, compressedData, uncompressedSize, maxBlockSize, LZ4HC_CLEVEL_DEFAULT);
                    if(compressedSize <= 0){
                        DecompressDataException e;
                        throw e;
                    }
                    blocksStream.WriteInt32(compressedSize);
                    blocksStream.WriteInt16(blocksFlag);
                    assertStream.WriteData(compressedData, compressedSize);
                    position += uncompressedSize;
                }
            }while(uncompressedSize > 0 && !file->stream->eof());
            file->size = position - file->offset;
        }
        blocksStream.WriteInt32(files.size());
        for(std::list<MemoryFile*>::iterator it = files.begin(); it != files.end(); it++){
            MemoryFile* file = (*it);
            blocksStream.WriteInt64(file->offset);
            blocksStream.WriteInt64(file->size);
            blocksStream.WriteInt32(fileFlag);
            blocksStream.WriteStringToNull(file->name);
        }
        uncompressedSize = blocksStream.size();
        char buffer[uncompressedSize], compressedBuffer[uncompressedSize];
        blocksStream.ReadData(buffer, uncompressedSize);
        compressedSize = LZ4_compress_HC(buffer, compressedBuffer, uncompressedSize, uncompressedSize, LZ4HC_CLEVEL_DEFAULT);
        if(compressedSize <= 0){
            DecompressDataException e;
            throw e;
        }
        bundleStream.WriteInt64(bundleStream.size() + compressedSize + assertStream.size() + 12);
        //这里加的12是下面3个int32的大小
        bundleStream.WriteInt32(compressedSize);
        bundleStream.WriteInt32(uncompressedSize);
        bundleStream.WriteInt32(bundleFlag);
        bundleStream.WriteData(compressedBuffer, compressedSize);
        assertStream.WriteTo(bundleStream);
    }

    void BundleFile::patch(map<string, map<int64_t, BinaryStream*>*> &mods){
        for (std::list<MemoryFile*>::iterator it = files.begin(); it != files.end(); it++){
            MemoryFile* file = (*it);
            map<string, map<int64_t, BinaryStream*>*>::iterator modIterator = mods.find(file->name);
            map<int64_t, BinaryStream*>* mod = nullptr;
            if(modIterator != mods.end()){
                mod = modIterator->second;
            }
            if(mod != nullptr){
                BinaryStream* output = new BinaryStream(true);
                AssertFile::patch(*(file->stream), *output, *mod);
                delete file->stream;
                file->stream = output;
            }
        }
    }

    CompressMethod BundleFile::getCompressMethod(int32_t flag){
        return CompressMethod(flag & 0x3F);
    }

    bool BundleFile::infoBlocksIsAtOfFileEnd(int32_t flag){
        return (flag & 0x80) != 0;
    }

    BundleFile::~BundleFile(){
        for (std::list<MemoryFile*>::iterator it = files.begin(); it != files.end(); it++){
            delete (*it)->stream;
            delete (*it);
        }
    }
}