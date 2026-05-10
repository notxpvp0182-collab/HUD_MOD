package com.hudmod;

import com.hudmod.config.HudConfig;
import com.hudmod.gui.HudEditorScreen;
import com.hudmod.hud.ArmorHud;
import com.hudmod.hud.HeldItemHud;
import com.hudmod.hud.HudRenderer;
import com.hudmod.hud.TotemCounterHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HudMod — main client entrypoint.
 *
 * Initialisation order:
 *  1. Load HudConfig from disk (or create defaults).
 *  2. Construct all HUD subsystems.
 *  3. Register HudRenderCallback (render every frame).
 *  4. Register ClientTickEvents.END_CLIENT_TICK (totem counter + keybind check).
 *
 * Shutdown / config persistence:
 *  • Config is auto-saved when HudEditorScreen closes.
 *  • No shutdown hooks needed — saving is triggered by the user action.
 */
public class HudMod implements ClientModInitializer {

    public static final String MOD_ID = "hudmod";
    public static final Logger LOGGER  = LoggerFactory.getLogger(MOD_ID);

    // ── Singleton accessors used by other subsystems ──────────────────────────
    private static HudConfig  CONFIG;
    private static HudRenderer RENDERER;

    public static HudConfig  getConfig()   { return CONFIG;   }
    public static HudRenderer getRenderer() { return RENDERER; }

    // ── Keybind ───────────────────────────────────────────────────────────────
    private static KeyBinding openEditorKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[HudMod] Initialising…");

        // 1. Config
        CONFIG = HudConfig.load();

        // 2. Sub-HUDs
        ArmorHud        armorHud   = new ArmorHud(CONFIG);
        HeldItemHud     heldHud    = new HeldItemHud(CONFIG);
        TotemCounterHud totemHud   = new TotemCounterHud(CONFIG);

        // 3. Central renderer
        RENDERER = new HudRenderer(CONFIG, armorHud, heldHud, totemHud);

        // 4. Register HUD render callback (runs every frame, client-side)
        HudRenderCallback.EVENT.register((drawContext, tickDelta) ->
            RENDERER.render(drawContext, tickDelta));

        // 5. Keybind (default: H)
        openEditorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.hudmod.open_editor",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "category.hudmod"
        ));

        // 6. Tick event — lightweight; only scans totems every N ticks
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            // Totem counter lazy update
            if (mc.player != null) {
                totemHud.onTick(mc);
            }

            // Keybind check — wasPressed() is one int comparison
            while (openEditorKey.wasPressed()) {
                if (mc.currentScreen == null) {
                    mc.setScreen(new HudEditorScreen(CONFIG));
                }
            }
        });

        LOGGER.info("[HudMod] Ready. Press [H] (default) to open HUD Settings.");
    }
}
