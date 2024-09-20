package com.example.pitchshifting.processor;

import android.media.AudioManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.UniversalAudioInputStream;
import be.tarsos.dsp.io.android.AndroidAudioPlayer;

public class AudioPlayer {
    final File file;
    final TarsosDSPAudioFormat format;
    final int sampleRate;
    final int size;

    AudioDispatcher dispatcher;
    OnProcessingFinishedHandler onProcessingFinishedHandler;

    public AudioPlayer(File file, TarsosDSPAudioFormat format, int sampleRate, int size) {
        this.file = file;
        this.format = format;
        this.sampleRate = sampleRate;
        this.size = size;
    }

    public void start() throws FileNotFoundException {
        stop();

        FileInputStream fileInputStream = new FileInputStream(file);
        dispatcher = new AudioDispatcher(new UniversalAudioInputStream(fileInputStream, format), size, 0);
        AudioProcessor playerProcessor = new AndroidAudioPlayer(format, sampleRate, AudioManager.STREAM_MUSIC);
        dispatcher.addAudioProcessor(playerProcessor);

        dispatcher.addAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                return true;
            }

            @Override
            public void processingFinished() {
                onProcessingFinishedHandler.onProcessingFinished();
            }
        });

        Thread audioThread = new Thread(dispatcher, "Audio Thread");
        audioThread.start();
    }

    public void setOnProcessingFinishedHandler(OnProcessingFinishedHandler onProcessingFinishedHandler) {
        this.onProcessingFinishedHandler = onProcessingFinishedHandler;
    }

    public void stop() {
        if (dispatcher != null) {
            if (!dispatcher.isStopped())
                dispatcher.stop();
            dispatcher = null;
        }
    }
}
