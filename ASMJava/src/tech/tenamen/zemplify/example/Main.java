package tech.tenamen.zemplify.example;

import tech.tenamen.asm.*;

public class Main {

    /**
     * bytecodeを受け取り、それをASMを用いて改造し、改造されたbytecodeを返します。
     *
     * @param classBytes bytecode
     * @param windowName window name
     * @param className class name
     * @return 改造されたbytecode
     */
    public static byte[] main(byte[] classBytes, String windowName, String className) {
        if (className.equalsIgnoreCase("net/minecraft/client/renderer/entity/LivingEntityRenderer")) {
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM5, classWriter) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    System.out.println(name);
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }
            };
            ClassReader classReader = new ClassReader(classBytes);
            classReader.accept(classVisitor, 0);
            return classWriter.toByteArray();
        }

        return classBytes;
    }
}