#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include "org_esa_s2tbx_jni_EnvironmentVariablesNative.h"

JNIEXPORT jint JNICALL Java_org_esa_s2tbx_jni_EnvironmentVariablesNative_chdir(JNIEnv *env, jclass thisObj, jstring inDir) {
   	const char *dir = (*env)->GetStringUTFChars(env, inDir, NULL);
   	if (!dir) {
    	return -1;
	}

    int res = chdir(dir);

   	(*env)->ReleaseStringUTFChars(env, inDir, dir);

    return res;
}

JNIEXPORT jstring JNICALL Java_org_esa_s2tbx_jni_EnvironmentVariablesNative_getcwd(JNIEnv *env, jclass thisObj) {
	char *currentDirectory;
    currentDirectory = getcwd(NULL, 0);

	// convert the C-string (char*) into JNI String (jstring) and return
   	return (*env)->NewStringUTF(env, currentDirectory);
}

JNIEXPORT jstring JNICALL Java_org_esa_s2tbx_jni_EnvironmentVariablesNative_getenv(JNIEnv *env, jclass thisObj, jstring inJNIKey) {
	// convert the JNI String (jstring) into C-String (char*)
   	const char *inCKey = (*env)->GetStringUTFChars(env, inJNIKey, NULL);
   	if (NULL == inCKey) {
    	return NULL;
	}

	char *existingValue;
    existingValue = getenv(inCKey);

   	// release resources
   	(*env)->ReleaseStringUTFChars(env, inJNIKey, inCKey);

	// convert the C-string (char*) into JNI String (jstring) and return
   	return (*env)->NewStringUTF(env, existingValue);
}

JNIEXPORT jint JNICALL Java_org_esa_s2tbx_jni_EnvironmentVariablesNative_putenv(JNIEnv * env, jclass thisObj, jstring inJNIKeyEqualValue) {
	// convert the JNI String (jstring) into C-String (char*)
   	const char *inCKeyEqualValue = (*env)->GetStringUTFChars(env, inJNIKeyEqualValue, NULL);
   	if (NULL == inCKeyEqualValue) {
    	return -1;
	}

	jint result = 0;

	// putenv() takes ownership of the string
    if (putenv(strdup(inCKeyEqualValue)) != 0) {
		// failed to set the environment variable
		result = -2;
	}

   	// release resources
   	(*env)->ReleaseStringUTFChars(env, inJNIKeyEqualValue, inCKeyEqualValue);

   	return result;
}
