// dllmain.cpp : DLL アプリケーションのエントリ ポイントを定義します。
#include "pch.h"

//#define MINIZ_HEADER_FILE_ONLY
#include "miniz/miniz.c"
#include <filesystem>
#include <fstream>
#include <Windows.h>
#include <Commdlg.h>
#include <fstream>
#include <vector>

#define DEBUG_CONSOLE true // デバッグ用コンソールを表示する場合はtrueにします
#define ASM_MAIN_CLASS "tech.tenamen.zemplify.example.Main" // ASMのメインクラスの場所を定義します
#define ASM_MAIN_METHOD "main" // Javaのエントリーポイント関数の名前を定義します
#define ASM_DEFINE_CLASSES_METHOD "getDefineClasses" // JavaのgetDefineClasses関数の名前を定義します

JavaVM* javaVM;
JNIEnv* jniEnv;
jvmtiEnv* jvmtiEnvironment;

std::map<std::string, jclass> cachedKlass = std::map<std::string, jclass>();
std::vector<std::string> shouldDefineClasses = std::vector<std::string>();

HWND currentWindowHandle;
char title[128];

/*
* jclassのクラス名を取得します。
*/
const char* GetName(jclass clz)
{
    const auto cls = jniEnv->FindClass("java/lang/Class");
    const auto mid_getName = jniEnv->GetMethodID(cls, "getName", "()Ljava/lang/String;");
    const auto jname = (jstring)jniEnv->CallObjectMethod(clz, mid_getName);
    const char* name = jniEnv->GetStringUTFChars(jname, 0);
    jniEnv->ReleaseStringUTFChars(jname, name);
    return name;
}

// ファイル選択ダイアログを開く関数
std::wstring openFileDialog() {
    OPENFILENAME ofn;
    wchar_t fileName[MAX_PATH] = L"";

    ZeroMemory(&ofn, sizeof(ofn));
    ofn.lStructSize = sizeof(ofn);
    ofn.hwndOwner = NULL;
    ofn.lpstrFile = fileName;
    ofn.nMaxFile = MAX_PATH;
    ofn.lpstrFilter = L"All Files\0*.*\0";
    ofn.nFilterIndex = 1;
    ofn.Flags = OFN_PATHMUSTEXIST | OFN_FILEMUSTEXIST;

    if (GetOpenFileName(&ofn) == TRUE) {
        return fileName;
    }
    else {
        return L"";
    }
}

// ファイルをconst unsigned char[]型に読み込む関数
std::vector<unsigned char> readFileToUnsignedCharArray(const std::wstring& filePath) {
    std::ifstream file(filePath, std::ios::binary);
    if (!file.is_open()) {
        printf("Error file opening\n");
        return {};
    }

    // ファイルサイズを取得
    file.seekg(0, std::ios::end);
    std::streampos fileSize = file.tellg();
    file.seekg(0, std::ios::beg);

    // バッファを確保してファイルを読み込む
    std::vector<unsigned char> buffer(fileSize);
    file.read(reinterpret_cast<char*>(buffer.data()), fileSize);
    file.close();

    return buffer;
}


void loadJar(jobject classLoader, const unsigned char* jarBytes, size_t size)
{
    mz_zip_archive archive{};
    if (!mz_zip_reader_init_mem(&archive, jarBytes, size, 0))
    {
        printf("!mz_zip_reader_init_mem(&archive, jarBytes, size, 0)\n");
        return;
    }
    mz_uint file_number = mz_zip_reader_get_num_files(&archive);
    printf("zip file number: %d\n", file_number);
    for (mz_uint i = 0; i < file_number; i++)
    {

        if (!mz_zip_reader_is_file_supported(&archive, i) || mz_zip_reader_is_file_a_directory(&archive, i))
            continue;

        char str[256] = { 0 };
        mz_zip_reader_get_filename(&archive, i, str, 256);
        std::string filename(str);
        if (filename.substr(filename.size() - 6) != ".class")
            continue;

        printf("Loading %s\n", filename.c_str());
        size_t classBytes_size = 0;
        unsigned char* classBytes = (unsigned char*)mz_zip_reader_extract_to_heap(&archive, i, &classBytes_size, 0);
        if (!classBytes)
        {
            mz_zip_reader_end(&archive);
            return;
        }

        jclass jaclass = jniEnv->DefineClass(nullptr, classLoader, (const jbyte*)classBytes, classBytes_size);
        if (jaclass) jniEnv->DeleteLocalRef(jaclass);
        mz_free(classBytes);
    }
    mz_zip_reader_end(&archive);
    return;
}


