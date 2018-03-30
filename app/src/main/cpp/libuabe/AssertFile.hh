#ifndef XAUSKY_ASSERT_FILE_HH
#define XAUSKY_ASSERT_FILE_HH
#include "BinaryStream.hh"
#include <map>
#include <list>
using namespace std;
namespace xausky {
    class AssertFile {
        public:
        static void patch(BinaryStream& input, BinaryStream& output, map<int64_t, BinaryStream*>& mods);
    };
}

#endif