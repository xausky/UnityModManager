# see https://developer.android.com/ndk/guides/android_mk.html


LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := libzip
LOCAL_SRC_FILES := $(LOCAL_PATH)/../../../../native/libzip/obj/local/$(TARGET_ARCH_ABI)/libzip.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := ZipUtils
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../../../native/libzip/jni/lib
LOCAL_SRC_FILES :=  $(wildcard $(LOCAL_PATH)/*.c)
LOCAL_STATIC_LIBRARIES := libzip
LOCAL_LDLIBS := -lz -llog
include $(BUILD_SHARED_LIBRARY)
