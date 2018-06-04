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
        static void CopyFile(string target, string origin);
        static void MakeFileFolder(string target);
        static list<string> ListFolderFiles(string path, bool recursive);
        static map<string, map<int64_t, BinaryStream*>*>* MakeBundlePatch(string path);
        static void FreeBundlePatch(map<string, map<int64_t, BinaryStream*>*>* mods);
    };
}
#endif