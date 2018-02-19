NDK_BUILD = $(shell cat ../local.properties | grep ndk.dir | cut -d = -f 2)/ndk-build
LOCAL_PATH = $(PWD)

copy: libzip ZipPatch
	cp -r $(LOCAL_PATH)/ZipPatch/libs/* $(LOCAL_PATH)/../app/src/main/assets
libzip:
	NDK_PROJECT_PATH=$(LOCAL_PATH)/libzip $(NDK_BUILD)
ZipPatch:
	NDK_PROJECT_PATH=$(LOCAL_PATH)/ZipPatch $(NDK_BUILD)
clean:
	-rm -rf $(LOCAL_PATH)/libzip/obj
	-rm -rf $(LOCAL_PATH)/libzip/libs
	-rm -rf $(LOCAL_PATH)/ZipPatch/obj
	-rm -rf $(LOCAL_PATH)/ZipPatch/libs
	-rm -rf $(LOCAL_PATH)/../app/src/main/assets/*

.PHONY: libzip ZipPatch copy clean