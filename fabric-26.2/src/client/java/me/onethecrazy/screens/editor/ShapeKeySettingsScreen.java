package me.onethecrazy.screens.editor;

import com.aksulightning.fbxplayermodels.model.shape.ShapeKey;
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
import net.minecraft.client.input.MouseButtonEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class ShapeKeySettingsScreen extends Screen {
    private static final int MARGIN = 12;
    private static final int ROW_HEIGHT = 40;
    private static final int SCROLLBAR_WIDTH = 6;
    private final Screen parent;
    private final List<ShapeKeySlider> sliders = new ArrayList<>();
    private CacheSkin selectedCache;
    private CacheSkin previewCache;
    private boolean modelLoaded;
    private List<ShapeKey> shapeKeys = List.of();
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

    public ShapeKeySettingsScreen(Screen parent, CacheSkin selectedCache) {
        super(Component.translatable("gui.fbxplayermodels.shape_keys_title"));
        this.parent = parent;
        this.selectedCache = selectedCache;
    }

    @Override
    protected void init() {
        if (!modelLoaded) {
            if (selectedCache == null) selectedCache = SkinManager.loadSelectedSkinPreview();
            modelLoaded = true;
            fitModelBounds();
        }
        shapeKeys = selectedCache == null || selectedCache.skinnedModel == null
                ? List.of() : selectedCache.skinnedModel.shapeKeys;
        layoutPanels();
        previewRenderer = new SkinPreviewRenderer(previewX, previewY, previewSize,
                Math.max(1f, previewSize / 2f - 8f) / modelRadius);
        updatePreview();
        sliders.clear();
        rotating = false;
        draggingScrollbar = false;
        scrollOffset = clamp(scrollOffset, 0, maxScrollOffset());
        addRenderableOnly(this::renderContent);
        for (int i = 0; i < shapeKeys.size(); i++) {
            ShapeKey key = shapeKeys.get(i);
            ShapeKeySlider slider = new ShapeKeySlider(i, key.id());
            slider.setTooltip(Tooltip.create(Component.nullToEmpty(key.name())));
            sliders.add(slider);
            addRenderableWidget(slider);
        }
        updateSliders();

        int footerY = this.height - 28;
        int buttonWidth = Math.max(1, (this.width - MARGIN * 2 - 12) / 3);
        addRenderableWidget(Button.builder(Component.translatable("gui.fbxplayermodels.shape_keys_reset_view"), button -> {
            yaw = 0f;
            pitch = 0f;
            zoom = 1f;
        }).bounds(MARGIN, footerY, buttonWidth, 20).build());
        Button reset = Button.builder(Component.translatable("gui.fbxplayermodels.shape_keys_reset"), button -> {
            FBXPlayerModelsClient.options().selectedSkin.defaultShapeKeyProfile().reset();
            saveProfile();
            for (ShapeKeySlider slider : sliders) slider.refreshValue();
        }).bounds(MARGIN + buttonWidth + 6, footerY, buttonWidth, 20).build();
        reset.active = !shapeKeys.isEmpty();
        addRenderableWidget(reset);
        addRenderableWidget(Button.builder(Component.translatable("gui.fbxplayermodels.shape_keys_back"), button -> onClose())
                .bounds(MARGIN + (buttonWidth + 6) * 2, footerY, buttonWidth, 20).build());
    }

    private void layoutPanels() {
        int bodyTop = 48;
        int bodyBottom = this.height - 36;
        if (this.width >= 480) {
            previewSize = Math.max(1, Math.min(320, Math.min((this.width - MARGIN * 3) * 2 / 5, bodyBottom - bodyTop - 22)));
            previewX = MARGIN;
            previewY = bodyTop + Math.max(0, (bodyBottom - bodyTop - previewSize - 22) / 2);
            controlsX = previewX + previewSize + MARGIN;
            controlsWidth = Math.max(1, this.width - MARGIN - controlsX - 10);
            listTop = bodyTop + 22;
        } else {
            previewSize = Math.max(1, Math.min(this.width - MARGIN * 2, Math.min(128, (bodyBottom - bodyTop) / 2 - 16)));
            previewX = (this.width - previewSize) / 2;
            previewY = bodyTop;
            controlsX = MARGIN;
            controlsWidth = Math.max(1, this.width - MARGIN * 2 - 10);
            listTop = previewY + previewSize + 42;
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

    private void updatePreview() {
        if (selectedCache == null) {
            previewCache = null;
            return;
        }
        List<Vertex> vertices = selectedCache.skinnedModel == null ? selectedCache.vertices
                : selectedCache.skinnedModel.staticVertices();
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

    private void saveProfile() {
        SkinManager.saveDefaultShapeKeyProfile();
        if (selectedCache != null && selectedCache.skinnedModel != null) {
            selectedCache = new CacheSkin(selectedCache.skinnedModel.withShapeKeyProfile(
                    FBXPlayerModelsClient.options().selectedSkin.defaultShapeKeyProfile()), selectedCache.format);
        }
        updatePreview();
    }

    private void renderContent(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.text(font, title, (this.width - font.width(title)) / 2, 10, 0xFFFFFFFF, true);
        String modelName = FBXPlayerModelsClient.options().selectedSkin.name;
        context.text(font, font.plainSubstrByWidth(modelName == null ? "" : modelName, this.width - MARGIN * 2),
                MARGIN, 28, 0xFFCCCCCC, true);
        context.fill(previewX, previewY, previewX + previewSize, previewY + previewSize, 0x66000000);
        previewRenderer.setView(yaw, pitch, zoom);
        previewRenderer.renderModelPreview(context, previewCache);
        String hint = Component.translatable("gui.fbxplayermodels.shape_keys_preview_hint").getString();
        context.text(font, font.plainSubstrByWidth(hint, this.width >= 480 ? previewSize : this.width - MARGIN * 2),
                this.width >= 480 ? previewX : MARGIN, previewY + previewSize + 6, 0xFFAAAAAA, false);
        String profile = Component.translatable("gui.fbxplayermodels.shape_keys_profile_status", shapeKeys.size()).getString();
        context.text(font, font.plainSubstrByWidth(profile, controlsWidth), controlsX, listTop - 18, 0xFFFFFFFF, true);
        context.enableScissor(controlsX, listTop, controlsX + controlsWidth, listBottom);
        if (shapeKeys.isEmpty()) {
            int textY = listTop + 4;
            for (String key : selectedCache == null
                    ? List.of("gui.fbxplayermodels.shape_keys_no_model")
                    : List.of("gui.fbxplayermodels.shape_keys_empty", "gui.fbxplayermodels.shape_keys_blender_export")) {
                for (var line : font.split(Component.translatable(key), controlsWidth)) {
                    context.text(font, line, controlsX, textY, 0xFFCCCCCC, false);
                    textY += font.lineHeight + 2;
                }
                textY += 6;
            }
        } else {
            for (int i = 0; i < shapeKeys.size(); i++) {
                int y = listTop + i * ROW_HEIGHT - scrollOffset;
                if (y + font.lineHeight < listTop || y > listBottom) continue;
                context.text(font, font.plainSubstrByWidth(shapeKeys.get(i).name(), controlsWidth), controlsX, y, 0xFFFFFFFF, true);
            }
        }
        context.disableScissor();
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
            updateSliders();
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
        return Math.max(0, shapeKeys.size() * ROW_HEIGHT - (listBottom - listTop));
    }

    private void updateSliders() {
        for (ShapeKeySlider slider : sliders) {
            int y = listTop + slider.row * ROW_HEIGHT + 14 - scrollOffset;
            slider.setY(y);
            slider.visible = y >= listTop && y + slider.getHeight() <= listBottom;
            if (!slider.visible && getFocused() == slider) setFocused(null);
        }
    }

    private int scrollbarX() {
        return controlsX + controlsWidth + 4;
    }

    private int thumbHeight() {
        int height = Math.max(1, listBottom - listTop);
        return Math.min(height, Math.max(20, height * height / Math.max(1, shapeKeys.size() * ROW_HEIGHT)));
    }

    private int thumbTop() {
        int track = listBottom - listTop - thumbHeight();
        return listTop + (maxScrollOffset() == 0 ? 0 : scrollOffset * track / maxScrollOffset());
    }

    private void updateScrollFromScrollbar(int top) {
        int track = listBottom - listTop - thumbHeight();
        scrollOffset = track <= 0 ? 0 : clamp(top - listTop, 0, track) * maxScrollOffset() / track;
        updateSliders();
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

    private final class ShapeKeySlider extends AbstractSliderButton {
        private final int row;
        private final String id;

        private ShapeKeySlider(int row, String id) {
            super(controlsX, 0, controlsWidth, 20, Component.empty(),
                    FBXPlayerModelsClient.options().selectedSkin.defaultShapeKeyProfile().weight(id));
            this.row = row;
            this.id = id;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("gui.fbxplayermodels.shape_keys_weight", Math.round(value * 100)));
        }

        @Override
        protected void applyValue() {
            value = Math.round(value * 100) / 100.0;
            FBXPlayerModelsClient.options().selectedSkin.defaultShapeKeyProfile().setWeight(id, (float) value);
            saveProfile();
            updateMessage();
        }

        private void refreshValue() {
            value = FBXPlayerModelsClient.options().selectedSkin.defaultShapeKeyProfile().weight(id);
            updateMessage();
        }
    }
}
