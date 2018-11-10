#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_sensetimeliangweile_basiccamera2_NativeLibraries_stringFromJNI(JNIEnv *env,
                                                                                jobject instance) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}