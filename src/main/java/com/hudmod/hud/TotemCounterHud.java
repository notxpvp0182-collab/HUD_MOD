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

        ItemStack totemStack = new ItemStack(Items.TOTEM_OF_UNDYING);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x + sPad, y + sPad, 0.0);
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.drawItem(totemStack, 0, 0);
        ctx.getMatrices().pop();

        int textColor;
        if (cachedCount == 0) {
            float pulse = RenderUtil.getPulse(800L);
            textColor   = RenderUtil.blendColors(0xFFFF5555, 0xFFFFFFFF, pulse);
        } else {
            textColor = RenderUtil.applyOpacity(cfg.textColor, cfg.opacity);
        }

        String text = "\u00d7" + cachedCount;
        ctx.drawText(mc.textRenderer, text,
                x + sPad + sIcon + 2,
                y + sPad + sIcon / 2 - 4,
                textColor, true);
    }

    // ── FIX: শুধু একবার loop করো — inventory.size() সব slot cover করে ──
    private static int countTotems(MinecraftClient mc) {
        int count = 0;
        // getInventory().size() = 36 main + 4 armor + 1 offhand = 41
        // এতে main hand ও offhand সব include আছে — আলাদা count দরকার নেই
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (s.isOf(Items.TOTEM_OF_UNDYING)) count += s.getCount();
        }
        return count;
    }
                        }
