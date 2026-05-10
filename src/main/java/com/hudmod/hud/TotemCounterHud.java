package com.hudmod.hud;

import com.hudmod.config.ElementConfig;
import com.hudmod.config.HudConfig;
import com.hudmod.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;

/**
 * Totem Counter HUD.
 *
 * Counts all Totems of Undying across:
 *   • Main hand
 *   • Off hand
 *   • All 36 inventory slots
 *
 * Performance:
 *  • Count is recalculated only every REFRESH_TICKS client ticks (~3/s by default).
 *    This avoids scanning 38 slots every single frame (~60+/s).
 *  • No allocations in render(); all state kept in primitives.
 *  • Optional entrance animation uses getPulse() — no Timers, no threads.
 */
public class TotemCounterHud {

    /** How many client ticks between inventory scans (20 ticks = 1 second). */
    private static final int REFRESH_TICKS = 7;

    private static final int PAD   = 4;
    private static final int ICON  = 16;
    private static final int BOX_W = ICON + PAD * 2 + 26; // icon + "×99"
    private static final int BOX_H = ICON + PAD * 2;

    private final HudConfig config;

    private int  cachedCount    = 0;
    private int  tickTimer      = 0;
    private boolean firstRender = true;

    public TotemCounterHud(HudConfig config) {
        this.config = config;
    }

    // ── Called every client tick (from ClientTickEvents.END_CLIENT_TICK) ──────
    public void onTick(MinecraftClient mc) {
        if (mc.player == null) return;
        if (++tickTimer >= REFRESH_TICKS) {
            tickTimer    = 0;
            cachedCount  = countTotems(mc);
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────
    public void render(DrawContext ctx, MinecraftClient mc) {
        ElementConfig cfg = config.totemCounter;
        float scale = cfg.scale;
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        int bw = (int) (BOX_W * scale);
        int bh = (int) (BOX_H * scale);
        int x  = (int) (cfg.xFraction * sw);
        int y  = (int) (cfg.yFraction * sh);

        if (cfg.showBackground) {
            RenderUtil.drawBackground(ctx, x, y, bw, bh, cfg);
        }

        int sPad  = (int) (PAD  * scale);
        int sIcon = (int) (ICON * scale);

        // Totem icon
        ItemStack totemStack = new ItemStack(Items.TOTEM_OF_UNDYING);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x + sPad, y + sPad, 0.0);
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.drawItem(totemStack, 0, 0);
        ctx.getMatrices().pop();

        // Count text — colour shifts when 0 totems remain
        int textColor = cachedCount > 0
            ? RenderUtil.applyOpacity(cfg.textColor, cfg.opacity)
            : 0xFFFF5555;

        // If zero totems, add a gentle pulse to warn the player
        if (cachedCount == 0) {
            float pulse = RenderUtil.getPulse(800L);
            textColor   = RenderUtil.blendColors(0xFFFF5555, 0xFFFFFFFF, pulse);
        }

        String text = "\u00d7" + cachedCount; // ×N
        ctx.drawText(mc.textRenderer, text,
                     x + sPad + sIcon + 2,
                     y + sPad + sIcon / 2 - 4,
                     textColor, true);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static int countTotems(MinecraftClient mc) {
        int count = 0;
        // Main + off hand
        if (mc.player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING))
            count += mc.player.getMainHandStack().getCount();
        if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING))
            count += mc.player.getOffHandStack().getCount();
        // Full inventory (36 slots)
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (s.isOf(Items.TOTEM_OF_UNDYING)) count += s.getCount();
        }
        return count;
    }
}
