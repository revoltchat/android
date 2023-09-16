#include <android/log.h>
#include <jni.h>
#include <string.h>

int hardCrashCounter = 0;

extern "C"
JNIEXPORT void JNICALL
Java_chat_revolt_ndk_Pipebomb_incrementHardCrashCounter(JNIEnv *env, jobject thiz) {
    hardCrashCounter++;
}

extern "C"
JNIEXPORT void JNICALL
Java_chat_revolt_ndk_Pipebomb_doHardCrash(JNIEnv *env,
                                          jobject thiz) {
    int *p = nullptr;
    *p = 0;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_chat_revolt_ndk_Pipebomb_checkHardCrash(JNIEnv *env, jobject thiz) {
    return hardCrashCounter > 3;
}