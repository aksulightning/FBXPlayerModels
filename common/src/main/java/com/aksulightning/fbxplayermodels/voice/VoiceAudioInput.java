package com.aksulightning.fbxplayermodels.voice;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;
import java.util.function.BooleanSupplier;

/** Optional voice-chat bridge and local microphone fallback; neither depends on Minecraft classes. */
public final class VoiceAudioInput {
    public enum Status { OFF, VOICE_CHAT_WAITING, VOICE_CHAT_READY, MICROPHONE_STARTING, MICROPHONE_READY, MICROPHONE_UNAVAILABLE }
    private static final VoiceAudioEnvelope ENVELOPE = new VoiceAudioEnvelope();
    private static volatile VoiceShapeSettings settings = new VoiceShapeSettings();
    private static volatile boolean enabled, useVoiceChat;
    private static volatile Status status = Status.OFF;
    private static volatile BooleanSupplier voiceChatAllowed = () -> false;
    private static volatile BooleanSupplier voiceChatConnected = () -> false;
    private static volatile TargetDataLine microphone;
    private static Thread microphoneThread;
    private static volatile long microphoneGeneration;
    private static boolean microphoneFailed;

    private VoiceAudioInput() {}

    public static synchronized void configure(VoiceShapeSettings next, boolean voiceChatInstalled, boolean active) {
        VoiceShapeSettings copy = next.snapshot();
        boolean nextEnabled = active && copy.enabled();
        boolean nextVoiceChat = copy.input() == VoiceShapeSettings.Input.VOICE_CHAT
                || copy.input() == VoiceShapeSettings.Input.AUTO && voiceChatInstalled;
        boolean changed = enabled != nextEnabled || useVoiceChat != nextVoiceChat
                || settings.response() != copy.response() || settings.target() != copy.target()
                || !settings.name.equals(copy.name) || !settings.shapeKeyId.equals(copy.shapeKeyId);
        settings = copy;
        enabled = nextEnabled;
        useVoiceChat = nextVoiceChat;
        if (changed) { ENVELOPE.reset(); microphoneFailed = false; stopMicrophone(); }
        if (!enabled) { status = Status.OFF; return; }
        if (useVoiceChat) {
            boolean connected = voiceChatConnected.getAsBoolean();
            status = connected ? Status.VOICE_CHAT_READY : Status.VOICE_CHAT_WAITING;
            if (!connected || !voiceChatAllowed.getAsBoolean()) ENVELOPE.reset();
        } else if (microphoneThread == null && !microphoneFailed) {
            startMicrophone();
        }
    }

    public static void attachVoiceChat(BooleanSupplier allowed, BooleanSupplier connected) {
        voiceChatAllowed = allowed;
        voiceChatConnected = connected;
    }

    public static synchronized void acceptVoiceChat(short[] pcm) {
        if (enabled && useVoiceChat && voiceChatAllowed.getAsBoolean()) ENVELOPE.accept(pcm, settings);
    }

    public static float level() { return enabled ? ENVELOPE.sample(settings) : 0f; }
    public static Status status() { return status; }
    public static synchronized void stop() {
        enabled = false;
        ENVELOPE.reset();
        stopMicrophone();
        microphoneFailed = false;
        status = Status.OFF;
    }

    private static void startMicrophone() {
        status = Status.MICROPHONE_STARTING;
        long generation = ++microphoneGeneration;
        microphoneThread = new Thread(() -> capture(generation), "FBX Voice Shape microphone");
        microphoneThread.setDaemon(true);
        microphoneThread.start();
    }

    private static void capture(long generation) {
        TargetDataLine line = null;
        try {
            AudioFormat format = new AudioFormat(48000f, 16, 1, true, false);
            line = AudioSystem.getTargetDataLine(format);
            line.open(format, 9600);
            synchronized (VoiceAudioInput.class) {
                if (generation != microphoneGeneration || !enabled || useVoiceChat) return;
                microphone = line;
                line.start();
                status = Status.MICROPHONE_READY;
            }
            byte[] bytes = new byte[1920];
            short[] pcm = new short[960];
            int filled = 0;
            while (enabled && !useVoiceChat && generation == microphoneGeneration) {
                int read = line.read(bytes, filled, bytes.length - filled);
                if (read <= 0) break;
                filled += read;
                if (filled < bytes.length) continue;
                for (int i = 0; i < pcm.length; i++) pcm[i] = (short) ((bytes[i * 2] & 255) | bytes[i * 2 + 1] << 8);
                synchronized (VoiceAudioInput.class) {
                    if (generation == microphoneGeneration) ENVELOPE.accept(pcm, settings);
                }
                filled = 0;
            }
        } catch (Exception exception) {
            synchronized (VoiceAudioInput.class) {
                if (generation == microphoneGeneration) {
                    microphoneFailed = true;
                    status = Status.MICROPHONE_UNAVAILABLE;
                    System.getLogger(VoiceAudioInput.class.getName()).log(System.Logger.Level.WARNING,
                            "Voice Shape could not open the default microphone", exception);
                }
            }
        } finally {
            if (line != null) line.close();
            synchronized (VoiceAudioInput.class) {
                if (generation == microphoneGeneration) {
                    microphone = null;
                    microphoneThread = null;
                    ENVELOPE.reset();
                    if (enabled && !useVoiceChat) {
                        microphoneFailed = true;
                        status = Status.MICROPHONE_UNAVAILABLE;
                    }
                }
            }
        }
    }

    private static void stopMicrophone() {
        ++microphoneGeneration;
        TargetDataLine line = microphone;
        microphone = null;
        microphoneThread = null;
        if (line != null) line.close();
    }
}
