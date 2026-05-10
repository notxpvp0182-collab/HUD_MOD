package com.hudmod.hud;

import com.hudmod.config.HudConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Central HUD renderer.
 * Registered once with Fabric's HudRenderCallback.
 * Routes rendering to each sub-HUD only when the sub-HUD is enabled,
 * so disabled elements cost exactly zero draw calls.
 */
public class HudRenderer {

    private final HudConfig        config;
    private final ArmorHud         armorHud;
    private final HeldItemHud      heldItemHud;
    private final TotemCounterHud  totemHud;

    public HudRenderer(HudConfig config,
                       ArmorHud armorHud,
                       HeldItemHud heldItemHud,
                       TotemCounterHud totemHud) {
        this.config      = config;
        this.armorHud    = armorHud;
        this.heldItemHud = heldItemHud;
        this.totemHud    = totemHud;
    }

    /**
     * Called every frame by Fabric's HudRenderCallback.
     * delta is the partial tick — not used here, keeping render stateless.
     */
    public void render(DrawContext ctx, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        // Skip during F3 debug screen — avoids double-cluttering the screen
        if (mc.getDebugHud().shouldShowDebugHud()) return;

        if (config.armorHudEnabled)     armorHud.render(ctx, mc);
        if (config.heldItemEnabled)     heldItemHud.render(ctx, mc);
        if (config.totemCounterEnabled) totemHud.render(ctx, mc);
    }

    // Accessors used by the GUI editor
    public ArmorHud        getArmorHud()    { return armorHud;    }
    public HeldItemHud     getHeldItemHud() { return heldItemHud; }
    public TotemCounterHud getTotemHud()    { return totemHud;    }
}
