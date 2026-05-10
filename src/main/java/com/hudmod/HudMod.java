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
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HudMod implements ClientModInitializer {

    public static final String MOD_ID = "hudmod";
    public static final Logger LOGGER  = LoggerFactory.getLogger(MOD_ID);

    private static HudConfig   CONFIG;
    private static HudRenderer RENDERER;

    public static HudConfig   getConfig()   { return CONFIG;   }
    public static HudRenderer getRenderer() { return RENDERER; }

    private static KeyBinding openEditorKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[HudMod] Initialising...");

        CONFIG = HudConfig.load();

        ArmorHud        armorHud = new ArmorHud(CONFIG);
        HeldItemHud     heldHud  = new HeldItemHud(CONFIG);
        TotemCounterHud totemHud = new TotemCounterHud(CONFIG);

        RENDERER = new HudRenderer(CONFIG, armorHud, heldHud, totemHud);

        HudRenderCallback.EVENT.register((drawContext, tickCounter) ->
            RENDERER.render(drawContext, 0f));

        // 1.21.10 — KeyBinding.Category replaces String category
        KeyBinding.Category hudCategory = KeyBinding.Category.create(
            Identifier.of("hudmod", "main")
        );

        openEditorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.hudmod.open_editor",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            hudCategory
        ));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player != null) totemHud.onTick(mc);

            while (openEditorKey.wasPressed()) {
                if (mc.currentScreen == null) {
                    mc.setScreen(new HudEditorScreen(CONFIG));
                }
            }
        });

        LOGGER.info("[HudMod] Ready. Press H to open HUD Settings.");
    }
                                                  }
