package com.example.pitchshifting.model;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class FileSize {
    final static int BYTES = 1;
    final static int KILOBYTES = 2;
    final static int MEGABYTES = 3;
    final static int GIGABYTES = 4;

    final int code;
    final float size;

    public FileSize(int code, float size) {
        this.code = code;
        this.size = size;
    }

    public FileSize(File file) throws FileNotFoundException {
        if (!file.exists()) {
            throw new FileNotFoundException("File not found");
        }

        int code = 1;
        float size = Float.valueOf(String.valueOf(file.length()));

        while (size >= 1024) {
            Log.i("getFileSize", String.valueOf(size));
            code = code + 1;
            size = size / 1024;
        }

        this.code = code;
        this.size = size;
    }

    public String getString() {
        String text = String.format("%f ", this.size);
        switch (code) {
            case KILOBYTES:
                return text + "KB";
            case MEGABYTES:
                return text + " MB";
            case GIGABYTES:
                return text + " GB";
            default:
                return text + " B";
        }
    }
}
