package com.hudmod.config;

/**
 * Stores per-element HUD settings.
 * Each HUD element (helmet, chestplate, held item, etc.) has its own independent instance.
 * Serialised to/from JSON via Gson (bundled with Minecraft — zero extra deps).
 */
public class ElementConfig {

    // ── Position (fractions of screen width/height, 0.0–1.0) ──────────────
    public float xFraction = 0.02f;
    public float yFraction = 0.30f;

    // ── Appearance ─────────────────────────────────────────────────────────
    public float scale          = 1.0f;
    public float opacity        = 1.0f;
    public boolean showBackground  = true;
    public boolean showBorder      = false;
    public boolean roundedCorners  = true;
    public float  cornerRadius     = 4f;

    // ── Colors (ARGB 0xAARRGGBB) ────────────────────────────────────────────
    public int backgroundColor = 0x80000000;   // 50% black
    public int borderColor     = 0xFFFFFFFF;   // white
    public int textColor       = 0xFFFFFFFF;   // white

    // ── Text ───────────────────────────────────────────────────────────────
    public float textSize = 1.0f;

    // ── Constructors ────────────────────────────────────────────────────────
    public ElementConfig() {}

    public ElementConfig(float xFraction, float yFraction) {
        this.xFraction = xFraction;
        this.yFraction = yFraction;
    }

    /** Deep copy – used by Reset-to-Default. */
    public ElementConfig copy() {
        ElementConfig c = new ElementConfig();
        c.xFraction       = this.xFraction;
        c.yFraction       = this.yFraction;
        c.scale           = this.scale;
        c.opacity         = this.opacity;
        c.showBackground  = this.showBackground;
        c.showBorder      = this.showBorder;
        c.roundedCorners  = this.roundedCorners;
        c.cornerRadius    = this.cornerRadius;
        c.backgroundColor = this.backgroundColor;
        c.borderColor     = this.borderColor;
        c.textColor       = this.textColor;
        c.textSize        = this.textSize;
        return c;
    }
}
