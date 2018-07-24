LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_CPP_FEATURES += exceptions
LOCAL_CPPFLAGS += -std=c++11
LOCAL_MODULE := libNativeUtils
LOCAL_C_INCLUDES += $(LOCAL_PATH)/libuabe
LOCAL_SRC_FILES += $(wildcard $(LOCAL_PATH)/libumm/*.c)
LOCAL_SRC_FILES := $(wildcard $(LOCAL_PATH)/libuabe/*.cc)
LOCAL_SRC_FILES += $(wildcard $(LOCAL_PATH)/libuabe/lz4/*.c)
LOCAL_SRC_FILES += $(wildcard $(LOCAL_PATH)/libuabe/miniz/*.c)
LOCAL_SRC_FILES += $(wildcard $(LOCAL_PATH)/libuabe/libumm/*.c)
LOCAL_SRC_FILES += $(wildcard $(LOCAL_PATH)/*.cc)
LOCAL_LDLIBS := -lz -llog
include $(BUILD_SHARED_LIBRARY)