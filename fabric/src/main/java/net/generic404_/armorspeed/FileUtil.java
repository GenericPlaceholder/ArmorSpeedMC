package net.generic404_.armorspeed;

import java.io.*;
import java.nio.file.Files;

public class FileUtil {
    public static void createFile(File file){
        try {
            if (file.createNewFile()) {
                LogUtil.log("File created: " + file.getName(), LogUtil.LogTypes.INFO);
            } else {
                LogUtil.log("File creation failed. Does it already exist?", LogUtil.LogTypes.ERROR);
            }
            LogUtil.log("File path: " + file.getAbsolutePath(), LogUtil.LogTypes.INFO);
        } catch (Exception ignored) {}
    }

    public static String printFile(File file) throws IOException {
        InputStream input = file.toURI().toURL().openStream();
        byte[] a = new byte[Math.toIntExact(Files.size(file.toPath()))];
        int n = input.read(a);
        input.close();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for(byte b : a){
            if(b!=0x00){
                outputStream.write(b);
            }
        }
        byte[] c = outputStream.toByteArray();
        outputStream.close();
        return new String(c, 0, n);
    }

    public static boolean splitFile(File file, String location, boolean deleteOriginal) throws IOException {
        InputStream inputStream = file.toURI().toURL().openStream();
        byte[] c = new byte[1024];
        int n = inputStream.read(c);
        inputStream.close();
        int middle = (int)(n/2);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for(byte b : c){
            if(b!=0x00){
                outputStream.write(b);
            }
        }
        byte[] a = outputStream.toByteArray();
        outputStream.close();

        byte[] part1 = new byte[middle];
        byte[] part2 = new byte[a.length - middle];

        for (int i = 0; i < middle; i++) {
            part1[i] = a[i];
        }

        for (int i = middle; i < a.length; i++) {
            part2[i - middle] = a[i];
        }

        try {
            File file1 = new File(location, file.getName()+".1.fragment");
            boolean file1created = false;
            if(file1.createNewFile()){
                file1created = true;
            } else {
                if(new File(location, file.getName()+".1.fragment").delete() && file1.createNewFile()) {
                    file1created = true;
                }
            }
            if(file1created){
                try (FileOutputStream fos = new FileOutputStream(file1.getPath())){
                    fos.write(part1);
                }
            } else {
                LogUtil.log("Unable to create fragment 2.", LogUtil.LogTypes.ERROR);
            }

            File file2 = new File(location, file.getName()+".2.fragment");
            boolean file2created = false;
            if(file2.createNewFile()){
                file2created = true;
            } else {
                if(new File(location, file.getName()+".2.fragment").delete() && file2.createNewFile()) {
                    file2created = true;
                }
            }
            if(file2created){
                try (FileOutputStream fos = new FileOutputStream(file2.getPath())){
                    fos.write(part2);
                }
            } else {
                LogUtil.log("Unable to create fragment 2.", LogUtil.LogTypes.ERROR);
            }
        } catch (Exception e) {
            LogUtil.log("Unable to split.", LogUtil.LogTypes.ERROR);
            e.printStackTrace();
            return false;
        }
        file.delete();
        return true;
    }

    public static String removeFileExtension(String name){
        String targetNameTemp = "";
        int counter0 = 0;
        for (String string : name.split("\\.")) {
            counter0++;
            if(name.split("\\.").length>=counter0) {
                targetNameTemp = String.join(targetNameTemp, string);
            }
        }
        return targetNameTemp;
    }
}
