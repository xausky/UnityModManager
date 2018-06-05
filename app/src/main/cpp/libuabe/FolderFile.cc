#include "FolderFile.hh"
#include "LogUtils.hh"
#include "Utils.hh"
#include "BundleFile.hh"
#include <sys/stat.h>

namespace xausky {
    char FolderPathBuffer[PATH_BUFFER_SIZE];

    void RestoreTarget(string targetPath, string backupPath){
        __LIBUABE_LOG("RestoreTarget: targetPath=%s,backupPath=%s\n", targetPath.c_str(), backupPath.c_str());
        list<string> files = Utils::ListFolderFiles(backupPath, true);
        for (list<string>::iterator it = files.begin(); it != files.end(); it++){
            Utils::CopyFile(targetPath + '/' + *it, backupPath + '/' + *it);
        }
    }

    void BackupFile(string filename, string targetPath, string backupPath){
        Utils::MakeFileFolder(backupPath + "/" + filename);
        Utils::CopyFile(backupPath + "/" + filename, targetPath + "/" + filename);
    }

    void PatchFile(string filename, string targetPath, string modsPath, string backupPath){
        struct stat modStat;
        string matchName = filename;
        size_t pos = matchName.find_last_of('_');
        if(pos != string::npos){
            matchName = matchName.substr(0, pos);
        }
        sprintf(FolderPathBuffer, "%s/%s", modsPath.c_str(), matchName.c_str());
        if(stat(FolderPathBuffer,&modStat)==0){
            if(S_ISDIR(modStat.st_mode)){
                __LIBUABE_LOG("bundle patch: %s\n", matchName.c_str());
                BackupFile(filename, targetPath, backupPath);
                BinaryStream bundle(targetPath + '/' + filename, true);
                BinaryStream patchedBundle(targetPath + '/' + filename, true);
                BundleFile bundleFile;
                bundleFile.open(bundle);
                map<string, map<int64_t, BinaryStream*>*>* patch = Utils::MakeBundlePatch(FolderPathBuffer);
                bundleFile.patch(*patch);
                bundleFile.save(patchedBundle);
                Utils::FreeBundlePatch(patch);
            }else if(S_ISREG(modStat.st_mode)){
                __LIBUABE_LOG("copy patch: %s\n", filename.c_str());
                BackupFile(filename, targetPath, backupPath);
                Utils::CopyFile(targetPath + '/' + filename, FolderPathBuffer);
                return;
            }
        }
    }

    int FolderFile::Patch(string targetPath, string modsPath, string backupPath){
        RestoreTarget(targetPath, backupPath);
        list<string> files = Utils::ListFolderFiles(targetPath, true);
        for (list<string>::iterator it = files.begin(); it != files.end(); it++){
            PatchFile(*it, targetPath, modsPath, backupPath);
        }
        return RESULT_STATE_OK;
    }

    int FolderFile::GenerateMapFile(string inputPath, string outputPath){
        list<string> files = Utils::ListFolderFiles(inputPath, true);
        FILE* out = fopen(outputPath.c_str(), "w");
        if(out == NULL){
            __LIBUABE_LOG("map file output open failed!\n");
            return RESULT_STATE_INTERNAL_ERROR;
        }
        for (list<string>::iterator it = files.begin(); it != files.end(); it++){
            string filename = *it;
            size_t pos = filename.find_last_of('_');
            if(pos != string::npos){
                filename = filename.substr(0, pos);
            }
            string key = filename;
            pos = filename.find_last_of('/');
            if(pos != string::npos){
                pos = filename.find_last_of('/',pos - 1);
                if(pos != string::npos){
                    key = filename.substr(pos + 1);
                }
            }
            fprintf(out, "%s:%s\n", key.c_str(), filename.c_str());
        }
        fclose(out);
        return RESULT_STATE_OK;
    }
}