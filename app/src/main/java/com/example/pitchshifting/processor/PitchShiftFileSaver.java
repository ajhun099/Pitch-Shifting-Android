package com.example.pitchshifting.processor;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.PitchShifter;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.UniversalAudioInputStream;

public class PitchShiftFileSaver {
    final File file;
    final TarsosDSPAudioFormat format;
    final int sampleRate;
    final int size;
    final int overlap;
    final float factor;

    AudioDispatcher dispatcher;
    OnProcessingFinishedHandler onProcessingFinishedHandler;

    public PitchShiftFileSaver(File file, float factor, TarsosDSPAudioFormat format, int sampleRate, int size, int overlap) {
        this.file = file;
        this.factor = factor;
        this.format = format;
        this.sampleRate = sampleRate;
        this.size = size;
        this.overlap = overlap;
    }

    public void saveFile(File pitchShiftedFile) throws FileNotFoundException {
        FileInputStream fileInputStream = new FileInputStream(file);
        TarsosDSPAudioFormat format = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, false);
        dispatcher = new AudioDispatcher(new UniversalAudioInputStream(fileInputStream, format), size, overlap);
        PitchShifter pitchShifter = new PitchShifter(1.0 / factor, sampleRate, size, overlap);
        dispatcher.addAudioProcessor(pitchShifter);

        RandomAccessFile randomAccessFile = new RandomAccessFile(pitchShiftedFile, "rw");
        AudioProcessor saveProcessor = new CustomWriter(format, randomAccessFile, file.length());
        dispatcher.addAudioProcessor(saveProcessor);

        dispatcher.addAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                Log.i("Buffer Size Process", String.valueOf(audioEvent.getSamplesProcessed()));
                return true;
            }

            @Override
            public void processingFinished() {
                if (onProcessingFinishedHandler != null) {
                    onProcessingFinishedHandler.onProcessingFinished();
                }
            }
        });

        Thread audioThread = new Thread(dispatcher, "Audio Thread");
        audioThread.start();
    }

    public void stop() {
        if (dispatcher != null) {
            if (!dispatcher.isStopped())
                dispatcher.stop();
            dispatcher = null;
        }
    }

    public void setOnProcessingFinishedHandler(OnProcessingFinishedHandler onProcessingFinishedHandler) {
        this.onProcessingFinishedHandler = onProcessingFinishedHandler;
    }
}
