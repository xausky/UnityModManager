#include "AssertFile.hh"
#include <iostream>
namespace xausky {
    typedef struct {
        BinaryStream* mod;
        int32_t size;
        int32_t offset;
    }patch_t;
    void AssertFile::patch(BinaryStream& input, BinaryStream& output, map<int64_t, BinaryStream*>& mods){
        input.position(0, ios::beg);
        int32_t tableSize = input.ReadInt32();
        int32_t dataEnd = input.ReadInt32();
        int32_t fileGen = input.ReadInt32();
        int32_t dataOffset = input.ReadInt32();
        input.position(4, ios::cur);
        string version = input.ReadStringToNull();
        int64_t baseOffset = input.position();
        input.position(0, ios::beg);
        char* header = new char[baseOffset];
        input.ReadData(header, baseOffset);
        output.WriteData(header, baseOffset);
        delete[] header;
        //后面的字节序是小端
        input.setEndian(false);
        output.setEndian(false);
        int32_t platform = input.ReadInt32();
        bool definitions = input.ReadBoolean();
        
        int32_t baseCount = input.ReadInt32();
        for(int i=0; i<baseCount; ++i){
            int32_t classId = input.ReadInt32();
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
        int64_t baseSize = input.position() - baseOffset;
        char* base = new char[baseSize];
        input.position(baseOffset, ios::beg);
        input.ReadData(base, baseSize);
        output.WriteData(base, baseSize);
        delete[] base;
        int assetCount = input.ReadInt32();
        output.WriteInt32(assetCount);
        input.AlignStream(4);
        output.AlignStreamOutput(4);
        patch_t patchs[assetCount];
        int64_t patchOffset = dataOffset;
        for (int i = 0; i < assetCount; i++){
            int64_t pathId = input.ReadInt64();
            int32_t offset = input.ReadInt32() + dataOffset;
            int32_t size = input.ReadInt32();
            int32_t index = input.ReadInt32();
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
        int64_t extraOffset = input.position();
        int64_t extraSize = dataOffset - extraOffset;
        char* extra = new char[extraSize];
        input.ReadData(extra, extraSize);
        output.WriteData(extra, extraSize);
        delete[] extra;
        for(int i = 0; i < assetCount; i++){
            patch_t patch = patchs[i];
            int64_t len;
            char* data;
            if(patch.mod != NULL){
                len =  patch.mod->size();
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
        //更新dataEnd
        output.setEndian(true);
        dataEnd = output.size();
        output.positionOutput(4, ios::beg);
        output.WriteInt32(dataEnd);
    }
}