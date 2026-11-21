package com.nameprotect;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

/**
 * ВСІ виклики Minecraft API, яких не було в оригінальному моді, зібрані тут.
 * Якщо при збірці якесь ім'я не знайдеться — правити треба лише цей файл (позначено VERIFY).
 */
public final class Compat {
    private Compat() {}

    // ---------------------------------------------------------------- текст
    /** VERIFY: Minecraft.font = font, Font.width = width(String) */
    public static int textWidth(String s) {
        return Minecraft.getInstance().font.width(s);
    }

    /** VERIFY: GuiGraphics.drawString = drawString(Font, String, x, y, color, shadow) */
    public static void text(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(Minecraft.getInstance().font, s, x, y, color, false);
    }

    // ---------------------------------------------------------------- FPS
    private static int frames;
    private static long fpsWindowStart = System.nanoTime();
    private static int fps;

    /** Викликається раз на кадр з HUD-рендера. */
    public static void frame() {
        frames++;
        long now = System.nanoTime();
        if (now - fpsWindowStart >= 500_000_000L) {
            fps = Math.round(frames * 1.0e9f / (now - fpsWindowStart));
            frames = 0;
            fpsWindowStart = now;
        }
    }

    public static int fps() {
        return fps;
    }

    // ---------------------------------------------------------------- пінг
    /** VERIFY: Minecraft.getConnection = getConnection, ClientPacketListener.getPlayerInfo = getPlayerInfo(UUID), PlayerInfo.getLatency = getLatency */
    public static int ping() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return -1;
            ClientPacketListener conn = mc.getConnection();
            if (conn == null) return -1;
            PlayerInfo info = conn.getPlayerInfo(mc.player.getUUID());
            return info == null ? -1 : info.getLatency();
        } catch (Throwable t) {
            return -1;
        }
    }

    // ---------------------------------------------------------------- здоров'я / броня
    /** VERIFY: getHealth = getHealth, getMaxHealth = getMaxHealth, getAbsorptionAmount = getAbsorptionAmount */
    public static float health(LivingEntity e) {
        return e.getHealth();
    }

    public static float maxHealth(LivingEntity e) {
        return Math.max(1f, e.getMaxHealth());
    }

    public static float absorption(LivingEntity e) {
        return e.getAbsorptionAmount();
    }

    /** slot = "MAINHAND" / "HEAD" / "CHEST" / "LEGS" / "FEET". VERIFY: LivingEntity.getItemBySlot = getItemBySlot */
    public static ItemStack stackIn(LivingEntity e, String slot) {
        for (EquipmentSlot s : EquipmentSlot.values()) {
            if (s.name().equals(slot)) return e.getItemBySlot(s);
        }
        return null;
    }

    /** VERIFY: ItemStack.isEmpty = isEmpty */
    public static boolean isEmpty(ItemStack s) {
        return s == null || s.isEmpty();
    }

    /** VERIFY: GuiGraphics.renderItem = renderItem(ItemStack, x, y) */
    public static void item(GuiGraphics g, ItemStack stack, int x, int y, float scale) {
        var m = g.pose();
        m.pushMatrix();
        m.translate((float) x, (float) y);
        m.scale(scale);
        g.renderItem(stack, 0, 0);
        m.popMatrix();
    }

    // ---------------------------------------------------------------- голова гравця
    private static final Map<Class<?>, List<Method>> ACCESSORS = new HashMap<>();
    private static final Map<Class<?>, Method> SKIN_GETTER = new HashMap<>();
    private static final Method NONE;

    static {
        Method none = null;
        try {
            none = Object.class.getMethod("toString");
        } catch (Exception ignored) {
        }
        NONE = none;
    }

    /** Шукає геттер скіна (PlayerSkin.getSkin) через reflection — без жорстких залежностей. */
    private static Object skinOf(Entity e) {
        try {
            Class<?> k = e.getClass();
            Method m = SKIN_GETTER.get(k);
            if (m == null) {
                m = NONE;
                for (Method c : k.getMethods()) {
                    if (c.getParameterCount() == 0 && !Modifier.isStatic(c.getModifiers())
                            && c.getReturnType() == PlayerSkin.class) {
                        m = c;
                        break;
                    }
                }
                SKIN_GETTER.put(k, m);
            }
            return m == NONE ? null : m.invoke(e);
        } catch (Throwable t) {
            return null;
        }
    }

    private static List<Method> accessors(Class<?> k) {
        return ACCESSORS.computeIfAbsent(k, kk -> {
            List<Method> out = new ArrayList<>();
            try {
                if (kk.isRecord()) {
                    for (RecordComponent rc : kk.getRecordComponents()) {
                        Method a = rc.getAccessor();
                        a.setAccessible(true);
                        out.add(a);
                    }
                } else {
                    for (Method c : kk.getMethods()) {
                        if (c.getParameterCount() == 0 && !Modifier.isStatic(c.getModifiers())
                                && Identifier.class.isAssignableFrom(c.getReturnType())) {
                            out.add(c);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
            return out;
        });
    }

    private static boolean isTexturePath(Identifier id) {
        String s = id.toString();
        return s.contains("textures/") || s.contains("skins/");
    }

    private static Identifier findTexture(Object o, int depth) {
        if (o == null || depth > 3) return null;
        if (o instanceof Identifier id) return isTexturePath(id) ? id : null;
        try {
            for (Method m : accessors(o.getClass())) {
                Class<?> rt = m.getReturnType();
                if (rt.isPrimitive() || rt == String.class || rt.isEnum() || rt == Boolean.class) continue;
                Identifier r = findTexture(m.invoke(o), depth + 1);
                if (r != null) return r;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static Identifier skinTexture(Entity e) {
        return e == null ? null : findTexture(skinOf(e), 0);
    }

    public static void head(GuiGraphics g, Entity e, int x, int y, int size) {
        Identifier tex = skinTexture(e);
        if (tex == null) {
            Ui.rrect(g, x, y, size, size, 3, 0xFF2A2A3A);
            String s = "?";
            if (e != null) {
                String n = e.getName().getString();
                if (!n.isEmpty()) s = n.substring(0, 1).toUpperCase();
            }
            Ui.textC(g, s, x + size / 2, y + (size - Ui.fontH()) / 2 + 1, Ui.MID);
            return;
        }
        var m = g.pose();
        m.pushMatrix();
        m.translate((float) x, (float) y);
        m.scale(size / 8f);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, 0, 0, 8f, 8f, 8, 8, 64, 64);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, 0, 0, 40f, 8f, 8, 8, 64, 64);
        m.popMatrix();
    }

    // ---------------------------------------------------------------- динамічна текстура (обкладинка)
    private static final Identifier COVER_ID = Identifier.fromNamespaceAndPath("nameprotect", "media_cover");
    private static boolean coverWarned;

    /**
     * PNG -> NativeImage (NativeImage) -> DynamicTexture (DynamicTexture) -> TextureManager.register.
     * Усе через reflection, щоб збірка не залежала від точних імен. Повертає null, якщо не вдалося.
     */
    public static Identifier uploadCover(byte[] png) {
        try {
            Class<?> ni = NativeImage.class;
            Object img = null;
            for (Method m : ni.getMethods()) {
                if (Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == InputStream.class && m.getReturnType() == ni) {
                    img = m.invoke(null, new ByteArrayInputStream(png));
                    break;
                }
            }
            if (img == null) throw new IllegalStateException("NativeImage.read(InputStream) not found");

            Class<?> dt = DynamicTexture.class;
            Object tex = null;
            for (Constructor<?> c : dt.getConstructors()) {
                Class<?>[] p = c.getParameterTypes();
                if (p.length == 1 && p[0] == ni) {
                    tex = c.newInstance(img);
                } else if (p.length == 2 && p[0] == Supplier.class && p[1] == ni) {
                    Supplier<String> label = () -> "nameprotect_cover";
                    tex = c.newInstance(label, img);
                }
                if (tex != null) break;
            }
            if (tex == null) throw new IllegalStateException("DynamicTexture constructor not found");

            Minecraft mc = Minecraft.getInstance();
            Object tm = null;
            for (Method m : mc.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getReturnType() == TextureManager.class) {
                    tm = m.invoke(mc);
                    break;
                }
            }
            if (tm == null) throw new IllegalStateException("TextureManager not found");
            for (Method m : tm.getClass().getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 2 && p[0] == Identifier.class && p[1].isInstance(tex) && m.getReturnType() == void.class) {
                    m.invoke(tm, COVER_ID, tex);
                    return COVER_ID;
                }
            }
            throw new IllegalStateException("TextureManager.register not found");
        } catch (Throwable t) {
            if (!coverWarned) {
                coverWarned = true;
                System.err.println("[nameprotect] cover texture failed: " + t);
            }
            return null;
        }
    }
}
