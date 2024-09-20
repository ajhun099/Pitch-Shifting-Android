package com.example.pitchshifting.processor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.writer.WriterProcessor;

public class AudioRecorder {
    final File file;
    final TarsosDSPAudioFormat format;
    final int sampleRate;
    final int size;

    AudioDispatcher dispatcher;
    OnProcessingFinishedHandler onProcessingFinishedHandler;

    public AudioRecorder(File file, TarsosDSPAudioFormat format, int sampleRate, int size) {
        this.file = file;
        this.format = format;
        this.sampleRate = sampleRate;
        this.size = size;
    }

    public void start() throws FileNotFoundException {
        this.stop();

        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, size, 0);
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
        AudioProcessor recordProcessor = new WriterProcessor(format, randomAccessFile);
        dispatcher.addAudioProcessor(recordProcessor);

        dispatcher.addAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                return true;
            }

            @Override
            public void processingFinished() { onProcessingFinishedHandler.onProcessingFinished(); }
        });

        Thread audioThread = new Thread(dispatcher, "Audio Thread");
        audioThread.start();
    }

    public void stop() {
        if (this.dispatcher != null) {
            if (!this.dispatcher.isStopped())
                this.dispatcher.stop();
            this.dispatcher = null;
        }
    }

    public void setOnProcessingFinishedHandler(OnProcessingFinishedHandler onProcessingFinishedHandler) {
        this.onProcessingFinishedHandler = onProcessingFinishedHandler;
    }
}
