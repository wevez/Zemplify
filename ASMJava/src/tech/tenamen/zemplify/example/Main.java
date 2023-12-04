package tech.tenamen.zemplify.example;

import tech.tenamen.asm.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        // 実際のファイルパスに変更してください
        String filePath = "C:\\Users\\ryo\\Documents\\Github\\Java\\Zemplify\\ASMJava\\out\\artifacts\\ASMJava_jar\\ASMJava.jar";

        try {
            // ファイルのパスを指定してバイトコードを取得
            byte[] bytecode = readClassFile(filePath);

            // FileWriterクラスのオブジェクトを生成する
            FileWriter file = new FileWriter("C:\\Users\\ryo\\Documents\\Github\\Java\\Zemplify\\Dll1\\Dll1\\jarBytes.h");
            // PrintWriterクラスのオブジェクトを生成する
            PrintWriter pw = new PrintWriter(new BufferedWriter(file));

            //ファイルに書き込む
            pw.println("#pragma once");
            pw.print("const unsigned char data[] = { ");
            for (byte b : bytecode) {
                pw.printf("0x%02X, ", b);
            }
            pw.print((" };"));
            //ファイルを閉じる
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] readClassFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return Files.readAllBytes(path);
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