package com.hudmod.gui;

import com.hudmod.config.ElementConfig;
import com.hudmod.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

/**
 * Lightweight right-click popup menu attached to a single HUD element.
 *
 * Supports:
 *   Scale slider, Opacity slider, Background toggle, Border toggle,
 *   Corner radius slider, Reset to default.
 *
 * All interaction is handled via raw mouse events — no heavy Widget system
 * to keep draw calls and object counts minimal (important on Android).
 *
 * The popup is owned and managed by HudEditorScreen; it draws itself on top.
 */
public class ElementPopupMenu {

    // Layout constants (unscaled UI pixels)
    private static final int W     = 160;
    private static final int ROW_H = 14;
    private static final int PAD   = 4;
    private static final int CORNER= 4;

    private static final int BG_COLOR  = 0xE5181818;
    private static final int BAR_BG    = 0xFF333333;
    private static final int BAR_FG    = 0xFF4A90D9;
    private static final int TEXT_COL  = 0xFFEEEEEE;
    private static final int LABEL_COL = 0xFF999999;
    private static final int SEP_COL   = 0xFF333333;
    private static final int HOVER_COL = 0x30FFFFFF;

    // Row indices
    private static final int ROW_SCALE      = 0;
    private static final int ROW_OPACITY    = 1;
    private static final int ROW_BACKGROUND = 2;
    private static final int ROW_BORDER     = 3;
    private static final int ROW_RADIUS     = 4;
    private static final int ROW_RESET      = 5;
    private static final int ROW_COUNT      = 6;

    private int x, y;
    private final ElementConfig cfg;
    private final ElementConfig defaultCfg;
    private final String label;

    private int  activeSlider = -1;   // which slider row is being dragged
    private int  hoveredRow   = -1;

    public ElementPopupMenu(int x, int y, String label, ElementConfig cfg) {
        this.x          = x;
        this.y          = y;
        this.label      = label;
        this.cfg        = cfg;
        this.defaultCfg = cfg.copy();   // snapshot for Reset
    }

    /** Clamp popup so it stays on screen. */
    public void clampToScreen(int sw, int sh) {
        int h = totalHeight();
        if (x + W > sw) x = sw - W - 2;
        if (y + h > sh) y = sh - h - 2;
        if (x < 0) x = 2;
        if (y < 0) y = 2;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    public void render(DrawContext ctx, int mouseX, int mouseY) {
        int h = totalHeight();
        RenderUtil.drawRoundedRect(ctx, x, y, W, h, CORNER, BG_COLOR);
        RenderUtil.drawBorder(ctx, x, y, W, h, 0xFF444444);

        // Title
        ctx.drawText(MinecraftClient.getInstance().textRenderer,
                     label, x + PAD, y + PAD, 0xFFFFFFFF, true);

        int rowTop = y + PAD + 10 + 2; // below title + separator

        ctx.fill(x + PAD, rowTop - 1, x + W - PAD, rowTop, SEP_COL); // title separator

        hoveredRow = -1;
        for (int i = 0; i < ROW_COUNT; i++) {
            int ry = rowTop + i * ROW_H;
            boolean hover = mouseX >= x && mouseX <= x + W && mouseY >= ry && mouseY < ry + ROW_H;
            if (hover) hoveredRow = i;
            if (hover) ctx.fill(x, ry, x + W, ry + ROW_H, HOVER_COL);
            drawRow(ctx, i, ry);
        }
    }

    private void drawRow(DrawContext ctx, int row, int ry) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int labelX = x + PAD;

        switch (row) {
            case ROW_SCALE -> {
                ctx.drawText(mc.textRenderer, "Scale", labelX, ry + 3, LABEL_COL, false);
                drawSlider(ctx, ry, cfg.scale, 0.25f, 3.0f);
                ctx.drawText(mc.textRenderer, String.format("%.1fx", cfg.scale), x + W - 30, ry + 3, TEXT_COL, false);
            }
            case ROW_OPACITY -> {
                ctx.drawText(mc.textRenderer, "Opacity", labelX, ry + 3, LABEL_COL, false);
                drawSlider(ctx, ry, cfg.opacity, 0.1f, 1.0f);
                ctx.drawText(mc.textRenderer, (int)(cfg.opacity*100)+"%", x + W - 30, ry + 3, TEXT_COL, false);
            }
            case ROW_BACKGROUND -> {
                ctx.drawText(mc.textRenderer, "Background", labelX, ry + 3, LABEL_COL, false);
                drawToggle(ctx, ry, cfg.showBackground);
            }
            case ROW_BORDER -> {
                ctx.drawText(mc.textRenderer, "Border", labelX, ry + 3, LABEL_COL, false);
                drawToggle(ctx, ry, cfg.showBorder);
            }
            case ROW_RADIUS -> {
                ctx.drawText(mc.textRenderer, "Corners", labelX, ry + 3, LABEL_COL, false);
                drawSlider(ctx, ry, cfg.cornerRadius, 0f, 8f);
                ctx.drawText(mc.textRenderer, (int)cfg.cornerRadius+"px", x + W - 30, ry + 3, TEXT_COL, false);
            }
            case ROW_RESET -> {
                ctx.drawText(mc.textRenderer, "\u00a7cReset to Default", labelX, ry + 3, TEXT_COL, false);
            }
        }
    }

