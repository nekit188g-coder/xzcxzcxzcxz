package com.nameprotect;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class NameProtectClient implements ClientModInitializer {
    /** Справжній нік (для NameProtect-підміни). */
    public static volatile String realName = "";

    private static KeyMapping menuKey;

    @Override
    public void onInitializeClient() {
        Config.load();
        Media.start();

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.nameprotect.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                realName = client.player.getName().getString();
            }
            KillEffects.tick();
            while (menuKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new MenuScreen());
                }
            }
        });

        WorldRenderEvents.END_EXTRACTION.register(ctx -> {
            Proj.begin(ctx);
            Targets.update();
            TargetEsp.extract();
            KillEffects.extract();
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("nameprotect", "overlay"), (g, tick) -> {
            KillEffects.render(g);
            TargetEsp.render(g);
            TargetHud.render(g);
            MusicPlayer.render(g);
            Watermark.render(g);
        });

        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (level.isClientSide() && entity instanceof LivingEntity) {
                Targets.set(entity);
                KillEffects.watch(entity);
                HitSounds.play();
            }
            return InteractionResult.PASS;
        });
    }
}
