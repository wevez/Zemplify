package tech.tenamen.zemplify.example;

import org.objectweb.asm.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {


    public static void main(String[] args) {
        String filePath = "C:\\Users\\ryo\\Documents\\Github\\Java\\Zemplify\\ASMMain\\out\\artifacts\\asm_jar\\asm.jar"; // 実際のファイルパスに変更してください

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

    public static byte[] main(byte[] classBytes, String windowName, String className) {
        switch (className) {
            case "net/minecraft/client/renderer/entity/player/PlayerRenderer": {
                System.out.println("PlayerRenderer");
                break;
            }
            case "net/minecraft/client/renderer/GameRenderer": {
                System.out.println("GameRenderer");
                break;
            }
            default:
                break;
        }
        return classBytes;
    }
}