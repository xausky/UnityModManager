NDK_BUILD = $(shell cat ../local.properties | grep ndk.dir | cut -d = -f 2)/ndk-build
LOCAL_PATH = $(PWD)

all: libzip ZipPatch

libzip:
	NDK_PROJECT_PATH=$(LOCAL_PATH)/libzip $(NDK_BUILD)
ZipPatch:
	NDK_PROJECT_PATH=$(LOCAL_PATH)/ZipPatch $(NDK_BUILD)

.PHONY: libzip ZipPatch all