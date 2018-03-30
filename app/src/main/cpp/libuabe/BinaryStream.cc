#include "BinaryStream.hh"
#include <sstream>
namespace xausky {
    union{  
        uint16_t value;
        uint8_t bytes[sizeof(uint16_t)];
    }ByteOrderTest;
    const uint16_t int16_mask_0 = 0x00FF;
    const uint16_t int16_mask_1 = 0xFF00;

    const uint32_t int32_mask_0 = 0x000000FF;
    const uint32_t int32_mask_1 = 0x0000FF00;
    const uint32_t int32_mask_2 = 0x00FF0000;
    const uint32_t int32_mask_3 = 0xFF000000;

    const uint64_t int64_mask_0 = 0x00000000000000FF;
    const uint64_t int64_mask_1 = 0x000000000000FF00;
    const uint64_t int64_mask_2 = 0x0000000000FF0000;
    const uint64_t int64_mask_3 = 0x00000000FF000000;
    const uint64_t int64_mask_4 = 0x000000FF00000000;
    const uint64_t int64_mask_5 = 0x0000FF0000000000;
    const uint64_t int64_mask_6 = 0x00FF000000000000;
    const uint64_t int64_mask_7 = 0xFF00000000000000;

    uint16_t BinaryStream::SwapByteOrder(uint16_t v){
        return ((v & int16_mask_0) << 8) | ((v & int16_mask_1) >> 8);
    }
    uint32_t BinaryStream::SwapByteOrder(uint32_t v){
        return ((v & int32_mask_0) << 24) | ((v & int32_mask_1) << 8) | ((v & int32_mask_2) >> 8) | ((v & int32_mask_3) >> 24);
    }
    uint64_t BinaryStream::SwapByteOrder(uint64_t v){
        return ((v & int64_mask_0) << 56) | ((v & int64_mask_1) << 40) | ((v & int64_mask_2) << 24) | ((v & int64_mask_3) << 8)
        | ((v & int64_mask_4) >> 8) | ((v & int64_mask_5) >> 24) | ((v & int64_mask_6) >> 40) | ((v & int64_mask_7) >> 56);
    }
    int16_t BinaryStream::AdjustByteOrder(int16_t v){
        return (int16_t)isSwapByteOrder?SwapByteOrder((uint16_t)v):v;
    }
    int32_t BinaryStream::AdjustByteOrder(int32_t v){
        return (int32_t)isSwapByteOrder?SwapByteOrder((uint32_t)v):v;
    }
    int64_t BinaryStream::AdjustByteOrder(int64_t v){
        return (int64_t)isSwapByteOrder?SwapByteOrder((uint64_t)v):v;
    }
    BinaryStream::BinaryStream(bool bigEndian){
        stringstream* stringStream = new stringstream();
        init(stringStream, bigEndian);
    }
    BinaryStream::BinaryStream(string file, bool bigEndian): BinaryStream(file, ios::in|ios::out|ios::binary, bigEndian){
    }
    BinaryStream::BinaryStream(string file, ios::openmode mode, bool bigEndian){
        fileStream = new fstream();
        fileStream->open(file.c_str(), mode);
        init(fileStream, bigEndian);
    }
    BinaryStream::BinaryStream(char* data, int len, bool bigEndian): BinaryStream(bigEndian){
        stream->write(data, len);
    }
    void BinaryStream::init(iostream* stream, bool bigEndian){
        this->stream = stream;
        this->stream->exceptions(ios::badbit);
        setEndian(bigEndian);
    }

    void BinaryStream::setEndian(bool bigEndian){
        ByteOrderTest.value = 0x2233;
        isSwapByteOrder = false;
        if((bigEndian && ByteOrderTest.bytes[0] == 0x33) || (!bigEndian && ByteOrderTest.bytes[0] == 0x22)){
            //文件字节序和主机字节序不一致，需要交换。
            isSwapByteOrder = true;
        }
    }
    string BinaryStream::ReadStringToNull(){
        string result;
        for(int i=0; i<1024; ++i){
            char c = this->stream->get();
            if(c == '\0'){
                break;
            }
            result += c;
        }
        return result;
    }
    int16_t BinaryStream::ReadInt16(){
        int16_t result;
        this->stream->read((char *)&result, sizeof(result));
        return AdjustByteOrder(result);
    }
    int32_t BinaryStream::ReadInt32(){
        int32_t result;
        this->stream->read((char *)&result, sizeof(result));
        return AdjustByteOrder(result);
    }
    int64_t BinaryStream::ReadInt64(){
        int64_t result;
        this->stream->read((char *)&result, sizeof(result));
        return AdjustByteOrder(result);
    }
    bool BinaryStream::ReadBoolean(){
        bool result;
        this->stream->read((char *)&result, sizeof(result));
        return result;
    }
    char BinaryStream::ReadByte(){
        return this->stream->get();
    }
    void BinaryStream::ReadData(char* buffer, int len){
        this->stream->read(buffer, len);
    }
    void BinaryStream::AlignStream(int alignment){
        int64_t pos = position();
        int64_t mod = pos % alignment;
        if(mod != 0){
            position(alignment - mod, ios::cur);
        }
    }
    void BinaryStream::AlignStreamOutput(int alignment){
        int64_t pos = size();
        int64_t mod = pos % alignment;
        if(mod != 0){
            WriteConst(0, alignment - mod);
        }
    }
    int64_t BinaryStream::position(){
        return this->stream->tellg();
    }
    int64_t BinaryStream::positionOutput(){
        return this->stream->tellp();
    }
    void BinaryStream::position(int64_t pos, ios_base::seekdir way){
        this->stream->seekg(pos, way);
    }
    void BinaryStream::positionOutput(int64_t pos, ios_base::seekdir way){
        this->stream->seekp(pos, way);
    }
    int64_t BinaryStream::size(){
        int64_t pos = position();
        position(0, ios_base::end);
        int64_t size = position();
        position(pos, ios_base::beg);
        return size;
    }
    int64_t BinaryStream::count(){
        return stream->gcount();
    }
    bool BinaryStream::eof(){
        return stream->eof();
    }
    void BinaryStream::WriteInt16(int16_t v){
        v = AdjustByteOrder(v);
        stream->write((char*)&v, sizeof(v));
    }
    void BinaryStream::WriteInt32(int32_t v){
        v = AdjustByteOrder(v);
        stream->write((char*)&v, sizeof(v));
    }
    void BinaryStream::WriteInt64(int64_t v){
        v = AdjustByteOrder(v);
        stream->write((char*)&v, sizeof(v));
    }
    void BinaryStream::WriteStringToNull(string s){
        stream->write(s.data(),s.size());
        stream->put('\0');
    }
    void BinaryStream::WriteConst(char c, int len){
        while(len--){
            stream->put(c);
        }
    }
    void BinaryStream::WriteData(char* buffer, int len){
        this->stream->write(buffer, len);
    }
    void BinaryStream::WriteTo(BinaryStream &stream){
        char buffer[10240];
        int len = 0;
        do{
            ReadData(buffer, 10240);
            len = count();
            stream.WriteData(buffer, len);
        }while(len > 0);
    }
    BinaryStream::~BinaryStream(){
        delete this->stream;
        delete this->dataBuffer;
    };
}