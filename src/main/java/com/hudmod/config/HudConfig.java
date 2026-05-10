package com.hudmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;

/**
 * Root config object.  Serialised as hudmod.json in the standard config directory.
 * Uses Gson, which is already bundled with Minecraft — no extra dependencies.
 *
 * Performance notes:
 *  • Gson.fromJson / toJson are called only on load/save, never per-frame.
 *  • All fields are plain Java primitives / POJOs — zero allocations at runtime.
 */
public class HudConfig {

    private static final Logger    LOGGER      = LoggerFactory.getLogger("hudmod");
    private static final Gson      GSON        = new GsonBuilder().setPrettyPrinting().create();
    private static final Path      CONFIG_PATH = FabricLoader.getInstance()
                                                    .getConfigDir().resolve("hudmod.json");

    // ── Per-element configs ────────────────────────────────────────────────
    // Default positions stagger down the left edge so pieces don't overlap.
    public ElementConfig helmet      = new ElementConfig(0.02f, 0.28f);
    public ElementConfig chestplate  = new ElementConfig(0.02f, 0.38f);
    public ElementConfig leggings    = new ElementConfig(0.02f, 0.48f);
    public ElementConfig boots       = new ElementConfig(0.02f, 0.58f);
    public ElementConfig heldItem    = new ElementConfig(0.40f, 0.84f);
    public ElementConfig totemCounter= new ElementConfig(0.02f, 0.70f);

    // ── Global feature toggles ─────────────────────────────────────────────
    public boolean armorHudEnabled      = true;
    public boolean helmetEnabled        = true;
    public boolean chestplateEnabled    = true;
    public boolean leggingsEnabled      = true;
    public boolean bootsEnabled         = true;
    public boolean heldItemEnabled      = true;
    public boolean totemCounterEnabled  = true;
    public boolean durabilityTextEnabled= true;
    public boolean durabilityBarEnabled = true;

    // ── Load / Save ────────────────────────────────────────────────────────

    public static HudConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
                HudConfig cfg = GSON.fromJson(r, HudConfig.class);
                if (cfg != null) {
                    LOGGER.info("[HudMod] Config loaded from {}", CONFIG_PATH);
                    // Null-guard: Gson may leave nested objects null if keys were absent
                    cfg.repairNulls();
                    return cfg;
                }
            } catch (Exception e) {
                LOGGER.error("[HudMod] Failed to read config, reverting to defaults.", e);
            }
        }
        HudConfig fresh = new HudConfig();
        fresh.save();
        return fresh;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, w);
            }
            LOGGER.info("[HudMod] Config saved.");
        } catch (Exception e) {
            LOGGER.error("[HudMod] Failed to save config.", e);
        }
    }

    public void resetToDefaults() {
        HudConfig d   = new HudConfig();
        this.helmet       = d.helmet;
        this.chestplate   = d.chestplate;
        this.leggings     = d.leggings;
        this.boots        = d.boots;
        this.heldItem     = d.heldItem;
        this.totemCounter = d.totemCounter;
        this.armorHudEnabled       = true;
        this.helmetEnabled         = true;
        this.chestplateEnabled     = true;
        this.leggingsEnabled       = true;
        this.bootsEnabled          = true;
        this.heldItemEnabled       = true;
        this.totemCounterEnabled   = true;
        this.durabilityTextEnabled = true;
        this.durabilityBarEnabled  = true;
    }

    /** Replace any null element configs with fresh defaults (safety after partial JSON). */
    private void repairNulls() {
        if (helmet       == null) helmet       = new ElementConfig(0.02f, 0.28f);
        if (chestplate   == null) chestplate   = new ElementConfig(0.02f, 0.38f);
        if (leggings     == null) leggings     = new ElementConfig(0.02f, 0.48f);
        if (boots        == null) boots        = new ElementConfig(0.02f, 0.58f);
        if (heldItem     == null) heldItem     = new ElementConfig(0.40f, 0.84f);
        if (totemCounter == null) totemCounter = new ElementConfig(0.02f, 0.70f);
    }

    // ── Convenience accessors used by the editor ───────────────────────────

    public ElementConfig[] armorConfigs() {
        return new ElementConfig[]{ helmet, chestplate, leggings, boots };
    }

    public boolean[] armorEnabled() {
        return new boolean[]{ helmetEnabled, chestplateEnabled, leggingsEnabled, bootsEnabled };
    }
}
