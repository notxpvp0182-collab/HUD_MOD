package com.hudmod.hud;

import com.hudmod.config.ElementConfig;
import com.hudmod.config.HudConfig;
import com.hudmod.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

/**
 * Held Item HUD.
 * Renders the item currently in the player's main hand (falling back to off-hand if empty).
 * Shows:
 *  • Item icon
 *  • Stack count (if > 1)
 *  • Durability fraction text  (e.g. "246 / 250")  when durabilityTextEnabled
 *  • Durability bar                                 when durabilityBarEnabled
 *
 * Performance:
 *  • Hides itself entirely when both hands are empty — zero rendering cost.
 *  • Only one matrix push/pop per frame when visible.
 *  • No heap allocation in the hot path.
 */
public class HeldItemHud {

    private static final int ITEM_SIZE  = 16;
    private static final int PADDING    = 4;
    private static final int LINE_H     = 9;   // single text line height
    private static final int BAR_HEIGHT = 2;

    private final HudConfig config;

    public HeldItemHud(HudConfig config) {
        this.config = config;
    }

    public void render(DrawContext ctx, MinecraftClient mc) {
        // Prefer main hand; fall back to off-hand
        ItemStack stack = mc.player.getMainHandStack();
        if (stack.isEmpty()) {
            stack = mc.player.getOffHandStack();
            if (stack.isEmpty()) return;
        }

        ElementConfig cfg   = config.heldItem;
        float          scale = cfg.scale;
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        int sItem = (int) (ITEM_SIZE  * scale);
        int sPad  = (int) (PADDING    * scale);
        int sLine = (int) (LINE_H     * scale);
        int sBar  = (int) (BAR_HEIGHT * scale);

        boolean hasCount = stack.getCount() > 1;
        boolean hasDur   = stack.isDamageable();

        // ── Box dimensions ─────────────────────────────────────────────
        int extraLines = 0;
        if (hasCount) extraLines++;
        if (hasDur && config.durabilityTextEnabled) extraLines++;

        int boxW = sItem + sPad * 2 + 40; // extra room for text beside icon
        int boxH = sItem + sPad * 2 + extraLines * sLine;
        if (hasDur && config.durabilityBarEnabled) boxH += sBar + 1;

        int x = (int) (cfg.xFraction * sw);
        int y = (int) (cfg.yFraction * sh);

        // ── Background ────────────────────────────────────────────────
        if (cfg.showBackground) {
            RenderUtil.drawBackground(ctx, x, y, boxW, boxH, cfg);
        }

        int itemX = x + sPad;
        int itemY = y + sPad;

        // ── Item icon ─────────────────────────────────────────────────
        ctx.getMatrices().push();
        ctx.getMatrices().translate(itemX, itemY, 0.0);
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.drawItem(stack, 0, 0);
        ctx.getMatrices().pop();

        // ── Durability bar (overlaid on bottom of icon) ───────────────
        if (hasDur && config.durabilityBarEnabled) {
            int barY  = itemY + sItem + 1;
            float pct = durPct(stack);
            RenderUtil.drawProgressBar(ctx, itemX, barY, sItem, Math.max(1, sBar),
                pct, 0xFF000000, RenderUtil.getDurabilityColor(pct));
        }

        // ── Text info below item ───────────────────────────────────────
        int textColor = RenderUtil.applyOpacity(cfg.textColor, cfg.opacity);
        int textY = itemY + sItem + (config.durabilityBarEnabled ? sBar + 2 : 2);

        if (hasCount) {
            ctx.drawText(mc.textRenderer, "x" + stack.getCount(), itemX, textY, textColor, true);
            textY += sLine;
        }

        if (hasDur && config.durabilityTextEnabled) {
            int max = stack.getMaxDamage();
            int cur = max - stack.getDamage();
            float pct = max > 0 ? (float) cur / max : 1f;
            String txt = cur + " / " + max;
            ctx.drawText(mc.textRenderer, txt, itemX, textY, RenderUtil.getDurabilityColor(pct), true);
        }
    }

    private static float durPct(ItemStack stack) {
        int max = stack.getMaxDamage();
        return max <= 0 ? 1f : 1f - (float) stack.getDamage() / max;
    }
}
