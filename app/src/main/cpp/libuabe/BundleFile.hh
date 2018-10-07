#ifndef XAUSKY_BUNDLE_FILE_HH
#define XAUSKY_BUNDLE_FILE_HH

#include <map>
#include <list>
#include <string>
#include "BinaryStream.hh"
#include "MemoryFile.hh"

using namespace std;
namespace xausky {
    enum CompressMethod {
        NONE = 0, LZ4 = 2, LZ4HC = 3,
    };

    struct UnsupprtBundleFileType : std::exception {
        const char *what() const noexcept { return "Supprt bundle file type only UnityFS"; }
    };

    struct UnsupprtCompressMethod : std::exception {
        const char *what() const noexcept { return "Supprt Compress method only LZ4HC"; }
    };

    struct DecompressDataException : std::exception {
        const char *what() const noexcept { return "decompress data excption"; }
    };

    class BundleFile {
    public:
        ~BundleFile();

        void open(BinaryStream &stream);

        void save(BinaryStream &stream, int32_t maxBlockSize = 131072, int32_t bundleFlag = 67,
                  int16_t blocksFlag = 3, int32_t fileFlag = 4);

        void patch(map<string, map<int64_t, BinaryStream *> *> &mods);

        static CompressMethod getCompressMethod(int32_t flag);

        static bool infoBlocksIsAtOfFileEnd(int32_t flag);

        string fileType = "UnityFS";
        string versionPlayer = "5.x.x";
        string versionEngine = "5.6.4p4";
        int32_t fileVersion = 6;
        list<MemoryFile *> files;
    };
}
#endif