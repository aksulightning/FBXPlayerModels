package com.aksulightning.fbxplayermodels.voice;

/** Stores scalar levels only. Audio frames are consumed immediately and never retained. */
final class VoiceAudioEnvelope {
    private float volume, baseline, previous, smoothed;
    private boolean aboveThreshold;
    private long lastFrame, pulseUntil, lastRead, lastPulse;

    synchronized void accept(short[] pcm, VoiceShapeSettings settings) {
        if (pcm == null || pcm.length == 0) { volume = 0f; return; }
        double square = 0;
        for (short sample : pcm) square += (double) sample * sample;
        float rms = (float) (Math.sqrt(square / pcm.length) / 32768.0);
        long now = System.nanoTime();
        if (now - lastFrame > 250_000_000L) { baseline = 0f; previous = 0f; aboveThreshold = false; }
        volume = VoiceShapeSettings.clamp(rms * settings.sensitivity, 0f, 1f);
        float threshold = Math.max(0.001f, settings.threshold);
        boolean loud = volume >= threshold;
        boolean onset = loud && volume - previous > Math.max(0.04f, threshold)
                && volume > baseline * 1.35f;
        if ((settings.response() == VoiceShapeSettings.Response.RHYTHM && onset
                || settings.response() == VoiceShapeSettings.Response.TRIGGER && loud && !aboveThreshold)
                && now - lastPulse >= 100_000_000L) {
            pulseUntil = now + 200_000_000L;
            lastPulse = now;
        }
        if (volume < threshold * 0.7f) aboveThreshold = false;
        else if (loud) aboveThreshold = true;
        baseline += (volume - baseline) * 0.06f;
        previous = volume;
        lastFrame = now;
    }

    synchronized float sample(VoiceShapeSettings settings) {
        long now = System.nanoTime();
        float desired = 0f;
        if (now - lastFrame <= 250_000_000L) {
            desired = settings.response() == VoiceShapeSettings.Response.VOLUME
                    ? Math.max(0f, (volume - settings.threshold) / (1f - settings.threshold))
                    : Math.max(0f, (pulseUntil - now) / 200_000_000f);
        }
        float dt = lastRead == 0L ? 0.05f : Math.min(0.1f, (now - lastRead) / 1_000_000_000f);
        lastRead = now;
        float duration = desired > smoothed ? 0.04f : 0.14f;
        smoothed += (desired - smoothed) * (1f - (float) Math.exp(-dt / duration));
        return smoothed < 0.0001f ? 0f : smoothed;
    }

    synchronized void reset() {
        volume = baseline = previous = smoothed = 0f;
        aboveThreshold = false;
        lastFrame = pulseUntil = lastRead = lastPulse = 0L;
    }
}
