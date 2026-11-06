package com.aksulightning.fbxplayermodels.forge1122.client;

import com.aksulightning.fbxplayermodels.forge1122.ClientModels;
import com.aksulightning.fbxplayermodels.forge1122.util.model.animation.CustomModelPose;
import com.aksulightning.fbxplayermodels.forge1122.util.model.rig.LogicalBodyPart;
import com.aksulightning.fbxplayermodels.forge1122.util.model.rig.LogicalRigBinding;
import com.aksulightning.fbxplayermodels.forge1122.util.objects.SkinnedModel;
import com.aksulightning.fbxplayermodels.forge1122.util.objects.Vertex;
import com.aksulightning.fbxplayermodels.forge1122.util.render.LegacyModelRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.client.config.GuiSlider;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class ModelScreen extends GuiScreen {
    private static final String[] STATES = {"Walk", "Sneak", "Sit", "Sleep"};
    private final GuiScreen parent;
    private GuiTextField path;
    private int tab;
    private float yaw = 180f;
    private String preview = "Idle";

    public ModelScreen(GuiScreen parent) { this.parent = parent; }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        String previous = path == null ? "" : path.getText();
        buttonList.clear();
        int left = 16;
        int controlWidth = Math.min(260, width / 2 - 24);
        buttonList.add(new GuiButton(0, width / 2 - 50, height - 26, 100, 20, "Done"));
        buttonList.add(new GuiButton(1, left, 32, controlWidth / 3, 20, "Model"));
        buttonList.add(new GuiButton(2, left + controlWidth / 3, 32, controlWidth / 3, 20, "Rig"));
        buttonList.add(new GuiButton(3, left + 2 * controlWidth / 3, 32, controlWidth / 3, 20, "Camera"));
        path = null;
        ClientConfig config = ClientModels.config;
        if (tab == 0) {
            path = new GuiTextField(20, fontRenderer, left, 62, controlWidth, 18);
            path.setMaxStringLength(4096);
            path.setText(previous);
            buttonList.add(new GuiButton(4, left, 86, controlWidth / 2 - 2, 20, "Import path"));
            buttonList.add(new GuiButton(5, left + controlWidth / 2 + 2, 86, controlWidth / 2 - 2, 20, "Browse..."));
            buttonList.add(new GuiButton(6, left, 110, controlWidth / 2 - 2, 20, "Reset model"));
            buttonList.add(new GuiButton(7, left + controlWidth / 2 + 2, 110, controlWidth / 2 - 2, 20,
                    config.enabled ? "Enabled" : "Disabled"));
            buttonList.add(new GuiSlider(8, left, 134, controlWidth, 20, "Scale: ", "", 0.1, 4,
                    config.scale, true, true, slider -> {
                        config.scale = (float) slider.getValue();
                    }));
            buttonList.add(new GuiButton(9, left, 158, controlWidth, 20,
                    "Animations: " + (config.animations ? "On" : "Off")));
        } else if (tab == 1) {
            if (ClientModels.importedModel() != null) {
                // The six bindings and four clip mappings fit even at Minecraft's minimum GUI height.
                for (int i = 0; i < LogicalBodyPart.values().length; i++) {
                    LogicalBodyPart part = LogicalBodyPart.values()[i];
                    buttonList.add(new GuiButton(30 + i, left, 58 + i * 22, controlWidth, 20,
                            part.displayName + ": " + shortLabel(config.binding.firstName(part), 18)));
                }
                int right = width / 2 + 16;
                for (int i = 0; i < STATES.length; i++) {
                    buttonList.add(new GuiButton(40 + i, right, 58 + i * 22, width - right - 16, 20,
                            STATES[i] + ": " + shortLabel(config.clips.get(STATES[i]), 20)));
                }
                buttonList.add(new GuiButton(44, right, 146, width - right - 16, 20, "Auto bind"));
            }
        } else {
            buttonList.add(new GuiButton(10, left, 62, controlWidth, 20,
                    "First-person body: " + (config.firstPerson ? "On" : "Off")));
            for (int axis = 0; axis < 3; axis++) {
                final int component = axis;
                float value = axis == 0 ? config.cameraX : axis == 1 ? config.cameraY : config.cameraZ;
                buttonList.add(new GuiSlider(11 + axis, left, 90 + axis * 24, controlWidth, 20,
                        "Camera " + "XYZ".charAt(axis) + ": ", "", -2, 2, value, true, true, slider -> {
                            float offset = (float) slider.getValue();
                            if (component == 0) config.cameraX = offset;
                            else if (component == 1) config.cameraY = offset;
                            else config.cameraZ = offset;
                        }));
            }
            buttonList.add(new GuiButton(14, left, 164, controlWidth, 20, "Reset camera"));
        }
    }

    private static String shortLabel(String value, int limit) {
        if (value == null || value.isEmpty()) return "Default";
        return value.length() > limit ? value.substring(0, limit - 1) + "..." : value;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button instanceof GuiSlider) return;
        ClientConfig config = ClientModels.config;
        if (button.id == 0) { mc.displayGuiScreen(parent); return; }
        if (button.id >= 1 && button.id <= 3) { tab = button.id - 1; initGui(); return; }
        switch (button.id) {
            case 4:
                try { ClientModels.select(Paths.get(path.getText().trim())); }
                catch (RuntimeException exception) { ClientModels.status = "Invalid model path: " + exception.getMessage(); }
                break;
            case 5: ClientModels.chooseFile(); break;
            case 6: ClientModels.reset(); break;
            case 7: config.enabled = !config.enabled; break;
            case 9: config.animations = !config.animations; break;
            case 10: config.firstPerson = !config.firstPerson; break;
            case 14: config.cameraX = config.cameraY = config.cameraZ = 0f; break;
            case 44:
                config.binding = LogicalRigBinding.autoBind(ClientModels.importedModel().bones.stream()
                        .map(SkinnedModel.Bone::name).collect(Collectors.toList()));
                break;
            default:
                if (button.id >= 30 && button.id < 36) {
                    LogicalBodyPart part = LogicalBodyPart.values()[button.id - 30];
                    List<String> bones = new ArrayList<>();
                    bones.add("");
                    ClientModels.importedModel().bones.forEach(bone -> bones.add(bone.name()));
                    int index = bones.indexOf(config.binding.firstName(part));
                    config.binding.setSingle(part, bones.get((index + 1) % bones.size()));
                } else if (button.id >= 40 && button.id < 44) {
                    String state = STATES[button.id - 40];
                    List<String> clips = new ArrayList<>();
                    clips.add("");
                    clips.addAll(ClientModels.importedModel().animations.keySet());
                    int index = clips.indexOf(config.clips.getOrDefault(state, ""));
                    String next = clips.get((index + 1) % clips.size());
                    if (next.isEmpty()) config.clips.remove(state);
                    else config.clips.put(state, next);
                    preview = state;
                }
        }
        ClientModels.saveSettings();
        initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, "FBX Player Models - Forge 1.12.2 Client Only", width / 2, 12, 0xFFFFFF);
        if (tab != 1) {
            drawPreview(width * 3 / 4, height - 64, Math.min(64f, (height - 104) / 2.2f));
            drawCenteredString(fontRenderer, fontRenderer.trimStringToWidth(ClientModels.config.modelName,
                    width / 2 - 32), width * 3 / 4, 56, 0xDDDDDD);
        } else if (ClientModels.importedModel() == null) {
            drawCenteredString(fontRenderer, "Select a rigged FBX model to edit its bindings.", width / 2, 88, 0xDDDDDD);
        }
        if (tab == 2 && (ClientModels.model() == null || !ClientModels.model().canHideHead())) {
            fontRenderer.drawSplitString("First-person body requires a rig with a bound head.", width / 2 + 16,
                    82, width / 2 - 32, 0xDDDDDD);
        }
        if (path != null) path.drawTextBox();
        drawCenteredString(fontRenderer, fontRenderer.trimStringToWidth(ClientModels.status, width - 24),
                width / 2, height - 42, 0xCCCCCC);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawPreview(int x, int bottom, float scale) {
        if (!ClientModels.hasModel()) return;
        List<Vertex> vertices = ClientModels.model() == null ? ClientModels.staticVertices()
                : ClientModels.model().renderWithForcedHeadLook(preview, (System.nanoTime() % 60000000000L) / 1e9f,
                        CustomModelPose.HeadLookRotation.NONE, CustomModelPose.LimbPose.NONE);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, bottom, 150f);
            float size = scale;
            GlStateManager.scale(size, -size, size);
            GlStateManager.rotate(yaw, 0f, 1f, 0f);
            GlStateManager.color(1f, 1f, 1f, 1f);
            LegacyModelRenderer.render(vertices);
        } finally {
            GlStateManager.popMatrix();
        }
    }

    @Override protected void mouseClicked(int x, int y, int button) throws IOException {
        if (path != null) path.mouseClicked(x, y, button);
        super.mouseClicked(x, y, button);
    }

    @Override protected void mouseClickMove(int x, int y, int button, long elapsed) {
        if (x > width / 2 && tab != 1) yaw = (x - width * 3 / 4) * 2f + 180f;
        super.mouseClickMove(x, y, button, elapsed);
    }

    @Override protected void keyTyped(char character, int key) throws IOException {
        if (path != null && path.textboxKeyTyped(character, key)) return;
        if (key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(parent);
        else super.keyTyped(character, key);
    }

    @Override public void updateScreen() { if (path != null) path.updateCursorCounter(); }
    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); ClientModels.config.save(); }
}
