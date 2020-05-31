package com.example.reforceapk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.zip.Adler32;

public class main {
    public static void main(String[] args){
        try {
            Scanner sc = new Scanner(System.in);

            System.out.println("input path of source apk:");
            String sourcePath = sc.nextLine();
            if(sourcePath.isEmpty() )
            {
                sourcePath = "force/MyEmpty.apk";
            }
            File payloadSrcFile = new File(sourcePath);
            System.out.println("apk size:" + payloadSrcFile.length());

            System.out.println("input path of unShellDex:");
            String unShellDexPath = sc.nextLine();
            if(unShellDexPath.isEmpty() )
            {
                unShellDexPath= "force/MyEmpty.dex";
            }
            File unShellDexFile = new File(unShellDexPath);

            byte[] payloadArray = encrpt(readFileBytes(payloadSrcFile));
            byte[] unShellDexArray = readFileBytes(unShellDexFile);

            int payloadLen = payloadArray.length;
            int unShellDexLen = unShellDexArray.length;
            int totalLen = payloadLen + unShellDexLen + 4;
            byte[] newDex = new byte[totalLen];

            System.arraycopy(unShellDexArray,0,newDex,0,unShellDexLen);
            System.arraycopy(payloadArray,0,newDex,unShellDexLen,payloadLen);
            System.arraycopy(intToByte(payloadLen),0,newDex,totalLen - 4,4);

            fixFileSizeHeader(newDex);
            fixSHA1Header(newDex);
            fixCheckSumHeader(newDex);

            String str = "force/classes.dex";
            File file = new File(str);
            if(!file.exists()){
                file.createNewFile();
            }

            FileOutputStream localFileOutputStream  = new FileOutputStream(str);
            localFileOutputStream.write(newDex);
            localFileOutputStream.flush();
            localFileOutputStream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void fixCheckSumHeader(byte[] dexBytes) {
        Adler32 adler = new Adler32();
        adler.update(dexBytes,12,dexBytes.length -12);
        long value = adler.getValue();
        int va = (int)value;
        byte[] newcs = intToByte(va);
        byte[] recs = new byte[4];
        for (int i = 0; i < 4; i++) {
            recs[i] = newcs[newcs.length -i -1];
        }
        System.arraycopy(recs,0,dexBytes,8,4);
        System.out.println(Long.toHexString(value));
    }

    private static void fixSHA1Header(byte[] dexBytes)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(dexBytes,32,dexBytes.length -32);
        byte[] newdt = md.digest();
        System.arraycopy(newdt,0,dexBytes,12,20);
        String hexstr = "";
        for (int i = 0; i < newdt.length; i++) {
            hexstr += Integer.toString((newdt[i] & 0xff) + 0x100,16).substring(1);
        }
        System.out.println(hexstr);
    }

    private static void fixFileSizeHeader(byte[] dexBytes) {
        byte[] newfs = new byte[dexBytes.length];
        byte[] refs = new byte[4];
        for (int i = 0; i < 4; i++) {
            refs[i] = newfs[newfs.length -1 -i];
        }
        System.arraycopy(refs,0,dexBytes,32,4);
    }

    private static byte[] intToByte(int a) {
        return new byte[] {
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }

    private static byte[] encrpt(byte[] srcData) {
        for (int i = 0; i < srcData.length; i++) {
            srcData[i] = (byte)(srcData[i] ^ 0xFF);
        }
        return srcData;
    }

    private static byte[] readFileBytes(File file) throws IOException {
        byte[] arrayOfByte = new byte[1024];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileInputStream fis = new FileInputStream(file);
        while(true){
            int i = fis.read(arrayOfByte);
            if(i!=-1){
                baos.write(arrayOfByte,0,i);
            }
            else{
                baos.toByteArray();
            }
        }
    }
}
