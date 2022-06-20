package com.sliit.poseapp;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class FileWriter {
    private static final String FILE_NAME = "poses.txt";
    private FileOutputStream fos;
    private FileInputStream fis;

    public FileWriter(Context context, int option) {
        fos = null;
        fis = null;

        if (option == 1) {
            try {
                fos = context.openFileOutput(FILE_NAME, Context.MODE_APPEND);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }else if(option==0){
            try {
                fis=context.openFileInput(FILE_NAME);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }else{
            try {
                fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

   public void writeText(byte[] text){
       try {
           fos.write(text);

       } catch (IOException e) {
           e.printStackTrace();
       }
   }
    public void closeWriter(){
        try {
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void writeText(char c) {
        try {
            fos.write(c);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public String readText() {
        String text = null;
        try {
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();


            while ((text = br.readLine()) != null) {
                sb.append(text).append("\n");
            }
//            Log.d("dd", text.trim());

        }catch (IOException e){
        }
        return text;
    }
}
