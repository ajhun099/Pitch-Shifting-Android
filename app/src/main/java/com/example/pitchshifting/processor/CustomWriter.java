package com.example.pitchshifting.processor;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.writer.WaveHeader;

public class CustomWriter implements AudioProcessor {
    final RandomAccessFile output;
    final TarsosDSPAudioFormat format;
    final long totalFileSize;

    private int length = 0;
    private static final int HEADER_LENGTH = 44; // Default 44
    private long totalSize = 0;

    public CustomWriter(TarsosDSPAudioFormat format, RandomAccessFile output, long totalFileSize) {
        this.output = output;
        this.format = format;
        this.totalFileSize = totalFileSize;

        try {
            this.output.write(new byte[HEADER_LENGTH]);
        } catch (IOException error) {
            error.printStackTrace();
        }
    }

    @Override
    public boolean process(AudioEvent event) {

        int var2 = event.getOverlap();
        int var3 = event.getBufferSize() - var2;
        byte[] var4 = event.getByteBuffer();

        this.totalSize = this.totalSize + event.getBufferSize();

        try {
            this.length += event.getByteBuffer().length;
            this.output.write(var4, var2 * 2, var3 * 2);
        } catch (IOException error) {
            error.printStackTrace();
        }

        return true;
    }

    @Override
    public void processingFinished() {
        WaveHeader waveHeader = new WaveHeader((short) 1, (short) this.format.getChannels(), (int) this.format.getSampleRate(), (short) 16, this.length);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            waveHeader.write(outputStream);
            this.output.seek(0L);
            this.output.write(outputStream.toByteArray());
            this.output.close();
        } catch (IOException error) {
            error.printStackTrace();
        }
    }
}