void gc()
{
    jclass System_class = jniEnv->FindClass("java/lang/System");
    jmethodID gcID = jniEnv->GetStaticMethodID(System_class, "gc", "()V");
    jniEnv->CallStaticVoidMethod(System_class, gcID);
    jniEnv->DeleteLocalRef(System_class);
}

std::vector<std::string> getShouldDefineClasses()
{
    if (!cachedKlass.contains(ASM_MAIN_CLASS)) {
        printf("ASM_MAIN_CLASS not found\n");
        return {};
    }
    jclass ClassPatcherClass = cachedKlass.at(ASM_MAIN_CLASS);
    jmethodID getDefineClassesID = jniEnv->GetStaticMethodID(ClassPatcherClass, ASM_DEFINE_CLASSES_METHOD, "(Ljava/lang/String;)[Ljava/lang/String;");
    if (!getDefineClassesID) {
        printf("getDefineClassesID not found\n");
        return {};
    }
    jstring methodToPatchStr = jniEnv->NewStringUTF(title);
    jobjectArray defineClassRetValue = (jobjectArray)jniEnv->CallStaticObjectMethod(ClassPatcherClass, getDefineClassesID, title);
    if (!defineClassRetValue) {
        printf("getDefineClassRetValue might be empty\n");
        return {};
    }
    std::vector<std::string> retVal = std::vector<std::string>();
    for (int i = 0, l = jniEnv->GetArrayLength(defineClassRetValue); i < l; i++) {
        jstring currentItem = (jstring)jniEnv->GetObjectArrayElement(defineClassRetValue, i);
        if (!currentItem) {
            printf("%d is null\n", i);
            continue;
        }
        const char* c = jniEnv->GetStringUTFChars(currentItem, 0);
        retVal.push_back(std::string(c));
        //printf("index: %s\n", c);
    }
    return retVal;
}

jobject newClassLoader()
{
    jclass urlClass = jniEnv->FindClass("java/net/URL");
    jmethodID urlContructor = jniEnv->GetMethodID(urlClass, "<init>", "(Ljava/lang/String;)V");
    jstring str = jniEnv->NewStringUTF("uuum.jp");
    jobject url = jniEnv->NewObject(urlClass, urlContructor, str);
    jobjectArray urls = jniEnv->NewObjectArray(1, urlClass, url);
    jclass URLClassLoaderClass = jniEnv->FindClass("java/net/URLClassLoader");
    jmethodID URLClassLoaderContructor = jniEnv->GetMethodID(URLClassLoaderClass, "<init>", "([Ljava/net/URL;)V");
    jobject URLClassLoader = jniEnv->NewObject(URLClassLoaderClass, URLClassLoaderContructor, urls);

    jniEnv->DeleteLocalRef(urlClass);
    jniEnv->DeleteLocalRef(url);
    jniEnv->DeleteLocalRef(str);
    jniEnv->DeleteLocalRef(urls);
    jniEnv->DeleteLocalRef(URLClassLoaderClass);

    return URLClassLoader;
}

