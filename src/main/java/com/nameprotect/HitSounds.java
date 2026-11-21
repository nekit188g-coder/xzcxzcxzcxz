package com.nameprotect;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;

public final class HitSounds {
    private static final SoundEvent UNDERTALE = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("nameprotect", "hit_undertale"));
    private static final SoundEvent BUBBLE = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("nameprotect", "hit_bubble"));
    private static final float[] VOL = {0.25f, 0.5f, 1.0f};

    private HitSounds() {}

    public static void play() {
        play(Config.hitSound, 1.0f);
    }

    /** Звук вбивства: той самий хітсаунд, але нижчим тоном. */
    public static void playKill() {
        play(Config.hitSound, 0.7f);
    }

    public static void play(int mode, float pitch) {
        if (mode <= 0) return;
        SoundEvent ev = mode == 1 ? UNDERTALE : BUBBLE;
        float vol = VOL[Math.max(0, Math.min(2, Config.volume))];
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev, pitch, vol));
    }
}
