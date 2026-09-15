package me.onethecrazy.screens.editor;

import com.aksulightning.fbxplayermodels.voice.VoiceAudioInput;
import com.aksulightning.fbxplayermodels.voice.VoiceShapeSettings;
import com.aksulightning.fbxplayermodels.voice.VoiceShapePose;
import me.onethecrazy.util.render.VoiceShapeClient;
import net.minecraft.client.gui.components.AbstractWidget;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.Objects;
import me.onethecrazy.FBXPlayerModelsClient;
import me.onethecrazy.SkinManager;
import me.onethecrazy.screens.rendering.SkinPreviewRenderer;
import me.onethecrazy.util.objects.CacheSkin;
import me.onethecrazy.util.objects.Float3;
import me.onethecrazy.util.objects.Vertex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import org.joml.Vector3f;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.List;

public class VoiceShapeSettingsScreen extends Screen {
    private static final int MARGIN = 12;
    private static final int ROW_HEIGHT = 28;
    private static final int SCROLLBAR_WIDTH = 6;
    private final Screen parent;
    private final List<Control> controls = new ArrayList<>();
    private boolean livePreview = true;
    private float manualAmount;
    private long pulseUntil;
    private VoiceShapePose lastPreviewPose;
    private int rowCount;
    private CacheSkin selectedCache;
    private CacheSkin previewCache;
    private boolean modelLoaded;
    private SkinPreviewRenderer previewRenderer;
    private final Vector3f modelCenter = new Vector3f(0f, 1f, 0f);
    private float modelRadius = 1f;
    private float yaw;
    private float pitch;
    private float zoom = 1f;
    private int previewX;
    private int previewY;
    private int previewSize;
    private int controlsX;
    private int controlsWidth;
    private int listTop;
    private int listBottom;
    private int scrollOffset;
    private boolean rotating;
    private boolean draggingScrollbar;
    private int scrollbarGrabOffset;

    public VoiceShapeSettingsScreen(Screen parent, CacheSkin selectedCache) {
        super(Component.translatable("gui.fbxplayermodels.voice_shape_title"));
        this.parent = parent;
        this.selectedCache = selectedCache;
    }

    private VoiceShapeSettings settings() {
        return FBXPlayerModelsClient.options().selectedSkin.voiceShapeSettings();
    }

