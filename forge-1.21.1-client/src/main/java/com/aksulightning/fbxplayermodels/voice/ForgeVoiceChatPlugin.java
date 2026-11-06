package com.aksulightning.fbxplayermodels.voice;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;

/** Discovered by Simple Voice Chat only when that optional mod is installed. */
@ForgeVoicechatPlugin
public final class ForgeVoiceChatPlugin implements VoicechatPlugin {
    private final SimpleVoiceChatPlugin delegate = new SimpleVoiceChatPlugin();

    @Override
    public String getPluginId() { return delegate.getPluginId(); }

    @Override
    public void registerEvents(EventRegistration registration) { delegate.registerEvents(registration); }
}
