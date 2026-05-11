package com.hudmod.gui;

import com.hudmod.config.ElementConfig;
import com.hudmod.config.HudConfig;
import com.hudmod.util.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD Editor Screen.
 * Uses direct GLFW polling for all input — no Screen mouse/keyboard
 * overrides, no Fabric ScreenMouseEvents. Works on any MC/Fabric version.
 */
public class HudEditorScreen extends Screen {

    private static final int BG_OVERLAY  = 0xA0000000;
    private static final int PANEL_BG    = 0xE5202020;
    private static final int BTN_DEFAULT = 0xFF2C2C2C;
    private static final int BTN_ACTIVE  = 0xFF27AE60;
    private static final int BTN_DANGER  = 0xFF922B21;
    private static final int TEXT_COLOR  = 0xFFEEEEEE;
    private static final int TITLE_COLOR = 0xFFFFFFFF;

    private static final int PANEL_W = 200;
    private static final int ROW_H   = 18;
    private static final int BTN_H   = 16;
    private static final int PAD     = 6;

    private final HudConfig config;
    private boolean editMode = false;

    private static final class DragTarget {
        ElementConfig cfg; String label; int boxW, boxH;
        DragTarget(ElementConfig c, String l, int w, int h) {
            cfg=c; label=l; boxW=w; boxH=h;
        }
    }

    private final List<DragTarget> dragTargets = new ArrayList<>();
    private DragTarget       dragging  = null;
    private int              dragOffX  = 0;
    private int              dragOffY  = 0;
    private ElementPopupMenu popupMenu = null;

    // Button hit-rect indices
    private static final int BTN_ARMOR    = 0;
    private static final int BTN_HELMET   = 1;
    private static final int BTN_CHEST    = 2;
    private static final int BTN_LEGS     = 3;
    private static final int BTN_BOOTS    = 4;
    private static final int BTN_HELD     = 5;
    private static final int BTN_TOTEM    = 6;
    private static final int BTN_DUR_TEXT = 7;
    private static final int BTN_DUR_BAR  = 8;
    private static final int BTN_EDIT     = 9;
    private static final int BTN_SAVE     = 10;
    private static final int BTN_RESET    = 11;
    private static final int BTN_TOTAL    = 12;

    private final int[][] btnRects = new int[BTN_TOTAL][4];

    // GLFW state from previous frame — used for edge detection
    private boolean prevLeft  = false;
    private boolean prevRight = false;
    private boolean prevEsc   = false;

    public HudEditorScreen(HudConfig config) {
        super(Text.literal("HUD Settings"));
        this.config = config;
    }

    @Override
    public boolean shouldPause() { return false; }

    // ── Main render — also handles all input via GLFW polling ─────────────────
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long win = client.getWindow().getHandle();

