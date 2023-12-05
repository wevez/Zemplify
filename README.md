# Zemplify
External bytecode transformer using JNI and ASM.  
You can modify any jvm based program through OW2 ASM with Zemplify.
## How to use
### Java(ASM) side
- Make a function which returns a list of classes to modify.  
In the following code, I'm going to modify the class which name is net.minecraft.client.gui.GuiScreen.
```Java
public static String[] getDefineClasses(String windowName) {
    return new String[] {
        "net.minecraft.client.gui.GuiScreen"
    };
}
```
- Make a function which modify bytecodes.  
Is the following code, I'm going to modify to draw a random string in GuiScreen.
```Java
public static byte[] main(byte[] classBytes, String windowName, String className) {
    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM5, classWriter) {
        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            final MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (name.equals("drawScreen")) {
                return new MyMethodVisitor(mv);
            }
            return mv;
        }
    };
    ClassReader classReader = new ClassReader(classBytes);
    classReader.accept(classVisitor, 0);
    return classWriter.toByteArray();
}

public static class MyMethodVisitor extends MethodVisitor {

    public MyMethodVisitor(MethodVisitor mv) {
        super(Opcodes.ASM7, mv);
    }

    @Override
    public void visitCode() {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraft/client/Minecraft", "getMinecraft", "()Lnet/minecraft/client/Minecraft;", false);
        mv.visitFieldInsn(Opcodes.GETFIELD, "net/minecraft/client/Minecraft", "fontRendererObj", "Lnet/minecraft/client/gui/FontRenderer;");
        mv.visitLdcInsn("ABCDEFG");
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitInsn(Opcodes.ICONST_M1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/client/gui/FontRenderer", "drawString", "(Ljava/lang/String;III)I", false);
        mv.visitInsn(Opcodes.POP);
        super.visitCode();
    }
}
```
- Build a jar file.  
With InteliJ IDEA, you can build a jar file with Build > Build Artifacts... > ASMJava:jar > Build.
### C++(JVMTI) side
- Define some signatures.
```C++
#define DEBUG_CONSOLE true // デバッグ用コンソールを表示する場合はtrueにします
#define ASM_MAIN_CLASS "tech.tenamen.zemplify.example.Main" // ASMのメインクラスの場所を定義します
#define ASM_MAIN_METHOD "main" // Javaのエントリーポイント関数の名前を定義します
#define ASM_DEFINE_CLASSES_METHOD "getDefineClasses" // JavaのgetDefineClasses関数の名前を定義します
#define ASM_RETRANSFORM_CLASSES { "net.minecraft.client.gui.GuiScreen" } // Retransformするべきクラスを定義します
```
## Result
![](image.png)