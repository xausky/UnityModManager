#include "MemoryFile.hh"

namespace xausky {
    MemoryFile::MemoryFile(string name, BinaryStream* stream){
        this->name = name;
        this->stream = stream;
    }
    MemoryFile::~MemoryFile(){
    }
}