        // Sample current GLFW state
        boolean left  = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT)  == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean esc   = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS;

        // Rising-edge detection
        boolean leftClick   = left  && !prevLeft;
        boolean leftRelease = !left && prevLeft;
        boolean rightClick  = right && !prevRight;
        boolean escPress    = esc   && !prevEsc;

        // ── Edit mode input ───────────────────────────────────────────────────
        if (editMode) {
            if (escPress) {
                editMode  = false;
                popupMenu = null;
                dragTargets.clear();
            } else {
                // Popup drag
                if (popupMenu != null && left) popupMenu.mouseDragged(mouseX, mouseY, 0);
                if (leftRelease && popupMenu != null) popupMenu.mouseReleased();

                // Left click — start drag or close popup
                if (leftClick) {
                    if (popupMenu != null) {
                        if (!popupMenu.isOver(mouseX, mouseY)) popupMenu = null;
                        else { popupMenu.mouseClicked(mouseX, mouseY, 0); }
                    } else {
                        startDrag(mouseX, mouseY);
                    }
                }

                // Right click — open popup
                if (rightClick && popupMenu == null) {
                    openPopup(mouseX, mouseY);
                }

                // Dragging
                if (left && dragging != null) {
                    dragging.cfg.xFraction = Math.max(0f, Math.min(1f,
                        (float)(mouseX - dragOffX) / width));
                    dragging.cfg.yFraction = Math.max(0f, Math.min(1f,
                        (float)(mouseY - dragOffY) / height));
                }
                if (leftRelease) dragging = null;
            }

            if (dragTargets.isEmpty()) buildDragTargets();
            renderEditMode(ctx, mouseX, mouseY);

        // ── Settings mode input ───────────────────────────────────────────────
        } else {
            if (leftClick) handleSettingsClick(mouseX, mouseY);
            renderSettingsPanel(ctx, mouseX, mouseY);
        }

        // Store state for next frame
        prevLeft  = left;
        prevRight = right;
        prevEsc   = esc;
    }

    // ── Settings panel ────────────────────────────────────────────────────────

    private void renderSettingsPanel(DrawContext ctx, int mouseX, int mouseY) {
        ctx.fill(0, 0, width, height, BG_OVERLAY);

        int panelH = PAD + 10 + PAD + BTN_TOTAL * ROW_H + PAD + ROW_H + PAD + 14;
        int px = (width  - PANEL_W) / 2;
        int py = (height - panelH)  / 2;

        RenderUtil.drawRoundedRect(ctx, px, py, PANEL_W, panelH, 6, PANEL_BG);
        RenderUtil.drawBorder(ctx, px, py, PANEL_W, panelH, 0xFF444444);

        ctx.drawCenteredTextWithShadow(client.textRenderer,
            Text.literal("HUD Settings"), px + PANEL_W / 2, py + PAD, TITLE_COLOR);

        int ry = py + PAD + 12;
        ry = drawToggleRow(ctx, mouseX, mouseY, px, ry, "Armor HUD",       config.armorHudEnabled,       BTN_ARMOR);
        ry = drawToggleRow(ctx, mouseX, mouseY, px, ry, "  Helmet",        config.helmetEnabled,         BTN_HELMET);
        ry = drawToggleRow(ctx, mouseX, mouseY, px, ry, "  Chestplate",    config.chestplateEnabled,     BTN_CHEST);
        ry = drawToggleRow(ctx, mouseX, mouseY, px, ry, "  Leggings",      config.leggingsEnabled,       BTN_LEGS);
        ry = drawToggleRow(ctx, mouseX, mouseY, px, ry, "  Boots",         config.bootsEnabled,          BTN_BOOTS);
        ry = drawToggleRow(ctx, mouseX, mouseY, px, ry, "Held Item HUD",   config.heldItemEnabled,       BTN_HELD);
        ry = drawToggleRow(ctx, mouseX, mouseY, px, ry, "Totem Counter",   config.totemCounterEnabled,   BTN_TOTEM);
        ry = drawToggleRow(ctx, mouseX, mouseY, px, ry, "Durability Text", config.durabilityTextEnabled, BTN_DUR_TEXT);
        ry = drawToggleRow(ctx, mouseX, mouseY, px, ry, "Durability Bar",  config.durabilityBarEnabled,  BTN_DUR_BAR);

        ctx.fill(px + PAD, ry, px + PANEL_W - PAD, ry + 1, 0xFF444444);
        ry += 4;

        ry = drawActionButton(ctx, mouseX, mouseY, px, ry, "Edit Mode", BTN_EDIT,  BTN_DEFAULT);
        ry = drawActionButton(ctx, mouseX, mouseY, px, ry, "Save",      BTN_SAVE,  BTN_ACTIVE);
        ry = drawActionButton(ctx, mouseX, mouseY, px, ry, "Reset All", BTN_RESET, BTN_DANGER);

        ctx.fill(px + PAD, ry + 2, px + PANEL_W - PAD, ry + 3, 0xFF333333);
        ctx.drawCenteredTextWithShadow(client.textRenderer,
            Text.literal("\u00a77Created by \u00a7bNoTXGameR"),
            px + PANEL_W / 2, ry + 6, 0xFFFFFFFF);
    }

    private int drawToggleRow(DrawContext ctx, int mouseX, int mouseY,
                              int px, int ry, String label, boolean state, int btnIdx) {
        boolean hover = isOverBtn(btnIdx, mouseX, mouseY);
        ctx.fill(px+1, ry, px+PANEL_W-1, ry+ROW_H-1, hover ? 0x20FFFFFF : 0);
        ctx.drawText(client.textRenderer, label, px+PAD, ry+4, TEXT_COLOR, false);
        int tx = px + PANEL_W - PAD - 28;
        int ty = ry + (ROW_H - 8) / 2;
        ctx.fill(tx, ty, tx+28, ty+8, state ? 0xFF27AE60 : 0xFF555555);
        ctx.fill(state ? tx+20 : tx, ty, state ? tx+28 : tx+8, ty+8, 0xFFFFFFFF);
        btnRects[btnIdx][0]=px; btnRects[btnIdx][1]=ry;
        btnRects[btnIdx][2]=PANEL_W; btnRects[btnIdx][3]=ROW_H-1;
        return ry + ROW_H;
    }

    private int drawActionButton(DrawContext ctx, int mouseX, int mouseY,
                                 int px, int ry, String label, int btnIdx, int baseColor) {
        boolean hover = isOverBtn(btnIdx, mouseX, mouseY);
        int bx = px + PAD, bw = PANEL_W - PAD * 2;
        RenderUtil.drawRoundedRect(ctx, bx, ry, bw, BTN_H, 4,
            hover ? lighten(baseColor, 0x20) : baseColor);
        ctx.drawCenteredTextWithShadow(client.textRenderer,
            Text.literal(label), bx + bw/2, ry+3, TITLE_COLOR);
        btnRects[btnIdx][0]=bx; btnRects[btnIdx][1]=ry;
        btnRects[btnIdx][2]=bw; btnRects[btnIdx][3]=BTN_H;
        return ry + BTN_H + 3;
    }

    private boolean isOverBtn(int idx, int mx, int my) {
        int[] r = btnRects[idx];
        return mx>=r[0] && mx<=r[0]+r[2] && my>=r[1] && my<=r[1]+r[3];
    }

    private void handleSettingsClick(int x, int y) {
        if (isOverBtn(BTN_ARMOR,    x,y)) config.armorHudEnabled       = !config.armorHudEnabled;
        if (isOverBtn(BTN_HELMET,   x,y)) config.helmetEnabled         = !config.helmetEnabled;
        if (isOverBtn(BTN_CHEST,    x,y)) config.chestplateEnabled     = !config.chestplateEnabled;
        if (isOverBtn(BTN_LEGS,     x,y)) config.leggingsEnabled       = !config.leggingsEnabled;
        if (isOverBtn(BTN_BOOTS,    x,y)) config.bootsEnabled          = !config.bootsEnabled;
        if (isOverBtn(BTN_HELD,     x,y)) config.heldItemEnabled       = !config.heldItemEnabled;
        if (isOverBtn(BTN_TOTEM,    x,y)) config.totemCounterEnabled   = !config.totemCounterEnabled;
        if (isOverBtn(BTN_DUR_TEXT, x,y)) config.durabilityTextEnabled = !config.durabilityTextEnabled;
        if (isOverBtn(BTN_DUR_BAR,  x,y)) config.durabilityBarEnabled  = !config.durabilityBarEnabled;
        if (isOverBtn(BTN_EDIT,  x,y)) { editMode = true; dragTargets.clear(); }
        if (isOverBtn(BTN_SAVE,  x,y)) { config.save(); close(); }
        if (isOverBtn(BTN_RESET, x,y)) { config.resetToDefaults(); }
    }

    // ── Edit mode ─────────────────────────────────────────────────────────────

    private void renderEditMode(DrawContext ctx, int mouseX, int mouseY) {
        ctx.drawCenteredTextWithShadow(client.textRenderer,
            Text.literal("\u00a77Drag \u00b7 Right-click options \u00b7 ESC back"),
            width/2, 4, 0xFFFFFFFF);

        for (DragTarget t : dragTargets) {
            int ex = (int)(t.cfg.xFraction * width);
            int ey = (int)(t.cfg.yFraction * height);
            boolean hovered = mouseX>=ex && mouseX<=ex+t.boxW
                           && mouseY>=ey && mouseY<=ey+t.boxH;
            RenderUtil.drawBorder(ctx, ex-1, ey-1, t.boxW+2, t.boxH+2,
                hovered ? 0xFFFFFFFF : 0xFF4A90D9);
            ctx.drawText(client.textRenderer, t.label, ex, ey-9, 0xFFCCCCCC, true);
        }

        if (popupMenu != null) popupMenu.render(ctx, mouseX, mouseY);
    }

    private void buildDragTargets() {
        dragTargets.clear();
        String[] labels = {"Helmet","Chestplate","Leggings","Boots"};
        ElementConfig[] cfgs = config.armorConfigs();
        for (int i = 0; i < 4; i++) {
            int bw = (int)((16+8+(config.durabilityTextEnabled?36:0))*cfgs[i].scale);
            int bh = (int)((16+8+(config.durabilityBarEnabled?3:0))*cfgs[i].scale);
            dragTargets.add(new DragTarget(cfgs[i], labels[i], bw, bh));
        }
        dragTargets.add(new DragTarget(config.heldItem,     "Held Item",     80, 40));
        dragTargets.add(new DragTarget(config.totemCounter, "Totem Counter", 60, 28));
    }

    private void startDrag(int x, int y) {
        for (DragTarget t : dragTargets) {
            int ex = (int)(t.cfg.xFraction * width);
            int ey = (int)(t.cfg.yFraction * height);
            if (x>=ex && x<=ex+t.boxW && y>=ey && y<=ey+t.boxH) {
                dragging = t;
                dragOffX = x - ex;
                dragOffY = y - ey;
                return;
            }
        }
    }

    private void openPopup(int x, int y) {
        for (DragTarget t : dragTargets) {
            int ex = (int)(t.cfg.xFraction * width);
            int ey = (int)(t.cfg.yFraction * height);
            if (x>=ex && x<=ex+t.boxW && y>=ey && y<=ey+t.boxH) {
                popupMenu = new ElementPopupMenu(x, y, t.label, t.cfg);
                popupMenu.clampToScreen(width, height);
                return;
            }
        }
    }

    @Override
    public void close() {
        config.save();
        super.close();
    }

    private static int lighten(int argb, int amount) {
        int r = Math.min(255, ((argb>>16)&0xFF)+amount);
        int g = Math.min(255, ((argb>> 8)&0xFF)+amount);
        int b = Math.min(255, ( argb     &0xFF)+amount);
        return (argb&0xFF000000)|(r<<16)|(g<<8)|b;
    }
    }