    // ── Slider / toggle drawing ───────────────────────────────────────────────

    private static final int SLIDER_X_OFF = 55;
    private static final int SLIDER_W     = 65;
    private static final int SLIDER_H     = 4;

    private void drawSlider(DrawContext ctx, int ry, float value, float min, float max) {
        int sx  = x + SLIDER_X_OFF;
        int sy  = ry + (ROW_H - SLIDER_H) / 2;
        float t = (value - min) / (max - min);
        int fill = (int) (SLIDER_W * Math.max(0f, Math.min(1f, t)));
        ctx.fill(sx, sy, sx + SLIDER_W, sy + SLIDER_H, BAR_BG);
        ctx.fill(sx, sy, sx + fill,      sy + SLIDER_H, BAR_FG);
        // Thumb
        int tx = sx + fill - 2;
        ctx.fill(tx, sy - 2, tx + 4, sy + SLIDER_H + 2, 0xFFFFFFFF);
    }

    private void drawToggle(DrawContext ctx, int ry, boolean on) {
        int tx = x + W - PAD - 20;
        int ty = ry + ROW_H / 2 - 3;
        ctx.fill(tx, ty, tx + 20, ty + 6, on ? 0xFF27AE60 : 0xFF555555);
        ctx.fill(on ? tx + 14 : tx, ty, (on ? tx + 20 : tx + 6), ty + 6, 0xFFFFFFFF);
    }

    // ── Mouse interaction ─────────────────────────────────────────────────────

    /** Returns true if the popup consumed this click. */
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!isOver(mouseX, mouseY)) return false;

        int rowTop = y + PAD + 12;
        for (int i = 0; i < ROW_COUNT; i++) {
            int ry = rowTop + i * ROW_H;
            if (mouseY >= ry && mouseY < ry + ROW_H) {
                handleRowClick(i, mouseX, ry, button);
                return true;
            }
        }
        return true; // consume anyway (clicked bg)
    }

    private void handleRowClick(int row, int mouseX, int ry, int button) {
        switch (row) {
            case ROW_SCALE      -> { activeSlider = ROW_SCALE;   dragSlider(mouseX, cfg.scale, 0.25f, 3.0f, v -> cfg.scale = v); }
            case ROW_OPACITY    -> { activeSlider = ROW_OPACITY; dragSlider(mouseX, cfg.opacity, 0.1f, 1.0f, v -> cfg.opacity = v); }
            case ROW_BACKGROUND -> cfg.showBackground = !cfg.showBackground;
            case ROW_BORDER     -> cfg.showBorder     = !cfg.showBorder;
            case ROW_RADIUS     -> { activeSlider = ROW_RADIUS;  dragSlider(mouseX, cfg.cornerRadius, 0f, 8f, v -> cfg.cornerRadius = v); }
            case ROW_RESET      -> resetToDefault();
        }
    }

    public boolean mouseDragged(int mouseX, int mouseY, int button) {
        if (activeSlider < 0) return false;
        switch (activeSlider) {
            case ROW_SCALE   -> cfg.scale        = sliderValue(mouseX, 0.25f, 3.0f);
            case ROW_OPACITY -> cfg.opacity      = sliderValue(mouseX, 0.1f,  1.0f);
            case ROW_RADIUS  -> cfg.cornerRadius = sliderValue(mouseX, 0f,    8.0f);
        }
        return true;
    }

    public void mouseReleased() {
        activeSlider = -1;
    }

    private float sliderValue(int mouseX, float min, float max) {
        int sx = x + SLIDER_X_OFF;
        float t = (float)(mouseX - sx) / SLIDER_W;
        return min + Math.max(0f, Math.min(1f, t)) * (max - min);
    }

    private void dragSlider(int mouseX, float curVal, float min, float max, java.util.function.Consumer<Float> setter) {
        setter.accept(sliderValue(mouseX, min, max));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void resetToDefault() {
        cfg.scale           = defaultCfg.scale;
        cfg.opacity         = defaultCfg.opacity;
        cfg.showBackground  = defaultCfg.showBackground;
        cfg.showBorder      = defaultCfg.showBorder;
        cfg.cornerRadius    = defaultCfg.cornerRadius;
        cfg.backgroundColor = defaultCfg.backgroundColor;
        cfg.borderColor     = defaultCfg.borderColor;
        cfg.textColor       = defaultCfg.textColor;
        cfg.textSize        = defaultCfg.textSize;
    }

    public boolean isOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + W && mouseY >= y && mouseY <= y + totalHeight();
    }

    private int totalHeight() {
        return PAD + 10 + 2 + ROW_COUNT * ROW_H + PAD;
    }

    public int getX() { return x; }
    public int getY() { return y; }
}
