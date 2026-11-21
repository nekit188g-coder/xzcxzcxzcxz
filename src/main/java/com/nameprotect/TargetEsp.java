package com.nameprotect;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** TargetESP: картинка поверх цілі (моб/гравець), на яку дивишся або яку вдарив. */
public final class TargetEsp {
    private static final String[] NAMES = {"radar", "bruh", "circle", "what", "triad", "fsociety", "photo", "sardelki"};
    private static final boolean[] SPINNABLE = {true, true, true, true, true, true, false, false};
    private static final float[] SIZE = {0.7f, 1.0f, 1.4f};
    private static final Identifier[] TEX = new Identifier[NAMES.length];
    private static final long HOLD_MS = 2000;

    private static Entity lastTarget;

    private static boolean hasSmooth;
    private static double sx, sy, sz;
    private static long lastNs;

    private static boolean visible;
    private static float ndcX, ndcY, ndcH;

    static {
        for (int i = 0; i < NAMES.length; i++) {
            TEX[i] = Identifier.fromNamespaceAndPath("nameprotect", "textures/target/" + NAMES[i] + ".png");
        }
    }

    private TargetEsp() {}

    /** Щокадру після збору стану світу (Proj.begin і Targets.update вже викликано). */
    public static void extract() {
        visible = false;
        if (!Config.targetEsp) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Entity t = Targets.get();
        if (t == null) {
            hasSmooth = false;
            return;
        }
        // невидимих (ефект Invisibility тощо) не показуємо
        if (Config.espIgnoreInvisible && t.isInvisible()) {
            hasSmooth = false;
            return;
        }
        if (Targets.age() > HOLD_MS) return;
        if (t != lastTarget) {
            lastTarget = t;
            hasSmooth = false;
        }

        long now = System.nanoTime();
        double dt = lastNs == 0 ? 1.0 : (now - lastNs) / 1.0e9;
        lastNs = now;
        if (!hasSmooth) {
            sx = t.getX(); sy = t.getY(); sz = t.getZ();
            hasSmooth = true;
        } else {
            double k = 1.0 - Math.exp(-dt * 25.0);
            sx += (t.getX() - sx) * k;
            sy += (t.getY() - sy) * k;
            sz += (t.getZ() - sz) * k;
        }

        double h = t.getBbHeight();
        float[] c = new float[2], top = new float[2], bot = new float[2];
        if (!Proj.project(sx, sy + h * 0.5, sz, c)) return;
        if (!Proj.project(sx, sy + h, sz, top)) return;
        if (!Proj.project(sx, sy, sz, bot)) return;

        ndcX = c[0];
        ndcY = c[1];
        ndcH = Math.abs(top[1] - bot[1]) * 0.5f;   // частка висоти екрана
        visible = true;
    }

    public static void render(GuiGraphics g) {
        if (!visible || !Config.targetEsp) return;
        int w = g.guiWidth(), h = g.guiHeight();
        float size = ndcH * h * 1.15f * SIZE[Math.max(0, Math.min(2, Config.size))];
        size = Math.max(16f, Math.min(h * 0.8f, size));
        float cx = (ndcX * 0.5f + 0.5f) * w;
        float cy = (0.5f - ndcY * 0.5f) * h;
        if (cx < -size || cx > w + size || cy < -size || cy > h + size) return;

        int m = Math.max(0, Math.min(NAMES.length - 1, Config.mode));
        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        if (Config.spin && SPINNABLE[m]) {
            float ang = (float) ((System.nanoTime() / 1.0e9) % 3.0 / 3.0 * (Math.PI * 2.0));
            g.pose().rotate(ang);
        }
        g.pose().scale(size / 256f);
        g.blit(RenderPipelines.GUI_TEXTURED, TEX[m], -128, -128, 0, 0, 256, 256, 256, 256);
        g.pose().popMatrix();
    }
}
