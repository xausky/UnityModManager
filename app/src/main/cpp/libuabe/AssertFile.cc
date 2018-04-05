#include "AssertFile.hh"
#include "LogUtils.hh"
#include <iostream>
namespace xausky {
    typedef struct {
        BinaryStream* mod;
        int32_t size;
        int32_t offset;
    }patch_t;
    void AssertFile::patch(BinaryStream& input, BinaryStream& output, map<int64_t, BinaryStream*>& mods){
        int attachesCount = 0;
        for(map<int64_t, BinaryStream*>::iterator it = mods.begin(); it != mods.end(); ++it) {
            BinaryStream *stream = it->second;
            if (stream->classId != -1) {
                ++attachesCount;
            }
        }
        input.position(0, ios::beg);
        int32_t tableSize = input.ReadInt32();
        output.WriteInt32(tableSize + attachesCount * 20);
        int32_t dataEnd = input.ReadInt32();
        output.WriteInt32(dataEnd);
        int32_t fileGen = input.ReadInt32();
        output.WriteInt32(fileGen);
        int32_t dataOffset = input.ReadInt32();
        int32_t newDataOffset = dataOffset + attachesCount * 20;
        int mod = newDataOffset%8;
        if(mod != 0){
            newDataOffset += 8 - mod;
        }
        __LIBUABE_DEBUG("dataOffset:%ld, newDataOffset:%ld\n", dataOffset, newDataOffset);
        output.WriteInt32(newDataOffset);
        input.position(4, ios::cur);
        output.WriteConst(0, 4);
        string version = input.ReadStringToNull();
        output.WriteStringToNull(version);
        int64_t baseOffset = input.position();

        //后面的字节序是小端
        input.setEndian(false);
        output.setEndian(false);
        int32_t platform = input.ReadInt32();
        bool definitions = input.ReadBoolean();

        map<int32_t , int32_t > classIdMap;
        int32_t baseCount = input.ReadInt32();
        for(int i=0; i<baseCount; ++i){
            int32_t classId = input.ReadInt32();
            classIdMap[classId] = i;
            if(classId == 114){
                input.position(16, ios::cur);
            }
            input.position(19, ios::cur);
            if (definitions){
                int varCount = input.ReadInt32();
                int stringSize = input.ReadInt32();
                for (int i = 0; i < varCount; i++){
                    int16_t num = input.ReadInt16();
                    char level = input.ReadByte();
                    bool isArray = input.ReadBoolean();
                    int16_t typeIndex = input.ReadInt16();
                    int16_t test = input.ReadInt16();
                    int16_t nameIndex = input.ReadInt16();
                    test = input.ReadInt16();
                    int size = input.ReadInt32();
                    int index = input.ReadInt32();
                    int flag = input.ReadInt32();
                }
                input.position(stringSize, ios::cur);
            }
        }
        int baseSize = input.position() - baseOffset;
        char* base = new char[baseSize];
        input.position(baseOffset, ios::beg);
        input.ReadData(base, baseSize);
        output.WriteData(base, baseSize);
        delete[] base;
        int assetCount = input.ReadInt32();
        output.WriteInt32(assetCount + attachesCount);
        input.AlignStream(4);
        output.AlignStreamOutput(4);
        patch_t patchs[assetCount];
        int64_t patchOffset = dataOffset;
        for (int i = 0; i < assetCount; i++){
            int64_t pathId = input.ReadInt64();
            int32_t offset = input.ReadInt32() + dataOffset;
            int32_t size = input.ReadInt32();
            int32_t index = input.ReadInt32();
            __LIBUABE_DEBUG("pathid:%lld, offset:%ld, size:%ld, index:%ld\n", pathId, offset, size, index);
            map<int64_t, BinaryStream*>::iterator it = mods.find(pathId);
            patchs[i].size = size;
            patchs[i].offset = offset;
            patchs[i].mod = NULL;
            if(it != mods.end()){
                patchs[i].mod = it->second;
                size = patchs[i].mod->size();
            }
            output.WriteInt64(pathId);
            output.WriteInt32(patchOffset - dataOffset);
            output.WriteInt32(size);
            output.WriteInt32(index);
            patchOffset+=size;
            int mod = size%8;
            if(mod != 0){
                patchOffset += 8 - mod;
            }
        }
        patch_t attaches[mods.size()];
        attachesCount = 0;
        for(map<int64_t, BinaryStream*>::iterator it = mods.begin(); it != mods.end(); ++it){
            BinaryStream* stream = it->second;
            __LIBUABE_DEBUG("class id: %d\n", stream->classId);
            if(stream->classId != -1){
                map<int32_t , int32_t >::iterator classIterator = classIdMap.find(stream->classId);
                if(classIterator != classIdMap.end()){
                    __LIBUABE_DEBUG("class id found: %d\n", classIterator->second);
                    attaches[attachesCount].size = stream->size();
                    attaches[attachesCount].mod = stream;
                    output.WriteInt64(it->first);
                    output.WriteInt32(patchOffset - dataOffset);
                    output.WriteInt32(attaches[attachesCount].size);
                    output.WriteInt32(classIterator->second);
                    patchOffset += attaches[attachesCount].size;
                    int mod = attaches[attachesCount].size%8;
                    if(mod != 0){
                        patchOffset += 8 - mod;
                    }
                    ++attachesCount;
                } else {
                    __LIBUABE_LOG("class id not found: %d\n", stream->classId);
                }
            }
        }
        int64_t extraOffset = output.positionOutput();
        int32_t extraSize = (int32_t)(newDataOffset - extraOffset);
        char* extra = new char[extraSize];
        input.ReadData(extra, extraSize);
        output.WriteData(extra, extraSize);
        delete[] extra;
        for(int i = 0; i < assetCount; i++){
            patch_t patch = patchs[i];
            int32_t len;
            char* data;
            if(patch.mod != NULL){
                len =  (int32_t)patch.mod->size();
                data = new char[len];
                patch.mod->ReadData(data, len);
            } else {
                len = patch.size;
                data = new char[len];
                input.position(patch.offset, ios::beg);
                input.ReadData(data, len);
            }
            output.WriteData(data, len);
            output.AlignStreamOutput(8);
            delete[] data;
        }
        for (int i = 0; i < attachesCount; ++i) {
            patch_t patch = attaches[i];
            int32_t len =  (int32_t)patch.mod->size();
            char*  data = new char[len];
            patch.mod->ReadData(data, len);
            output.WriteData(data, len);
            output.AlignStreamOutput(8);
            delete[] data;
        }
        //更新dataEnd
        output.setEndian(true);
        dataEnd = (int32_t)output.size();
        output.positionOutput(4, ios::beg);
        output.WriteInt32(dataEnd);
    }
}