    @Override
    protected void init() {
        if (!modelLoaded) {
            if (selectedCache == null) selectedCache = SkinManager.loadSelectedSkinPreview();
            if (selectedCache != null && selectedCache.skinnedModel != null) {
                selectedCache = new CacheSkin(selectedCache.skinnedModel.withShapeKeyProfile(
                        FBXPlayerModelsClient.options().selectedSkin.defaultShapeKeyProfile()), selectedCache.format);
            }
            modelLoaded = true;
            fitModelBounds();
        }
        layoutPanels();
        previewRenderer = new SkinPreviewRenderer(previewX, previewY, previewSize,
                Math.max(1f, previewSize / 2f - 8f) / modelRadius);
        lastPreviewPose = null;
        controls.clear();
        rowCount = 0;
        rotating = draggingScrollbar = false;
        addRenderableOnly(this::renderContent);
        var settings = settings();
        addControl(Button.builder(label("target", choice("target", settings.target().name())), button -> {
            settings.target = VoiceShapeSettings.Target.values()[(settings.target().ordinal() + 1) % 3];
            settings.name = "";
            settings.shapeKeyId = "";
            saveSettings();
            rebuildWidgets();
        }).bounds(controlsX, 0, controlsWidth, 20).build());
        Button name = Button.builder(label("name", selectedName()), button -> openTargetSelection())
                .bounds(controlsX, 0, controlsWidth, 20).build();
        name.active = settings.target() != VoiceShapeSettings.Target.NONE && !targets().isEmpty();
        name.setTooltip(Tooltip.create(Component.literal(selectedName())));
        addControl(name);
        addControl(Button.builder(label("input", choice("input", settings.input().name())), button -> {
            settings.input = VoiceShapeSettings.Input.values()[(settings.input().ordinal() + 1) % 3];
            saveSettings();
            button.setMessage(label("input", choice("input", settings.input().name())));
        }).bounds(controlsX, 0, controlsWidth, 20).build());
        addControl(Button.builder(label("response", choice("response", settings.response().name())), button -> {
            settings.response = VoiceShapeSettings.Response.values()[(settings.response().ordinal() + 1) % 3];
            saveSettings();
            button.setMessage(label("response", choice("response", settings.response().name())));
        }).bounds(controlsX, 0, controlsWidth, 20).build());
        addControl(new SettingSlider("sensitivity", "", 1, 30, 0.5, () -> settings.sensitivity,
                value -> settings.sensitivity = (float) value, false));
        addControl(new SettingSlider("threshold", "%", 0, 95, 1, () -> settings.threshold * 100,
                value -> settings.threshold = (float) value / 100f, false));
        if (settings.target() == VoiceShapeSettings.Target.BONE) {
            addControl(new SettingSlider("start_x", "°", -180, 180, 1, () -> settings.startX, value -> settings.startX = (float) value, false));
            addControl(new SettingSlider("start_y", "°", -180, 180, 1, () -> settings.startY, value -> settings.startY = (float) value, false));
            addControl(new SettingSlider("start_z", "°", -180, 180, 1, () -> settings.startZ, value -> settings.startZ = (float) value, false));
            addControl(new SettingSlider("end_x", "°", -180, 180, 1, () -> settings.endX, value -> settings.endX = (float) value, false));
            addControl(new SettingSlider("end_y", "°", -180, 180, 1, () -> settings.endY, value -> settings.endY = (float) value, false));
            addControl(new SettingSlider("end_z", "°", -180, 180, 1, () -> settings.endZ, value -> settings.endZ = (float) value, false));
        } else if (settings.target() == VoiceShapeSettings.Target.SHAPE_KEY) {
            addControl(new SettingSlider("start_weight", "%", 0, 100, 1, () -> settings.startWeight * 100,
                    value -> settings.startWeight = (float) value / 100f, false));
            addControl(new SettingSlider("end_weight", "%", 0, 100, 1, () -> settings.endWeight * 100,
                    value -> settings.endWeight = (float) value / 100f, false));
        }
        addControl(Button.builder(label("preview", choice("preview", livePreview ? "LIVE" : "MANUAL")), button -> {
            livePreview = !livePreview;
            button.setMessage(label("preview", choice("preview", livePreview ? "LIVE" : "MANUAL")));
        }).bounds(controlsX, 0, controlsWidth, 20).build());
        addControl(new SettingSlider("preview_amount", "%", 0, 100, 1, () -> manualAmount * 100,
                value -> manualAmount = (float) value / 100f, true));
        scrollOffset = clamp(scrollOffset, 0, maxScrollOffset());
        updateControls();
        int footerY = this.height - 28;
        int buttonWidth = Math.max(1, (this.width - MARGIN * 2 - 12) / 3);
        addRenderableWidget(Button.builder(Component.translatable("gui.fbxplayermodels.shape_keys_reset_view"), button -> {
            yaw = pitch = 0f;
            zoom = 1f;
        }).bounds(MARGIN, footerY, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.fbxplayermodels.voice_shape_test"), button -> {
            pulseUntil = System.nanoTime() + 1_000_000_000L;
        }).bounds(MARGIN + buttonWidth + 6, footerY, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.fbxplayermodels.shape_keys_back"), button -> onClose())
                .bounds(MARGIN + (buttonWidth + 6) * 2, footerY, buttonWidth, 20).build());
        configureInput();
    }

    private Component label(String key, Object value) {
        return Component.translatable("gui.fbxplayermodels.voice_shape_" + key, value);
    }

    private String choice(String key, String value) {
        return Component.translatable("gui.fbxplayermodels.voice_shape_" + key + "_" + value.toLowerCase(java.util.Locale.ROOT)).getString();
    }

