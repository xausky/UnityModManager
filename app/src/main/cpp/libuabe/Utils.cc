#include "Utils.hh"
#include "LogUtils.hh"
#include <iostream>
#include <dirent.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>
#include <libgen.h>

#define PATH_BUFFER_SIZE (1024)

namespace xausky {

    void MakeFolder(const char *target) {
        char buffer[PATH_BUFFER_SIZE];
        if((strcmp(target,".") == 0) || (strcmp(target,"/")==0)) {
            return;
        }
        if(access(target,F_OK) == 0) {
            return;
        } else {
            char * dir = dirname(target);
            strcpy(buffer, dir);
            MakeFolder(buffer);
        }
        if(mkdir(target,0700) == -1){
            return;
        }
    }

    void Utils::MakeFileFolder(string target) {
        char buffer[PATH_BUFFER_SIZE];
        char * dir = dirname(target.c_str());
        strcpy(buffer, dir);
        MakeFolder(buffer);
    }


    void Utils::CopyFile(string target, string origin) {
        ifstream input(origin.c_str(), ios::binary | ios::in ) ;
        ofstream output(target.c_str(), ios::binary | ios::out ) ;
        output<<input.rdbuf();
    }

    list<string> Utils::ListFolderFiles(string path, bool recursive){
        list<string> result;
        list<string> folders;
        folders.push_back("");
        while (!folders.empty()){
            string current = folders.back();folders.pop_back();
            DIR *folder = opendir((path + "/" + current).c_str());
            struct dirent *rent = NULL;
            while((rent = readdir(folder)) != NULL){
                if(strcmp(rent->d_name,".")==0 || strcmp(rent->d_name,"..")==0){
                    continue;
                }
                if(rent->d_type == DT_REG){
                    result.push_back(current + string(rent->d_name));
                } else if(rent->d_type == DT_DIR && recursive){
                    folders.push_back(current + string(rent->d_name) + '/');
                }
            }
            closedir(folder);
        }
        return result;
    }
    map<string, map<int64_t, BinaryStream*>*>* Utils::MakeBundlePatch(string path){
        map<string, map<int64_t, BinaryStream*>*>* patch = new map<string, map<int64_t, BinaryStream*>*>();
        list<string> files = ListFolderFiles(path, false);
        for (list<string>::iterator it = files.begin(); it != files.end(); it++){
            string file = (*it);
            size_t first = file.find_first_of('-');
            size_t last = file.find_last_of('-');
            string name = file.substr(first + 1, last - first -1);
            string prefix = file.substr(0, first);
            string postfix = file.substr(last + 1, file.size() - last);
            int64_t pathId;
            int32_t classId;
            if(sscanf(prefix.c_str(),"+%X", &classId)!=1){
                classId = -1;
            }
            sscanf(postfix.c_str(), "%llu.dat", &pathId);
            map<string, map<int64_t, BinaryStream*>*>::iterator iterator = patch->find(name);
            map<int64_t, BinaryStream*>* mod;
            if(iterator == patch->end()){
                mod = new map<int64_t, BinaryStream*>();
                patch->insert(pair<string, map<int64_t, BinaryStream*>*>(name, mod));
            } else {
                mod = iterator->second;
            }
            BinaryStream* stream = new BinaryStream(path + "/" + file, true);
            stream->classId = classId;
            (*mod)[pathId] = stream;
        }
        return patch;
    }

    void Utils::FreeBundlePatch(map<string, map<int64_t, BinaryStream*>*>* mods){
        map<string, map<int64_t, BinaryStream*>*>::iterator it = mods->begin();
        while(it != mods->end()){
            map<int64_t, BinaryStream*>* patch = it->second;
            {
            map<int64_t, BinaryStream*>::iterator patchiterator = patch->begin();
            while(patchiterator != patch->end()){
                delete patchiterator->second;
                ++patchiterator;
            }
            }
            delete patch;
            ++it;
        }
        delete mods;
    }
}