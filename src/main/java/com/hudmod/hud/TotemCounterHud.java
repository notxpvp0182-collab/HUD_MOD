package com.hudmod.hud;

import com.hudmod.config.ElementConfig;
import com.hudmod.config.HudConfig;
import com.hudmod.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;

public class TotemCounterHud {

    private static final int REFRESH_TICKS = 7;
    private static final int PAD   = 4;
    private static final int ICON  = 16;
    private static final int BOX_W = ICON + PAD * 2 + 26;
    private static final int BOX_H = ICON + PAD * 2;

    private final HudConfig config;
    private int  cachedCount = 0;
    private int  tickTimer   = 0;

    public TotemCounterHud(HudConfig config) {
        this.config = config;
    }

    public void onTick(MinecraftClient mc) {
        if (mc.player == null) return;
        if (++tickTimer >= REFRESH_TICKS) {
            tickTimer   = 0;
            cachedCount = countTotems(mc);
        }
    }

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

        // 1.21.10 — Matrix3x2fStack
        ItemStack totemStack = new ItemStack(Items.TOTEM_OF_UNDYING);
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x + sPad, y + sPad);
        ctx.getMatrices().scale(scale, scale);
        ctx.drawItem(totemStack, 0, 0);
        ctx.getMatrices().popMatrix();

        int textColor;
        if (cachedCount == 0) {
            float pulse = RenderUtil.getPulse(800L);
            textColor   = RenderUtil.blendColors(0xFFFF5555, 0xFFFFFFFF, pulse);
        } else {
            textColor = RenderUtil.applyOpacity(cfg.textColor, cfg.opacity);
        }

        ctx.drawText(mc.textRenderer, "\u00d7" + cachedCount,
                x + sPad + sIcon + 2,
                y + sPad + sIcon / 2 - 4,
                textColor, true);
    }

    private static int countTotems(MinecraftClient mc) {
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (s.isOf(Items.TOTEM_OF_UNDYING)) count += s.getCount();
        }
        return count;
    }
            }
