# see https://developer.android.com/ndk/guides/android_mk.html


LOCAL_PATH := $(call my-dir)/lib
include $(CLEAR_VARS)
LOCAL_MODULE := libzip
LOCAL_CFLAGS := -DHAVE_CONFIG_H=1
LOCAL_C_INCLUDES += $(LOCAL_PATH)/gladman-fcrypt
LOCAL_SRC_FILES :=  $(wildcard $(LOCAL_PATH)/*.c)
include $(BUILD_STATIC_LIBRARY)
