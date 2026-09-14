package com.aksulightning.fbxplayermodels.voice;

import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientSoundEvent;
import de.maxhenkel.voicechat.api.events.ClientVoicechatInitializationEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;

/** Loaded only by Simple Voice Chat's optional Fabric entrypoint. Safe on a dedicated server. */
public final class SimpleVoiceChatPlugin implements VoicechatPlugin {
    @Override
    public String getPluginId() { return "fbx-player-models"; }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(ClientVoicechatInitializationEvent.class, event -> {
            var api = event.getVoicechat();
            VoiceAudioInput.attachVoiceChat(() -> !api.isMuted() && !api.isDisabled() && !api.isDisconnected(),
                    () -> !api.isDisconnected());
        });
        registration.registerEvent(ClientSoundEvent.class, event -> {
            if (!event.isCancelled()) VoiceAudioInput.acceptVoiceChat(event.getRawAudio());
        }, -100);
    }
}
