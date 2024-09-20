package com.example.pitchshifting.processor;

import android.media.AudioManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.GainProcessor;
import be.tarsos.dsp.PitchShifter;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.UniversalAudioInputStream;
import be.tarsos.dsp.io.android.AndroidAudioPlayer;

public class PitchShiftPlayer {
    final File file;
    final TarsosDSPAudioFormat format;
    final float factor;
    final int size;
    final int sampleRate;
    final int overlap;

    AudioDispatcher dispatcher;
    PitchShifter shifter;
    OnProcessingFinishedHandler onProcessingFinishedHandler;

    public PitchShiftPlayer(File file, TarsosDSPAudioFormat format, float factor, int size, int sampleRate, int overlap) {
        this.file = file;
        this.format = format;
        this.factor = factor;
        this.size = size;
        this.sampleRate = sampleRate;
        this.overlap = overlap;
    }

    public void play() throws FileNotFoundException {
        FileInputStream fileInputStream = new FileInputStream(file);
        TarsosDSPAudioFormat format = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, false);
        dispatcher = new AudioDispatcher(new UniversalAudioInputStream(fileInputStream, format), size, overlap);

        shifter = new PitchShifter(1.0 / factor, sampleRate, size, overlap);
        dispatcher.addAudioProcessor(shifter);

        GainProcessor gainProcessor = new GainProcessor(2.2);
        dispatcher.addAudioProcessor(gainProcessor);

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

    public void setFactor(float factor) {
        if (shifter != null) {
            shifter.setPitchShiftFactor(1.0f / factor);
        }
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
