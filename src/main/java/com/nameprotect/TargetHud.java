package com.nameprotect;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.GuiGraphics;

/** TargetHUD: плашка з головою, ніком, смугою HP і бронею цілі. */
public final class TargetHud {
    private static final String[] SLOTS = {"MAINHAND", "HEAD", "CHEST", "LEGS", "FEET"};
    private static final int W = 104, H = 34;

    private static Entity shown;
    private static float fade;
    private static float shownHp = -1f;

    private TargetHud() {}

    public static void render(GuiGraphics g) {
        Entity t = Config.targetHud ? Targets.get() : null;
        if (t != null) {
            if (t != shown) shownHp = -1f;
            shown = t;
        }
        fade += ((t != null ? 1f : 0f) - fade) * 0.18f;
        if (fade < 0.02f || shown == null) {
            if (fade < 0.02f) shown = null;
            return;
        }
        Entity e = shown;
        int sw = g.guiWidth(), sh = g.guiHeight();
        int x = Config.hudX >= 0 ? Config.hudX : (int) (sw * 0.2f);
        int y = Config.hudY >= 0 ? Config.hudY : (int) (sh * 0.1f);
        float a = fade;

        float hp = 0f, max = 1f;
        if (e instanceof LivingEntity le) {
            hp = Compat.health(le);
            max = Compat.maxHealth(le);
        }
        shownHp = shownHp < 0f ? hp : shownHp + (hp - shownHp) * 0.15f;
        float frac = Math.max(0f, Math.min(1f, shownHp / max));

        // фон
        Ui.rrect(g, x, y, W, H, 5, Ui.alpha(0xE0121219, a));

        // голова + нік
        Compat.head(g, e, x + 6, y + 6, 16);
        String name = e.getName().getString();
        while (name.length() > 3 && Ui.tw(name) > W - 34) name = name.substring(0, name.length() - 1);
        Ui.text(g, name, x + 27, y + 7, Ui.alpha(Ui.TXT, a));

        // смуга HP + число
        String hpText = Math.round(shownHp) + "hp";
        int hpW = Ui.tw(hpText);
        Ui.text(g, hpText, x + W - 6 - hpW, y + 22, Ui.alpha(Ui.ACCENT, a));
        int bx = x + 6, bw = W - 12 - hpW - 5, by = y + 25;
        Ui.rrect(g, bx, by, bw, 4, 2, Ui.alpha(0xFF262638, a));
        int fw = Math.max(frac > 0f ? 2 : 0, Math.round(bw * frac));
        if (fw > 0) Ui.rrect(g, bx, by, fw, 4, 2, Ui.alpha(Ui.ACCENT, a));

        // броня / предмет у руці
        if (Config.hudArmor && e instanceof LivingEntity le && a > 0.9f) {
            ItemStack[] items = new ItemStack[SLOTS.length];
            int n = 0;
            for (int i = 0; i < SLOTS.length; i++) {
                ItemStack s = Compat.stackIn(le, SLOTS[i]);
                if (!Compat.isEmpty(s)) items[n++] = s;
            }
            if (n > 0) {
                int iw = n * 14 + 6;
                int ix = x + 2;
                int iy = y + H + 3;
                Ui.rrect(g, ix, iy, iw, 18, 4, 0xC0121219);
                for (int i = 0; i < n; i++) {
                    Compat.item(g, items[i], ix + 4 + i * 14, iy + 3, 0.75f);
                }
            }
        }
    }
}
