#include "Utils.hh"
#include <iostream>
#include <dirent.h>
#include <sys/stat.h>
#include <sys/types.h>
namespace xausky {
    void Utils::PrintByte(char* data, int len){
        putchar('[');
        for(int i=0; i<len; ++i){
            printf("%02X",(unsigned char)data[i]);
            if(len != i-1){
                putchar(',');
            }
        }
        puts("]\n");
    }
    void Utils::MakeFolder(string path){
        mkdir(path.c_str(), 0777);
    }
    list<string> Utils::ListFolderFiles(string path){
        list<string> result;
        DIR *folder = opendir(path.c_str());
        struct dirent *rent = NULL;
        while((rent = readdir(folder)) != NULL){
            if(rent->d_type == DT_REG){
                result.push_back(string(rent->d_name));
            }
        }
        closedir(folder);
        return result;
    }
    map<string, map<int64_t, BinaryStream*>*>* Utils::MakeBundlePatch(string path){
        map<string, map<int64_t, BinaryStream*>*>* patch = new map<string, map<int64_t, BinaryStream*>*>();
        list<string> files = ListFolderFiles(path);
        for (list<string>::iterator it = files.begin(); it != files.end(); it++){
            string file = (*it);
            size_t first = file.find_first_of('-');
            size_t last = file.find_last_of('-');
            string name = file.substr(first + 1, last - first -1);
            string postfix = file.substr(last + 1, file.size() - last);
            int64_t pathId;
            sscanf(postfix.c_str(), "%llu.dat", &pathId);
            map<string, map<int64_t, BinaryStream*>*>::iterator iterator = patch->find(name);
            map<int64_t, BinaryStream*>* mod;
            if(iterator == patch->end()){
                mod = new map<int64_t, BinaryStream*>();
                patch->insert(pair<string, map<int64_t, BinaryStream*>*>(string(name), mod));
            } else {
                mod = iterator->second;
            }
            (*mod)[pathId] = new BinaryStream(path + "/" + file, true);
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
                printf("delete patchiterator second done.\n");
                ++patchiterator;
            }
            }
            delete patch;
            printf("delete patch done.\n");
            ++it;
        }
        delete mods;
        printf("delete mods done.\n");
    }
}