# see https://developer.android.com/ndk/guides/android_mk.html


LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libzip

 
LOCAL_CFLAGS := -DHAVE_CONFIG_H=1
LOCAL_C_INCLUDES += $(LOCAL_PATH)/gladman-fcrypt
 


MY_PATH=$(LOCAL_PATH)
 
# http://falsinsoft.blogspot.de/2014/10/androidmk-include-multiple-source-files.html
LOCAL_SRC_FILES :=  $(wildcard $(MY_PATH)/*.c)
LOCAL_SRC_FILES := $(LOCAL_SRC_FILES:$(MY_PATH)/%=%)
  


# link library libz
LOCAL_LDLIBS := -lz -llog

include $(BUILD_SHARED_LIBRARY)
#include$lude $(BUILD_STATIC_LIBRARY)
