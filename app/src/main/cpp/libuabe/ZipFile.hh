#ifndef XAUSKY_ZIP_FILE_HH
#define XAUSKY_ZIP_FILE_HH
#include <string>
using namespace std;
#define DATA_BUFFER_SIZE (1024*1024*300)
#define PATH_BUFFER_SIZE (1024)
#define RESULT_STATE_OK (0)
#define RESULT_STATE_INTERNAL_ERROR (-1)
namespace xausky {
    class ZipFile {
        public:
        static int Patch(string zipPath, string modsPath, string outputPath);
        static int GenerateMapFile(string inputPath, string outputPath);
    };
}
#endif