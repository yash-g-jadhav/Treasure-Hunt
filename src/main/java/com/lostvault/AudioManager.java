package com.lostvault;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/** Tiny generated sound-effects layer; no external audio assets or device are required. */
public final class AudioManager {
    private static volatile boolean muted = false;
    private AudioManager() { }
    public static boolean muted() { return muted; }
    public static void toggleMute() { muted = !muted; }

    public static void forMessage(String message) {
        if (muted || message == null || message.isBlank()) return;
        String m = message.toLowerCase();
        if (m.contains("gem collected") || m.contains("cache opened")) tone(880, 70, 0.13);
        else if (m.contains("clue") && (m.contains("recovered") || m.contains("collected"))) chord(620, 830);
        else if (m.contains("hazard") || m.contains("life remaining")) tone(150, 190, 0.18);
        else if (m.contains("puzzle solved") || m.contains("vault unlock") || m.contains("gate grinds")) chord(490, 740, 980);
        else if (m.contains("secret found")) chord(520, 690, 1040);
        else if (m.contains("treasure claimed") || m.contains("true ending") || m.contains("treasure ending")) chord(440, 554, 659, 880);
    }

    private static void chord(double... frequencies) {
        Thread t = new Thread(() -> { for (double f : frequencies) { play(f, 85, 0.10); try { Thread.sleep(18); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; } } }, "lost-vault-audio");
        t.setDaemon(true); t.start();
    }
    private static void tone(double frequency, int millis, double amplitude) { Thread t = new Thread(() -> play(frequency, millis, amplitude), "lost-vault-audio-tone"); t.setDaemon(true); t.start(); }
    private static void play(double frequency, int millis, double amplitude) {
        if (muted) return;
        final float sampleRate = 22050f;
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        int samples = (int) (sampleRate * millis / 1000.0);
        byte[] data = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            double envelope = Math.min(1.0, Math.min(i / 180.0, (samples - i) / 220.0));
            short value = (short) (Math.sin(2 * Math.PI * frequency * i / sampleRate) * 32767 * amplitude * envelope);
            data[i * 2] = (byte) (value & 0xff); data[i * 2 + 1] = (byte) ((value >>> 8) & 0xff);
        }
        try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
            line.open(format, 4096); line.start(); line.write(data, 0, data.length); line.drain();
        } catch (Exception ignored) {
            // Audio is optional. Missing sound hardware must never stop gameplay.
        }
    }
}
