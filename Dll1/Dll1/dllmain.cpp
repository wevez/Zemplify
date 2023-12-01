// dllmain.cpp : DLL アプリケーションのエントリ ポイントを定義します。
#include "pch.h"

//#define MINIZ_HEADER_FILE_ONLY
#include "miniz/miniz.c"
#include "jarBytes.h"
#include <filesystem>
#include <functional>
#include <fstream>

#define DEBUG_CONSOLE true // デバッグ用コンソールを表示する場合はtrueにします
#define ASM_MAIN_CLASS "tech.tenamen.zemplify.example.Main" // ASMのメインクラスの場所を定義します
#define ASM_MAIN_METHOD "main" // Javaのエントリーポイント関数の名前を定義します

JavaVM* m_Jvm;
JNIEnv* m_Env;
jvmtiEnv* jvmtiEnvironment;

std::map<std::string, jclass> m_CachedKlass = std::map<std::string, jclass>();

HWND currentWindowHandle;
char title[128];

/*
* jclassのクラス名を取得します。
*/
const char* GetName(jclass clz)
{
    const auto cls = m_Env->FindClass("java/lang/Class");
    const auto mid_getName = m_Env->GetMethodID(cls, "getName", "()Ljava/lang/String;");
    const auto jname = (jstring)m_Env->CallObjectMethod(clz, mid_getName);
    const char* name = m_Env->GetStringUTFChars(jname, 0);
    m_Env->ReleaseStringUTFChars(jname, name);
    return name;
}

void loadJar(jobject classLoader, const unsigned char* jarBytes, size_t size)
{
    mz_zip_archive archive{};
    if (!mz_zip_reader_init_mem(&archive, jarBytes, size, 0))
    {
        return;
    }
    mz_uint file_number = mz_zip_reader_get_num_files(&archive);
    for (mz_uint i = 0; i < file_number; i++)
    {

        if (!mz_zip_reader_is_file_supported(&archive, i) || mz_zip_reader_is_file_a_directory(&archive, i))
            continue;

        char str[256] = { 0 };
        mz_zip_reader_get_filename(&archive, i, str, 256);
        std::string filename(str);
        if (filename.substr(filename.size() - 6) != ".class")
            continue;

        //printf("%s\n", filename.c_str());
        size_t classBytes_size = 0;
        unsigned char* classBytes = (unsigned char*)mz_zip_reader_extract_to_heap(&archive, i, &classBytes_size, 0);
        if (!classBytes)
        {
            mz_zip_reader_end(&archive);
            return;
        }

        jclass jaclass = m_Env->DefineClass(nullptr, classLoader, (const jbyte*)classBytes, classBytes_size);
        if (jaclass) m_Env->DeleteLocalRef(jaclass);
        mz_free(classBytes);
    }
    mz_zip_reader_end(&archive);
    return;
}


void gc()
{
    jclass System_class = m_Env->FindClass("java/lang/System");
    jmethodID gcID = m_Env->GetStaticMethodID(System_class, "gc", "()V");
    m_Env->CallStaticVoidMethod(System_class, gcID);
    m_Env->DeleteLocalRef(System_class);
}

jobject newClassLoader()
{
    jclass urlClass = m_Env->FindClass("java/net/URL");
    jmethodID urlContructor = m_Env->GetMethodID(urlClass, "<init>", "(Ljava/lang/String;)V");
    jstring str = m_Env->NewStringUTF("file://ftp.yoyodyne.com/pub/files/foobar.txt");
    jobject url = m_Env->NewObject(urlClass, urlContructor, str);
    jobjectArray urls = m_Env->NewObjectArray(1, urlClass, url);
    jclass URLClassLoaderClass = m_Env->FindClass("java/net/URLClassLoader");
    jmethodID URLClassLoaderContructor = m_Env->GetMethodID(URLClassLoaderClass, "<init>", "([Ljava/net/URL;)V");
    jobject URLClassLoader = m_Env->NewObject(URLClassLoaderClass, URLClassLoaderContructor, urls);

    m_Env->DeleteLocalRef(urlClass);
    m_Env->DeleteLocalRef(url);
    m_Env->DeleteLocalRef(str);
    m_Env->DeleteLocalRef(urls);
    m_Env->DeleteLocalRef(URLClassLoaderClass);

    return URLClassLoader;
}

void retransformClasses()
{
    for (const auto m : m_CachedKlass) {
        if (m.first._Equal("net.minecraft.client.renderer.entity.player.PlayerRenderer") || m.first._Equal("net.minecraft.client.renderer.GameRenderer")) {
            printf("Found\n");
            jvmtiEnvironment->RetransformClasses(1, &m.second);
        }
    }
}