    private String selectedName() {
        if (settings().name == null || settings().name.isBlank()) return choice("name", "UNSELECTED");
        return settings().name;
    }

    private List<VoiceTargetSelectionScreen.Entry> targets() {
        if (selectedCache == null || selectedCache.skinnedModel == null) return List.of();
        var model = selectedCache.skinnedModel;
        return switch (settings().target()) {
            case NONE -> List.of();
            case BONE -> model.bones.stream().map(bone -> bone.name()).distinct()
                    .map(name -> new VoiceTargetSelectionScreen.Entry(name, name)).toList();
            case SHAPE_KEY -> model.shapeKeys.stream().map(key -> new VoiceTargetSelectionScreen.Entry(key.id(), key.name())).toList();
        };
    }

    private void openTargetSelection() {
        Minecraft.getInstance().gui.setScreen(new VoiceTargetSelectionScreen(this, targets(), entry -> {
            settings().name = entry.name();
            settings().shapeKeyId = settings().target() == VoiceShapeSettings.Target.SHAPE_KEY ? entry.id() : "";
            saveSettings();
        }));
    }

    private void configureInput() {
        VoiceAudioInput.configure(settings(), VoiceShapeClient.voiceChatInstalled(), true);
    }

    private void saveSettings() {
        SkinManager.saveVoiceShapeSettings();
        configureInput();
        lastPreviewPose = null;
    }

    private void addControl(AbstractWidget widget) {
        controls.add(new Control(widget, rowCount++));
        addRenderableWidget(widget);
    }

    private void layoutPanels() {
        int bodyTop = 62;
        int bodyBottom = this.height - 36;
        if (this.width >= 480) {
            previewSize = Math.max(1, Math.min(320, Math.min((this.width - MARGIN * 3) * 2 / 5, bodyBottom - bodyTop - 34)));
            previewX = MARGIN;
            previewY = bodyTop + Math.max(0, (bodyBottom - bodyTop - previewSize - 34) / 2);
            controlsX = previewX + previewSize + MARGIN;
            controlsWidth = Math.max(1, this.width - MARGIN - controlsX - 10);
            listTop = bodyTop + 22;
        } else {
            previewSize = Math.max(1, Math.min(this.width - MARGIN * 2, Math.min(128, bodyBottom - bodyTop - 48 - Math.min(84, Math.max(28, (bodyBottom - bodyTop) / 3)))));
            previewX = (this.width - previewSize) / 2;
            previewY = bodyTop;
            controlsX = MARGIN;
            controlsWidth = Math.max(1, this.width - MARGIN * 2 - 10);
            listTop = previewY + previewSize + 48;
        }
        listBottom = Math.max(listTop + 1, bodyBottom);
    }

