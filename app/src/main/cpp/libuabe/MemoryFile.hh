#ifndef XAUSKY_MEMORY_FILE_HH
#define XAUSKY_MEMORY_FILE_HH
#include "BinaryStream.hh"
using namespace std;
namespace xausky {
    class MemoryFile {
        public:
        MemoryFile(string name, BinaryStream* stream);
        ~MemoryFile();
        string name;
        BinaryStream* stream;
        int64_t offset;
        int64_t size;
    };
}
#endif