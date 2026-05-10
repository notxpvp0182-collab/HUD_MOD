package com.hudmod.hud;

import com.hudmod.config.ElementConfig;
import com.hudmod.config.HudConfig;
import com.hudmod.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

/**
 * Armor HUD — draws Helmet, Chestplate, Leggings, Boots independently.
 *
 * Performance:
 *  • No per-frame allocations.
 *  • Matrix push/pop only when rendering an item (infrequent).
 *  • Durability calculation is integer arithmetic only.
 *  • Warning pulse uses cached System.currentTimeMillis result from RenderUtil.
 */
public class ArmorHud {

    // Base sizes in UI pixels (before scale)
    private static final int ITEM_SIZE   = 16;
    private static final int PADDING     = 4;
    private static final int TEXT_WIDTH  = 34;   // approximate width of "100%" text
    private static final int BAR_HEIGHT  = 2;

    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    };

    private final HudConfig config;

    public ArmorHud(HudConfig config) {
        this.config = config;
    }

    public void render(DrawContext ctx, MinecraftClient mc) {
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        ElementConfig[] cfgs    = config.armorConfigs();
        boolean[]       enabled = config.armorEnabled();

        for (int i = 0; i < 4; i++) {
            if (!enabled[i]) continue;
            ItemStack stack = mc.player.getEquippedStack(SLOTS[i]);
            renderPiece(ctx, mc, stack, cfgs[i], sw, sh);
        }
    }

    private void renderPiece(DrawContext ctx, MinecraftClient mc,
                              ItemStack stack, ElementConfig cfg,
                              int sw, int sh) {
        float scale      = cfg.scale;
        int   sItem      = (int) (ITEM_SIZE  * scale);
        int   sPad       = (int) (PADDING    * scale);
        int   sTextW     = (int) (TEXT_WIDTH * scale);
        int   sBarH      = (int) (BAR_HEIGHT * scale);

        boolean hasDur = !stack.isEmpty() && stack.isDamageable();
        float   durPct = hasDur ? durPct(stack) : 1f;

        // Calculate box dimensions
        int boxW = sItem + sPad * 2;
        int boxH = sItem + sPad * 2;
        if (hasDur && config.durabilityTextEnabled) boxW += sTextW + 2;
        if (hasDur && config.durabilityBarEnabled)  boxH += sBarH + 1;

        int x = (int) (cfg.xFraction * sw);
        int y = (int) (cfg.yFraction * sh);

        // ── Background ────────────────────────────────────────────────
        if (cfg.showBackground) {
            RenderUtil.drawBackground(ctx, x, y, boxW, boxH, cfg);
        }

        // ── Low-durability warning overlay (pulsing red) ───────────────
        if (hasDur && durPct < 0.2f) {
            int alpha = (int) (RenderUtil.getPulse(900L) * 85);
            ctx.fill(x, y, x + boxW, y + boxH, (alpha << 24) | 0xFF0000);
        }

        // ── Item icon ─────────────────────────────────────────────────
        int itemX = x + sPad;
        int itemY = y + sPad;

        if (!stack.isEmpty()) {
            ctx.getMatrices().push();
            ctx.getMatrices().translate(itemX, itemY, 0.0);
            ctx.getMatrices().scale(scale, scale, 1f);
            ctx.drawItem(stack, 0, 0);
            ctx.getMatrices().pop();
        } else {
            // Empty slot ghost
            ctx.fill(itemX, itemY, itemX + sItem, itemY + sItem, 0x20FFFFFF);
        }

        // ── Durability bar ────────────────────────────────────────────
        if (hasDur && config.durabilityBarEnabled) {
            int barY = itemY + sItem + 1;
            RenderUtil.drawProgressBar(ctx, itemX, barY, sItem, Math.max(1, sBarH),
                durPct, 0xFF000000, RenderUtil.getDurabilityColor(durPct));
        }

        // ── Durability text ───────────────────────────────────────────
        if (hasDur && config.durabilityTextEnabled) {
            int textX = itemX + sItem + 3;
            int textY = itemY + sItem / 2 - 4;
            String text = (int) (durPct * 100) + "%";
            ctx.drawText(mc.textRenderer, text, textX, textY,
                         RenderUtil.getDurabilityColor(durPct), true);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static float durPct(ItemStack stack) {
        int max = stack.getMaxDamage();
        return max <= 0 ? 1f : 1f - (float) stack.getDamage() / max;
    }

    /** Used by the editor to draw a bounding box around each armor element. */
    public int getBoxWidth(ElementConfig cfg) {
        float scale = cfg.scale;
        int w = (int) ((ITEM_SIZE + PADDING * 2) * scale);
        if (config.durabilityTextEnabled) w += (int) ((TEXT_WIDTH + 2) * scale);
        return w;
    }

    public int getBoxHeight(ElementConfig cfg) {
        float scale = cfg.scale;
        int h = (int) ((ITEM_SIZE + PADDING * 2) * scale);
        if (config.durabilityBarEnabled) h += (int) ((BAR_HEIGHT + 1) * scale);
        return h;
    }
}
