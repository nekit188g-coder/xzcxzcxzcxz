package com.nameprotect;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;

/** Поточна ціль (на яку дивишся або яку вдарив). Спільна для TargetESP і TargetHUD. */
public final class Targets {
    /** Скільки мс ціль залишається актуальною після останнього погляду/удару. */
    public static final long HOLD_MS = 3000;

    private static Entity target;
    private static long time;

    private Targets() {}

    public static void set(Entity e) {
        target = e;
        time = System.currentTimeMillis();
    }

    public static Entity get() {
        return target;
    }

    public static long age() {
        return System.currentTimeMillis() - time;
    }

    /** Щокадру. */
    public static void update() {
        if (!Config.targetEsp && !Config.targetHud) {
            target = null;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            target = null;
            return;
        }
        HitResult hr = mc.hitResult;
        if (hr instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le
                && le != mc.player && le.isAlive()) {
            set(le);
        }
        Entity t = target;
        if (t != null && (!t.isAlive() || t.isRemoved() || age() > HOLD_MS)) {
            target = null;
        }
    }
}
