package com.nameprotect;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.GuiGraphics;

/** Тексти меню — картинки, відрендерені шрифтом arialmt.ttf. */
public final class Labels {
    public static final int H = 43;
    private static final Map<String, Integer> W = new HashMap<>();
    private static final Map<String, Identifier> TEX = new HashMap<>();

    static {
        W.put("wm", 189);
        W.put("targetesp", 189);
        W.put("mode", 107);
        W.put("m1", 154);
        W.put("m2", 132);
        W.put("m3", 147);
        W.put("m4", 130);
        W.put("m5", 139);
        W.put("m6", 199);
        W.put("m7", 216);
        W.put("m8", 192);
        W.put("hitsounds", 179);
        W.put("h0", 51);
        W.put("h1", 219);
        W.put("h2", 172);
        W.put("watermark", 186);
        W.put("on", 54);
        W.put("off", 78);
        W.put("killeffect", 155);
        W.put("killsound", 169);
        W.put("nameprotect", 232);
        W.put("spin", 83);
        W.put("size", 92);
        W.put("s", 30);
        W.put("m", 30);
        W.put("l", 30);
        W.put("volume", 147);
        W.put("v25", 86);
        W.put("v50", 86);
        W.put("v100", 108);
    }

    private Labels() {}

    public static int width(String id) {
        Integer w = W.get(id);
        return w == null ? 0 : w;
    }

    public static void draw(GuiGraphics g, String id, int x, int y, float scale) {
        Integer w = W.get(id);
        if (w == null) return;
        Identifier tex = TEX.computeIfAbsent(id, k -> Identifier.fromNamespaceAndPath("nameprotect", "textures/gui/label/" + k + ".png"));
        g.pose().pushMatrix();
        g.pose().translate((float) x, (float) y);
        g.pose().scale(scale);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, 0, 0, 0, 0, w, H, w, H);
        g.pose().popMatrix();
    }
}