    private void fitModelBounds() {
        if (selectedCache == null) return;
        List<Vertex> vertices = selectedCache.skinnedModel == null ? selectedCache.vertices
                : selectedCache.skinnedModel.vertices.stream().map(vertex -> vertex.vertex).toList();
        if (vertices == null || vertices.isEmpty()) return;
        Vector3f min = new Vector3f(Float.POSITIVE_INFINITY);
        Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY);
        for (Vertex vertex : vertices) {
            Float3 p = vertex.position;
            if (!Float.isFinite(p.x) || !Float.isFinite(p.y) || !Float.isFinite(p.z)) continue;
            min.min(new Vector3f(p.x, p.y, p.z));
            max.max(new Vector3f(p.x, p.y, p.z));
        }
        float radius = min.distance(max) / 2f;
        if (Float.isFinite(radius) && radius > 0f) {
            modelCenter.set(min).add(max).mul(0.5f);
            modelRadius = Math.max(0.001f, radius);
        }
    }

    private void updatePreview(VoiceShapePose pose) {
        if (Objects.equals(pose, lastPreviewPose)) return;
        lastPreviewPose = pose;
        if (selectedCache == null) { previewCache = null; return; }
        List<Vertex> vertices = selectedCache.skinnedModel == null ? selectedCache.vertices
                : selectedCache.skinnedModel.voicePreview(pose);
        List<Vertex> centered = new ArrayList<>();
        if (vertices != null) {
            for (Vertex vertex : vertices) {
                Float3 p = vertex.position;
                centered.add(new Vertex(new Float3(p.x - modelCenter.x, p.y - modelCenter.y + 1f, p.z - modelCenter.z),
                        vertex.normals, vertex.textureUV, vertex.texture, vertex.color));
            }
        }
        previewCache = new CacheSkin(centered, selectedCache.format);
    }

    private void renderContent(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.text(font, title, (this.width - font.width(title)) / 2, 10, 0xFFFFFFFF, true);
        String modelName = FBXPlayerModelsClient.options().selectedSkin.name;
        context.text(font, font.plainSubstrByWidth(modelName == null ? "" : modelName, this.width - MARGIN * 2),
                MARGIN, 28, 0xFFCCCCCC, true);
        String status = choice("status", VoiceAudioInput.status().name());
        if (settings().target() != VoiceShapeSettings.Target.NONE) {
            List<VoiceTargetSelectionScreen.Entry> available = targets();
            if (available.isEmpty()) status = choice("status", "NO_TARGETS");
            else if (settings().enabled() && available.stream().noneMatch(entry ->
                    settings().target() == VoiceShapeSettings.Target.SHAPE_KEY
                            ? Objects.equals(entry.id(), settings().shapeKeyId)
                            : Objects.equals(entry.name(), settings().name))) status = choice("status", "TARGET_MISSING");
        }
        if (!VoiceShapeClient.voiceChatInstalled()) status += " · " + choice("status", "RECOMMEND");
        context.text(font, font.plainSubstrByWidth(status, this.width - MARGIN * 2), MARGIN, 43, 0xFFAAAAAA, false);
        float amount = livePreview ? VoiceAudioInput.level() : manualAmount;
        long now = System.nanoTime();
        if (pulseUntil > now) amount = Math.min(1f, (pulseUntil - now) / 600_000_000f);
        updatePreview(VoiceShapePose.from(settings(), amount));
        context.fill(previewX, previewY, previewX + previewSize, previewY + previewSize, 0x66000000);
        previewRenderer.setView(yaw, pitch, zoom);
        previewRenderer.renderModelPreview(context, previewCache);
        String hint = Component.translatable("gui.fbxplayermodels.shape_keys_preview_hint").getString();
        context.text(font, font.plainSubstrByWidth(hint, this.width >= 480 ? previewSize : this.width - MARGIN * 2),
                this.width >= 480 ? previewX : MARGIN, previewY + previewSize + 6, 0xFFAAAAAA, false);
        int meterY = previewY + previewSize + 20;
        context.fill(previewX, meterY, previewX + previewSize, meterY + 4, 0xFF333333);
        context.fill(previewX, meterY, previewX + Math.round(previewSize * amount), meterY + 4, 0xFF55CC88);
        context.text(font, font.plainSubstrByWidth(Component.translatable("gui.fbxplayermodels.voice_shape_settings").getString(), controlsWidth),
                controlsX, listTop - 18, 0xFFFFFFFF, true);
        renderScrollbar(context);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (insidePreview(mouseX, mouseY)) {
            zoom = Math.max(0.25f, Math.min(3f, zoom * (float) Math.pow(1.1, verticalAmount)));
            return true;
        }
        if (insideControls(mouseX, mouseY) && maxScrollOffset() > 0) {
            scrollOffset = clamp(scrollOffset - (int) Math.round(verticalAmount * 28), 0, maxScrollOffset());
            updateControls();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && insidePreview(event.x(), event.y())) {
            rotating = true;
            return true;
        }
        if (event.button() == 0 && maxScrollOffset() > 0 && event.x() >= scrollbarX() && event.x() < scrollbarX() + SCROLLBAR_WIDTH
                && event.y() >= listTop && event.y() < listBottom) {
            draggingScrollbar = true;
            scrollbarGrabOffset = event.y() >= thumbTop() && event.y() < thumbTop() + thumbHeight()
                    ? (int) event.y() - thumbTop() : thumbHeight() / 2;
            updateScrollFromScrollbar((int) event.y() - scrollbarGrabOffset);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && (rotating || draggingScrollbar)) {
            rotating = false;
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (event.button() == 0 && rotating) {
            yaw = (yaw + (float) deltaX * 0.6f) % 360f;
            pitch = Math.max(-90f, Math.min(90f, pitch - (float) deltaY * 0.6f));
            return true;
        }
        if (event.button() == 0 && draggingScrollbar) {
            updateScrollFromScrollbar((int) event.y() - scrollbarGrabOffset);
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    private boolean insidePreview(double x, double y) {
        return x >= previewX && x < previewX + previewSize && y >= previewY && y < previewY + previewSize;
    }

    private boolean insideControls(double x, double y) {
        return x >= controlsX && x < scrollbarX() + SCROLLBAR_WIDTH && y >= listTop && y < listBottom;
    }

    private int maxScrollOffset() {
        return Math.max(0, rowCount * ROW_HEIGHT - (listBottom - listTop));
    }

    private void updateControls() {
        for (Control control : controls) {
            AbstractWidget widget = control.widget();
            int y = listTop + control.row() * ROW_HEIGHT - scrollOffset;
            widget.setY(y);
            widget.visible = y >= listTop && y + widget.getHeight() <= listBottom;
            if (!widget.visible && getFocused() == widget) setFocused(null);
        }
    }

    private int scrollbarX() {
        return controlsX + controlsWidth + 4;
    }

    private int thumbHeight() {
        int height = Math.max(1, listBottom - listTop);
        return Math.min(height, Math.max(20, height * height / Math.max(1, rowCount * ROW_HEIGHT)));
    }

    private int thumbTop() {
        int track = listBottom - listTop - thumbHeight();
        return listTop + (maxScrollOffset() == 0 ? 0 : scrollOffset * track / maxScrollOffset());
    }

    private void updateScrollFromScrollbar(int top) {
        int track = listBottom - listTop - thumbHeight();
        scrollOffset = track <= 0 ? 0 : clamp(top - listTop, 0, track) * maxScrollOffset() / track;
        updateControls();
    }

    private void renderScrollbar(GuiGraphicsExtractor context) {
        if (maxScrollOffset() == 0) return;
        int x = scrollbarX();
        context.fill(x, listTop, x + SCROLLBAR_WIDTH, listBottom, 0x66000000);
        context.fill(x, thumbTop(), x + SCROLLBAR_WIDTH, thumbTop() + thumbHeight(), 0xFFAAAAAA);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Control(AbstractWidget widget, int row) {}

    private final class SettingSlider extends AbstractSliderButton {
        private final String key, unit;
        private final double min, max, step;
        private final DoubleConsumer setter;
        private final boolean previewOnly;

        private SettingSlider(String key, String unit, double min, double max, double step,
                              DoubleSupplier getter, DoubleConsumer setter, boolean previewOnly) {
            super(controlsX, 0, controlsWidth, 20, Component.empty(),
                    (VoiceShapeSettings.clamp((float) getter.getAsDouble(), (float) min, (float) max) - min) / (max - min));
            this.key = key;
            this.unit = unit;
            this.min = min;
            this.max = max;
            this.step = step;
            this.setter = setter;
            this.previewOnly = previewOnly;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            double number = Math.round((min + value * (max - min)) / step) * step;
            String text = step < 1 ? String.format(java.util.Locale.ROOT, "%.1f", number) : Long.toString(Math.round(number));
            setMessage(label(key, text + unit));
        }

        @Override
        protected void applyValue() {
            double number = Math.round((min + value * (max - min)) / step) * step;
            setter.accept(number);
            if (previewOnly) {
                livePreview = false;
                for (Control control : controls) {
                    if (control.widget() instanceof Button button && control.row() == rowCount - 2)
                        button.setMessage(label("preview", choice("preview", "MANUAL")));
                }
            } else saveSettings();
            updateMessage();
        }
    }
}
