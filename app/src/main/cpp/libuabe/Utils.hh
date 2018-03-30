#ifndef XAUKSY_UTILS_HH
#define XAUKSY_UTILS_HH
#include <map>
#include <list>
#include <string>
#include "BinaryStream.hh"
using namespace std;
namespace xausky {
    class Utils {
        public:
        static void PrintByte(char* data, int len);
        static void MakeFolder(string path);
        static list<string> ListFolderFiles(string path);
        static map<string, map<int64_t, BinaryStream*>*>* MakeBundlePatch(string path);
        static void FreeBundlePatch(map<string, map<int64_t, BinaryStream*>*>* mods);
    };
}
#endif