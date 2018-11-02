#ifndef XAUSKY_FOLDER_FILE_HH
#define XAUSKY_FOLDER_FILE_HH
#include <string>
using namespace std;
#define PATH_BUFFER_SIZE (1024)
#define RESULT_STATE_OK (0)
#define RESULT_STATE_INTERNAL_ERROR (-1)
namespace xausky {
    class FolderFile {
    public:
        static int Patch(string targetPath, string modsPath, string backupPath);
        static int GenerateMapFile(string inputPath, string outputPath);
    };
}
#endif
