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
                "net.minecraft.client.gui.GuiScreen",
                "net.minecraft.client.renderer.entity.RendererEntityLiving",
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
            // invokestatic net/minecraft/client/Minecraft.getMinecraft()Lnet/minecraft/client/Minecraft;
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraft/client/Minecraft", "getMinecraft", "()Lnet/minecraft/client/Minecraft;", false);

            // getfield net/minecraft/client/Minecraft.fontRendererObj:net.minecraft.client.gui.FontRenderer;
            mv.visitFieldInsn(Opcodes.GETFIELD, "net/minecraft/client/Minecraft", "fontRendererObj", "Lnet/minecraft/client/gui/FontRenderer;");

            // ldc "ABCDEFG" (java.lang.String)
            mv.visitLdcInsn("ABCDEFG");

            // iconst_0
            mv.visitInsn(Opcodes.ICONST_0);

            // iconst_0
            mv.visitInsn(Opcodes.ICONST_0);

            // iconst_m1
            mv.visitInsn(Opcodes.ICONST_M1);

            // invokevirtual net/minecraft/client/gui/FontRenderer.drawString(Ljava/lang/String;III)I
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/client/gui/FontRenderer", "drawString", "(Ljava/lang/String;III)I", false);

            // pop
            mv.visitInsn(Opcodes.POP);
            super.visitCode();
        }
    }
}