LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := libZipUtils
LOCAL_CFLAGS := -DHAVE_CONFIG_H=1
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../libzip
LOCAL_SRC_FILES :=  $(wildcard $(LOCAL_PATH)/*.c)
LOCAL_STATIC_LIBRARIES := libzip
LOCAL_LDLIBS := -lz -llog
include $(BUILD_SHARED_LIBRARY)

