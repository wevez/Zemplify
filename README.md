# Zemplify
External bytecode transformer using JNI and ASM.  
You can modify any jvm based program through OW2 ASM with Zemplify.
## How to use
### Java(ASM) side
- Make a function which returns a list of classes to modify.  
In the following code, I'm going to modify the class which name is net.minecraft.client.Minecraft.
```Java
public static String[] getDefineClasses(String windowName) {
    return new String[] {
        "net.minecraft.client.Minecraft"
    };
}
```
- Make a function which modify bytecodes.  
Is the following code, I'm going to modify to print the class name and methods name.
```Java
public static byte[] main(byte[] classBytes, String windowName, String className) {
    System.out.println("Class name: " + className);
    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM5, classWriter) {
        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            System.out.println("Method name: " + name);
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
    };
    ClassReader classReader = new ClassReader(classBytes);
    classReader.accept(classVisitor, 0);
    return classWriter.toByteArray();
}
```
- Build a jar file.  
With InteliJ IDEA, you can build a jar file with Build > Build Artifacts... > ASMJava:jar > Build.
### C++(JVMTI) side
- Define some signatures.
```C++
#define ASM_MAIN_CLASS "tech.tenamen.zemplify.example.Main" // ASMのメインクラスの場所を定義します
#define ASM_MAIN_METHOD "main" // Javaのエントリーポイント関数の名前を定義します
#define ASM_DEFINE_CLASSES_METHOD "getDefineClasses" // JavaのgetDefineClasses関数の名前を定義します
```