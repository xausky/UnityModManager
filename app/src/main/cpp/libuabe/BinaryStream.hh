#ifndef XAUSKY_BINARY_STREAM_HH
#define XAUSKY_BINARY_STREAM_HH
#include <string>
#include <fstream>
using namespace std;
namespace xausky {
    class BinaryStream {
        public:
        BinaryStream(bool bigEndian);
        BinaryStream(string file, bool bigEndian);
        BinaryStream(string file, ios::openmode mode, bool bigEndian);
        BinaryStream(char* data, int len, bool bigEndian);
        ~BinaryStream();

        int16_t ReadInt16();
        int32_t ReadInt32();
        int64_t ReadInt64();
        bool ReadBoolean();
        char ReadByte();
        void ReadData(char* buffer, int len);
        string ReadStringToNull();
        void AlignStream(int alignment);

        void WriteInt16(int16_t v);
        void WriteInt32(int32_t v);
        void WriteInt64(int64_t v);
        void WriteData(char* buffer, int len);
        void WriteConst(char c, int len);
        void WriteStringToNull(string s);
        void AlignStreamOutput(int alignment);

        void WriteTo(BinaryStream &stream);

        int64_t position();
        int64_t positionOutput();
        void position(int64_t pos, ios_base::seekdir way);
        void positionOutput(int64_t pos, ios_base::seekdir way);
        int64_t size();
        int64_t count();
        bool eof();
        void setEndian(bool bigEndian);

        static uint16_t SwapByteOrder(uint16_t v);
        static uint32_t SwapByteOrder(uint32_t v);
        static uint64_t SwapByteOrder(uint64_t v);

        int32_t classId;

        private:
        void init(iostream* stream, bool bigEndian);
        int16_t AdjustByteOrder(int16_t v);
        int32_t AdjustByteOrder(int32_t v);
        int64_t AdjustByteOrder(int64_t v);
        iostream* stream = NULL;
        fstream* fileStream = NULL;
        string* dataBuffer = NULL;
        bool isSwapByteOrder;
    };
}
#endif