void retransformClasses()
{
    for (const auto m : cachedKlass) {
        bool shouldRetransform = false;
        for (std::string c : shouldDefineClasses) {
            if (m.first._Equal(c.c_str())) {
                shouldRetransform = true;
                break;
            }
        }
        if (shouldRetransform) {
            printf("Retransforming class: %s\n", m.first.c_str());
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
    if (!cachedKlass.contains(ASM_MAIN_CLASS)) {
        printf("ASM_MAIN_CLASS not found\n");
        return;
    }
    jclass ClassPatcherClass = cachedKlass.at(ASM_MAIN_CLASS);
    jbyteArray original_class_bytes = jni_env->NewByteArray(class_data_len);
    jni_env->SetByteArrayRegion(original_class_bytes, 0, class_data_len, (const jbyte*)class_data);

    jmethodID patchMethodID = jni_env->GetStaticMethodID(ClassPatcherClass, ASM_MAIN_METHOD, "([BLjava/lang/String;Ljava/lang/String;)[B");
    if (!patchMethodID) {
        printf("patchMethodID not found\n");
        return;
    }
    jstring methodToPatchStr = jni_env->NewStringUTF(title);
    jstring ThreadContextClassName = jni_env->NewStringUTF(name);

    jbyteArray new_class_bytes = (jbyteArray)jni_env->CallStaticObjectMethod(
        ClassPatcherClass,
        patchMethodID,
        original_class_bytes,
        methodToPatchStr,
        ThreadContextClassName
    );

    jni_env->DeleteLocalRef(original_class_bytes);
    *new_class_data_len = jni_env->GetArrayLength(new_class_bytes);
    jvmti_env->Allocate(*new_class_data_len, new_class_data);
    jni_env->GetByteArrayRegion(new_class_bytes, 0, *new_class_data_len, (jbyte*)*new_class_data);
    jni_env->DeleteLocalRef(new_class_bytes);
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
    if (JNI_GetCreatedJavaVMs(&javaVM, 1, &vmCount) != JNI_OK || vmCount == 0) {
        printf("JNI_GetCreatedJavaVMs(&m_Jvm, 1, &vmCount) != JNI_OK || vmCount == 0\n");
        return 0;
    }
    // JNIEnvを取得します。
    jint res = javaVM->GetEnv(reinterpret_cast<void**>(&jniEnv), JNI_VERSION_1_6);
    if (res == JNI_EDETACHED) {
        if (javaVM->AttachCurrentThread(reinterpret_cast<void**>(&jniEnv), nullptr) != JNI_OK) {
            printf("m_Jvm->AttachCurrentThread(reinterpret_cast<void**>(&m_Env), nullptr) != JNI_OK\n");
            return 0;
        }
    }
    // jvmtiEnvを取得します。
    if (javaVM->GetEnv((void**)&jvmtiEnvironment, JVMTI_VERSION_1_1) != JNI_OK) {
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

    std::wstring selectedFilePath = openFileDialog();

    if (!selectedFilePath.empty()) {
        // ファイルをconst unsigned char[]型に読み込む
        std::vector<unsigned char> fileData = readFileToUnsignedCharArray(selectedFilePath);
        const unsigned char* dataArray = fileData.data();
        loadJar(classLoader, dataArray, fileData.size());
    }
    
    // GetLoadedClassesでクラスを取得し、キャッシュしておきます。
    // env->FindClassは正常に動作しない場合があるので、GetLoadedClassesを使用します。
    jclass* classes;
    jint classCount;
    jvmtiEnvironment->GetLoadedClasses(&classCount, &classes);
    for (int i = 0; i < classCount; i++)
    {
        auto klass = classes[i];
        cachedKlass.emplace(std::make_pair(GetName(klass), klass));
    }
    shouldDefineClasses = getShouldDefineClasses();
    retransformClasses();
    jvmtiEnvironment->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL);

    jniEnv->DeleteLocalRef(classLoader);
    gc();

    Sleep(1000);
    // JVMTIを終了させます。
    javaVM->DetachCurrentThread();
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

