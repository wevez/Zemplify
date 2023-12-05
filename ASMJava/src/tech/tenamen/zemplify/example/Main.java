package tech.tenamen.zemplify.example;

import tech.tenamen.asm.*;

public class Main {

    /**
     * 改造するべきクラスの名前を持った配列を返します。
     *
     * @param windowName window name
     * @return クラスの名前の配列
     */
    public static String[] getDefineClasses(String windowName) {
        return new String[] {
          "net.minecraft.client.Minecraft"
        };
    }

    /**
     * bytecodeを受け取り、それをASMを用いて改造し、改造されたbytecodeを返します。
     *
     * @param classBytes bytecode
     * @param windowName window name
     * @param className class name
     * @return 改造されたbytecode
     */
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
}