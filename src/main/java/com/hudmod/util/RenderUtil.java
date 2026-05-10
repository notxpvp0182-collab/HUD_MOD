package com.hudmod.util;

import com.hudmod.config.ElementConfig;
import net.minecraft.client.gui.DrawContext;

/**
 * Static rendering utilities.
 *
 * Performance notes:
 *  • Every method is static and final — direct calls, no virtual dispatch.
 *  • drawRoundedRect uses exactly 7 fill() calls — minimum possible.
 *  • No heap allocations in hot paths.
 *  • getPulse() uses only System.currentTimeMillis() — no object creation.
 */
public final class RenderUtil {

    private RenderUtil() {}

    // ── Background / border ─────────────────────────────────────────────────

    public static void drawBackground(DrawContext ctx, int x, int y, int w, int h, ElementConfig cfg) {
        int bg = applyOpacity(cfg.backgroundColor, cfg.opacity);
        if (cfg.roundedCorners && cfg.cornerRadius > 0f) {
            drawRoundedRect(ctx, x, y, w, h, (int) cfg.cornerRadius, bg);
        } else {
            ctx.fill(x, y, x + w, y + h, bg);
        }
        if (cfg.showBorder) {
            drawBorder(ctx, x, y, w, h, applyOpacity(cfg.borderColor, cfg.opacity));
        }
    }

    /**
     * Rounded rectangle drawn with 7 axis-aligned fills.
     * Corners are square-approximated for maximum performance (no sin/cos).
     */
    public static void drawRoundedRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }
        // Horizontal centre strip
        ctx.fill(x + r, y,         x + w - r, y + h,         color);
        // Left and right strips
        ctx.fill(x,         y + r, x + r,         y + h - r, color);
        ctx.fill(x + w - r, y + r, x + w,         y + h - r, color);
        // Four corner squares (inside the r×r corner boxes)
        ctx.fill(x + 1,         y + 1,         x + r,         y + r,         color); // TL
        ctx.fill(x + w - r,     y + 1,         x + w - 1,     y + r,         color); // TR
        ctx.fill(x + 1,         y + h - r,     x + r,         y + h - 1,     color); // BL
        ctx.fill(x + w - r,     y + h - r,     x + w - 1,     y + h - 1,     color); // BR
    }

    /** 1-pixel border. */
    public static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,         y,         x + w,     y + 1,     color); // top
        ctx.fill(x,         y + h - 1, x + w,     y + h,     color); // bottom
        ctx.fill(x,         y + 1,     x + 1,     y + h - 1, color); // left
        ctx.fill(x + w - 1, y + 1,     x + w,     y + h - 1, color); // right
    }

    /** Horizontal progress bar (filled left→right). */
    public static void drawProgressBar(DrawContext ctx, int x, int y, int w, int h,
                                        float progress, int bgColor, int fgColor) {
        ctx.fill(x, y, x + w, y + h, bgColor);
        int fill = (int) (w * Math.max(0f, Math.min(1f, progress)));
        if (fill > 0) ctx.fill(x, y, x + fill, y + h, fgColor);
    }

    // ── Color helpers ────────────────────────────────────────────────────────

    /**
     * Applies an additional opacity multiplier to an ARGB color.
     * Does not allocate any objects.
     */
    public static int applyOpacity(int argb, float opacity) {
        int a = (int) ((argb >>> 24) * Math.max(0f, Math.min(1f, opacity)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    /** Green → yellow → red based on durability fraction. */
    public static int getDurabilityColor(float pct) {
        if (pct > 0.6f) return 0xFF55FF55;
        if (pct > 0.3f) return 0xFFFFFF55;
        return 0xFFFF5555;
    }

    // ── Animation ────────────────────────────────────────────────────────────

    /**
     * Returns a value in [0, 1] that oscillates with the given period.
     * Uses System.currentTimeMillis() — no object creation.
     */
    public static float getPulse(long periodMs) {
        return (float) Math.abs(Math.sin((double) System.currentTimeMillis() * Math.PI / periodMs));
    }

    /** Linear interpolation between two ARGB colors. */
    public static int blendColors(int a, int b, float t) {
        int aa = (a >>> 24) & 0xFF, ra = (a >> 16) & 0xFF, ga = (a >> 8) & 0xFF, ba = a & 0xFF;
        int ab = (b >>> 24) & 0xFF, rb = (b >> 16) & 0xFF, gb = (b >> 8) & 0xFF, bb = b & 0xFF;
        return  (lerp(aa, ab, t) << 24)
              | (lerp(ra, rb, t) << 16)
              | (lerp(ga, gb, t) << 8)
              |  lerp(ba, bb, t);
    }

    private static int lerp(int a, int b, float t) {
        return (int) (a + (b - a) * t);
    }
}