void JNICALL ClassFileLoadHook
(
    jvmtiEnv* jvmti_env,
    JNIEnv* jni_env,
    jclass class_being_redefined,
    jobject loader,
    const char* name,
    jobject protection_domain,
    jint class_data_len,
    const unsigned char* class_data,
    jint* new_class_data_len,
    unsigned char** new_class_data
)
{
    if (!m_CachedKlass.contains(ASM_MAIN_CLASS)) {
        printf("cache not found\n");
        return;
    }
    
    jclass ClassPatcherClass = m_CachedKlass.at(ASM_MAIN_CLASS);
    jbyteArray original_class_bytes = jni_env->NewByteArray(class_data_len);
    jni_env->SetByteArrayRegion(original_class_bytes, 0, class_data_len, (const jbyte*)class_data);

    jmethodID patchMethodID = jni_env->GetStaticMethodID(ClassPatcherClass, ASM_MAIN_METHOD, "([BLjava/lang/String;Ljava/lang/String;)[B");
    if (!patchMethodID) {
        printf("patchMethodID not found\n");
        return;
    }
    jstring titleStr = jni_env->NewStringUTF(title);
    jstring classNameStr = jni_env->NewStringUTF(name);
    jbyteArray new_class_bytes = (jbyteArray)jni_env->CallStaticObjectMethod(
        ClassPatcherClass,
        patchMethodID,
        original_class_bytes,
        titleStr,
        classNameStr
    );


    *new_class_data_len = jni_env->GetArrayLength(new_class_bytes);
    jvmti_env->Allocate(*new_class_data_len, new_class_data);
    jni_env->GetByteArrayRegion(new_class_bytes, 0, *new_class_data_len, (jbyte*)*new_class_data);
}

/*
* DLLのメイン関数です。
*/
DWORD APIENTRY Main(HMODULE hModule)
{
    // コンソールを表示します
    if (DEBUG_CONSOLE) {
        AllocConsole();
        FILE* fStream;
        freopen_s(&fStream, "conin$", "r", stdin);
        freopen_s(&fStream, "conout$", "w", stdout);
        freopen_s(&fStream, "conout$", "w", stderr);
    }
    // DLL呼び出し元のHWNDとウィンドウタイトルを取得します。
    for (currentWindowHandle = GetTopWindow(NULL); currentWindowHandle != NULL; currentWindowHandle = GetNextWindow(currentWindowHandle, GW_HWNDNEXT))
    {
        if (!IsWindowVisible(currentWindowHandle) || GetWindowTextLength(currentWindowHandle) == 0) continue;
        DWORD pid;
        GetWindowThreadProcessId(currentWindowHandle, &pid);
        if (pid != GetCurrentProcessId()) continue;
        GetWindowTextA(currentWindowHandle, title, 128);
        break;
    }
    // JavaVMを取得します。
    jsize vmCount = 0;
    if (JNI_GetCreatedJavaVMs(&m_Jvm, 1, &vmCount) != JNI_OK || vmCount == 0) {
        printf("JNI_GetCreatedJavaVMs(&m_Jvm, 1, &vmCount) != JNI_OK || vmCount == 0\n");
        return 0;
    }
    // JNIEnvを取得します。
    jint res = m_Jvm->GetEnv(reinterpret_cast<void**>(&m_Env), JNI_VERSION_1_6);
    if (res == JNI_EDETACHED) {
        if (m_Jvm->AttachCurrentThread(reinterpret_cast<void**>(&m_Env), nullptr) != JNI_OK) {
            printf("m_Jvm->AttachCurrentThread(reinterpret_cast<void**>(&m_Env), nullptr) != JNI_OK\n");
            return 0;
        }
    }
    // jvmtiEnvを取得します。
    if (m_Jvm->GetEnv((void**)&jvmtiEnvironment, JVMTI_VERSION_1_1) != JNI_OK) {
        printf("m_Jvm->GetEnv((void**)&jvmtiEnvironment, JVMTI_VERSION_1_1) != JNI_OK\n");
        return 0;
    }

    jvmtiCapabilities capabilities{};
    capabilities.can_retransform_classes = JVMTI_ENABLE;
    jvmtiEnvironment->AddCapabilities(&capabilities);
    jvmtiEventCallbacks callbacks{};
    callbacks.ClassFileLoadHook = &ClassFileLoadHook;
    jvmtiEnvironment->SetEventCallbacks(&callbacks, sizeof(jvmtiEventCallbacks));
    jvmtiEnvironment->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL);

    jobject classLoader = newClassLoader();

    loadJar(classLoader, data, sizeof(data));

    // GetLoadedClassesでクラスを取得し、キャッシュしておきます。
    // env->FindClassは正常に動作しない場合があるので、GetLoadedClassesを使用します。
    jclass* classes; jint classCount;
    jvmtiEnvironment->GetLoadedClasses(&classCount, &classes);
    for (int i = 0; i < classCount; i++)
    {
        auto klass = classes[i];
        m_CachedKlass.emplace(std::make_pair(GetName(klass), klass));
    }

    retransformClasses();
    jvmtiEnvironment->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL);

    m_Env->DeleteLocalRef(classLoader);
    gc();

    Sleep(1000);
    // JVMTIを終了させます。
    m_Jvm->DetachCurrentThread();
    // コンソールを非表示にします
    if (DEBUG_CONSOLE) {
        ShowWindow(GetConsoleWindow(), SW_HIDE);
        FreeConsole();
        FreeLibraryAndExitThread(hModule, 0);
    }
    return 0;
}

BOOL APIENTRY DllMain( HMODULE hModule,
                       DWORD  ul_reason_for_call,
                       LPVOID lpReserved
                     )
{
    switch (ul_reason_for_call)
    {
    case DLL_PROCESS_ATTACH: {
        const auto handle = CreateThread(nullptr, 0, (LPTHREAD_START_ROUTINE)Main, hModule, 0, nullptr);
        if (handle != NULL) CloseHandle(handle);
        break;
    }
    case DLL_THREAD_ATTACH:
    case DLL_THREAD_DETACH:
    case DLL_PROCESS_DETACH:
        break;
    }
    return TRUE;
}

