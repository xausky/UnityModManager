#ifndef XAUSKY_ZIP_FILE_HH
#define XAUSKY_ZIP_FILE_HH
#include <string>
using namespace std;
#define DATA_BUFFER_SIZE (1024*1024*100)
#define PATH_BUFFER_SIZE (1024)
#define RESULT_STATE_OK (0)
#define RESULT_STATE_INTERNAL_ERROR (-1)
#define RESULT_STATE_PASSWORD_ERROR (-2)
#define RESULT_STATE_FILE_CONFLICT  (-3)
namespace xausky {
    class ZipFile {
        public:
        static int patch(string zipPath, string modsPath, string outputPath);
        static int GenerateMapFile(string inputPath, string outputPath);
    };
}
#endif