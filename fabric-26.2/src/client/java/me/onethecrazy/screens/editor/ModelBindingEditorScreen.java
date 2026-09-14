package me.onethecrazy.screens.editor;

import me.onethecrazy.FBXPlayerModelsClient;
import me.onethecrazy.SkinManager;
import me.onethecrazy.util.FileUtil;
import me.onethecrazy.util.model.rig.LogicalBodyPart;
import me.onethecrazy.util.model.rig.LogicalRigBinding;
import me.onethecrazy.util.objects.CacheSkin;
import me.onethecrazy.util.objects.SkinnedModel;
import me.onethecrazy.util.parsing.FBXParser;
import me.onethecrazy.util.parsing.ParsingFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public class ModelBindingEditorScreen extends Screen {
    private static final int MARGIN = 12;
    private static final int ROW_HEIGHT = 22;
    private static final int SECTION_SPACING = 10;
    private static final int LABEL_WIDTH = 150;
    private static final int CONTROL_WIDTH = 160;
    private static final int COLUMN_GAP = 12;
    private static final int CONTENT_WIDTH = LABEL_WIDTH + COLUMN_GAP + CONTROL_WIDTH;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLL_STEP = 22;
    private static final double CAMERA_OFFSET_MIN = -1.5;
    private static final double CAMERA_OFFSET_MAX = 1.5;
    private final Screen parent;
    private List<String> boneNames = List.of();
    private List<String> clipNames = List.of();
    private CacheSkin selectedCache;
    private final List<ScrollableControl> scrollableControls = new ArrayList<>();
    private int scrollOffset;
    private boolean draggingScrollbar;
    private int scrollbarGrabOffset;

    public ModelBindingEditorScreen(Screen parent) {
        super(Component.nullToEmpty("Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        selectedCache = null;
        selectedCache = currentCache();
        if (selectedCache == null) selectedCache = SkinManager.loadSelectedSkinPreview();
        boneNames = currentBones();
        clipNames = currentClips();
        scrollableControls.clear();
        clampScrollOffset();
        int labelX = contentX();
        int controlX = controlX();
        int y = 56;

        for (LogicalBodyPart part : LogicalBodyPart.values()) {
            LogicalBodyPart target = part;
            addScrollableWidget(Button.builder(bindingValueText(target), button -> {
                cycleBinding(target);
                button.setMessage(bindingValueText(target));
            }).bounds(controlX, y, CONTROL_WIDTH, 20).build(), y);
            y += ROW_HEIGHT;
        }

        y += SECTION_SPACING;
        addScrollableWidget(Button.builder(Component.nullToEmpty("Auto Bind"), button -> {
            FBXPlayerModelsClient.options().selectedSkin.logicalRigBinding = LogicalRigBinding.autoBind(boneNames);
            SkinManager.saveCurrentBinding();
            Minecraft.getInstance().gui.setScreen(new ModelBindingEditorScreen(parent));
        }).bounds(labelX, y, LABEL_WIDTH, 20).build(), y);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(controlX, this.height - 32, CONTROL_WIDTH, 20).build());

        y += ROW_HEIGHT + SECTION_SPACING * 2;
        addScrollableWidget(Button.builder(animationToggleText(), button -> {
            FBXPlayerModelsClient.options().selectedSkin.setAnimationsEnabled(!FBXPlayerModelsClient.options().selectedSkin.animationsEnabled());
            SkinManager.saveCurrentBinding();
            button.setMessage(animationToggleText());
        }).bounds(controlX, y, CONTROL_WIDTH, 20).build(), y);

        int clipY = y + ROW_HEIGHT;
        for (String state : List.of("Walk", "Sneak", "Sit", "Sleep")) {
            String logicalState = state;
            addScrollableWidget(Button.builder(clipValueText(logicalState), button -> {
                cycleClip(logicalState);
                button.setMessage(clipValueText(logicalState));
            }).bounds(controlX, clipY, CONTROL_WIDTH, 20).build(), clipY);
            clipY += ROW_HEIGHT;
        }

        int firstPersonY = clipY + SECTION_SPACING * 2;
        addScrollableWidget(Button.builder(firstPersonSelfModelText(), button -> {
            FBXPlayerModelsClient.options().renderSelfModelInFirstPerson = !FBXPlayerModelsClient.options().renderSelfModelInFirstPerson;
            FileUtil.writeSave(FBXPlayerModelsClient.options());
            button.setMessage(firstPersonSelfModelText());
        }).bounds(controlX, firstPersonY, CONTROL_WIDTH, 20).build(), firstPersonY);

        addScrollableWidget(new CameraOffsetSlider(controlX, firstPersonY + ROW_HEIGHT, CONTROL_WIDTH, 20, "X",
                () -> FBXPlayerModelsClient.options().firstPersonCameraOffsetX,
                value -> FBXPlayerModelsClient.options().firstPersonCameraOffsetX = (float) value), firstPersonY + ROW_HEIGHT);
        addScrollableWidget(new CameraOffsetSlider(controlX, firstPersonY + ROW_HEIGHT * 2, CONTROL_WIDTH, 20, "Y",
                () -> FBXPlayerModelsClient.options().firstPersonCameraOffsetY,
                value -> FBXPlayerModelsClient.options().firstPersonCameraOffsetY = (float) value), firstPersonY + ROW_HEIGHT * 2);
        addScrollableWidget(new CameraOffsetSlider(controlX, firstPersonY + ROW_HEIGHT * 3, CONTROL_WIDTH, 20, "Z",
                () -> FBXPlayerModelsClient.options().firstPersonCameraOffsetZ,
                value -> FBXPlayerModelsClient.options().firstPersonCameraOffsetZ = (float) value), firstPersonY + ROW_HEIGHT * 3);

        addScrollableWidget(Button.builder(Component.translatable("gui.fbxplayermodels.shape_keys_open"), button ->
                Minecraft.getInstance().gui.setScreen(new ShapeKeySettingsScreen(this, currentCache()))
        ).bounds(controlX, shapeKeysButtonY(), CONTROL_WIDTH, 20).build(), shapeKeysButtonY());
        addScrollableWidget(Button.builder(Component.translatable("gui.fbxplayermodels.voice_shape_open"), button ->
                Minecraft.getInstance().gui.setScreen(new VoiceShapeSettingsScreen(this, currentCache()))
        ).bounds(controlX, shapeKeysButtonY() + ROW_HEIGHT, CONTROL_WIDTH, 20).build(), shapeKeysButtonY() + ROW_HEIGHT);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.text(font, title, this.width / 2 - font.width(title) / 2, MARGIN, 0xFFFFFFFF, true);
        context.text(font, Component.nullToEmpty("Detected bones: " + boneNames.size()), contentX(), 24, 0xFFCCCCCC, true);

        renderSettingsLabels(context);
        renderScrollbar(context);

        boolean showSidePanel = this.width >= controlX() + CONTROL_WIDTH + 260;
        int x = controlX() + CONTROL_WIDTH + 28;
        int y = 56;
        if (showSidePanel) {
            context.text(font, Component.nullToEmpty("Bones / objects"), x, y - 16, 0xFFFFFFFF, true);
            if (boneNames.isEmpty()) {
                context.text(font, Component.nullToEmpty("No bindable bones detected."), x, y, 0xFFFFAAAA, true);
            } else {
                for (int i = 0; i < Math.min(18, boneNames.size()); i++) {
                    context.text(font, Component.nullToEmpty(boneNames.get(i)), x, y + i * 10, 0xFFCCCCCC, false);
                }
            }

            int clipY = y + Math.min(18, Math.max(1, boneNames.size())) * 10 + 24;
            context.text(font, Component.nullToEmpty("Animation clips: " + clipNames.size()), x, clipY, 0xFFFFFFFF, true);
            for (int i = 0; i < Math.min(8, clipNames.size()); i++) {
                context.text(font, Component.nullToEmpty(clipNames.get(i)), x, clipY + 12 + i * 10, 0xFFCCCCCC, false);
            }

            int pivotY = Math.max(220, clipY + 112);
            context.text(font, Component.nullToEmpty("Logical bind pivots"), x, pivotY, 0xFFFFFFFF, true);
            int pivotRow = 0;
            for (LogicalBodyPart part : LogicalBodyPart.values()) {
                context.text(font, Component.nullToEmpty(part.displayName + ": " + pivotText(part)), x, pivotY + 12 + pivotRow * 10, 0xFFCCCCCC, false);
                pivotRow++;
            }
        }

        List<String> warnings = FBXPlayerModelsClient.options().selectedSkin.warnings();
        if (!warnings.isEmpty()) {
            int warningY = this.height - 82 - Math.min(2, warnings.size()) * 10;
            for (int i = 0; i < Math.min(2, warnings.size()); i++) {
                context.text(font, Component.nullToEmpty(warnings.get(i)), MARGIN, warningY + i * 10, 0xFFFFFF88, false);
            }
        }

        CacheSkin cache = currentCache();
        String status = cache == null ? "Rig: none" : cache.debugStatus();
        context.text(font, Component.nullToEmpty(status), MARGIN, this.height - 54, 0xFFCCCCCC, true);
        context.text(font, Component.nullToEmpty("Rig binding controls procedural fallback movement only."), MARGIN, this.height - 42, 0xFFAAAAAA, true);

        if (showSidePanel && cache != null && cache.format == ParsingFormat.FBX) {
            int materialY = Math.max(40, this.height - 110);
            int materialX = Math.min(this.width - 220, Math.max(330, this.width / 2));
            List<String> diagnostics = FBXParser.lastMaterialDiagnostics();
            for (int i = 0; i < Math.min(6, diagnostics.size()); i++) {
                context.text(font, Component.nullToEmpty(diagnostics.get(i)), materialX, materialY + i * 10, 0xFFCCCCCC, false);
            }
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (maxScrollOffset() <= 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        scrollOffset = clamp(scrollOffset - (int) Math.round(verticalAmount * SCROLL_STEP), 0, maxScrollOffset());
        updateScrollableWidgets();
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && isOverScrollbar(event.x(), event.y())) {
            draggingScrollbar = true;
            scrollbarGrabOffset = (int) event.y() - scrollbarThumbTop();
            updateScrollFromScrollbar((int) event.y() - scrollbarGrabOffset);
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingScrollbar && event.button() == 0) {
            draggingScrollbar = false;
            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (draggingScrollbar) {
            updateScrollFromScrollbar((int) event.y() - scrollbarGrabOffset);
            return true;
        }

        return super.mouseDragged(event, deltaX, deltaY);
    }

    private void cycleBinding(LogicalBodyPart part) {
        LogicalRigBinding binding = FBXPlayerModelsClient.options().selectedSkin.binding();
        if (boneNames.isEmpty()) {
            binding.setSingle(part, "");
            SkinManager.saveCurrentBinding();
            return;
        }

        String current = binding.firstName(part);
        int next = current.isBlank() ? 0 : boneNames.indexOf(current) + 1;
        if (next < 0 || next >= boneNames.size()) {
            binding.setSingle(part, "");
        } else {
            binding.setSingle(part, boneNames.get(next));
        }
        SkinManager.saveCurrentBinding();
    }

    private Component bindingValueText(LogicalBodyPart part) {
        String value = FBXPlayerModelsClient.options().selectedSkin.binding().firstName(part);
        return Component.nullToEmpty(value.isBlank() ? "Unbound" : value);
    }

    private Component animationToggleText() {
        return Component.nullToEmpty("Animations: " + (FBXPlayerModelsClient.options().selectedSkin.animationsEnabled() ? "ON" : "OFF"));
    }

    private Component firstPersonSelfModelText() {
        String translationKey = FBXPlayerModelsClient.options().renderSelfModelInFirstPerson
                ? "gui.fbxplayermodels.first_person_self_model_enabled"
                : "gui.fbxplayermodels.first_person_self_model_disabled";
        return Component.translatable(translationKey);
    }

    private void cycleClip(String state) {
        if (clipNames.isEmpty()) {
            FBXPlayerModelsClient.options().selectedSkin.clipMappings().remove(state);
            SkinManager.saveCurrentBinding();
            return;
        }

        String current = FBXPlayerModelsClient.options().selectedSkin.clipMappings().getOrDefault(state, "");
        int next = current.isBlank() ? 0 : clipNames.indexOf(current) + 1;
        if (next < 0 || next >= clipNames.size()) {
            FBXPlayerModelsClient.options().selectedSkin.clipMappings().remove(state);
        } else {
            FBXPlayerModelsClient.options().selectedSkin.clipMappings().put(state, clipNames.get(next));
        }
        SkinManager.saveCurrentBinding();
    }

    private Component clipValueText(String state) {
        String value = FBXPlayerModelsClient.options().selectedSkin.clipMappings().getOrDefault(state, "");
        return Component.nullToEmpty(value.isBlank() ? "Procedural/default" : value);
    }

    private void renderSettingsLabels(GuiGraphicsExtractor context) {
        int labelX = contentX();
        int y = scrolledY(42);
        drawScrollableText(context, "Model Binding", this.width / 2 - font.width("Model Binding") / 2, y, 0xFFFFFFFF, true);
        y = scrolledY(56);

        for (LogicalBodyPart part : LogicalBodyPart.values()) {
            drawScrollableText(context, part.displayName, labelX, y + 6, 0xFFFFFFFF, true);
            y += ROW_HEIGHT;
        }

        y += SECTION_SPACING;
        drawScrollableText(context, "Automatically match common bone names", controlX(), y + 6, 0xFFCCCCCC, false);

        y += ROW_HEIGHT + SECTION_SPACING * 2;
        drawScrollableText(context, "Animation Clips", this.width / 2 - font.width("Animation Clips") / 2, y - 14, 0xFFFFFFFF, true);
        drawScrollableText(context, "Animations", labelX, y + 6, 0xFFFFFFFF, true);
        y += ROW_HEIGHT;

        for (String state : List.of("Walk", "Sneak", "Sit", "Sleep")) {
            drawScrollableText(context, state + " Clip", labelX, y + 6, 0xFFFFFFFF, true);
            y += ROW_HEIGHT;
        }

        y += SECTION_SPACING * 2;
        drawScrollableText(context, "First Person", this.width / 2 - font.width("First Person") / 2, y - 14, 0xFFFFFFFF, true);
        drawScrollableText(context, "Self Model", labelX, y + 6, 0xFFFFFFFF, true);
        drawScrollableText(context, "Camera X Offset", labelX, y + ROW_HEIGHT + 6, 0xFFFFFFFF, true);
        drawScrollableText(context, "Camera Y Offset", labelX, y + ROW_HEIGHT * 2 + 6, 0xFFFFFFFF, true);
        drawScrollableText(context, "Camera Z Offset", labelX, y + ROW_HEIGHT * 3 + 6, 0xFFFFFFFF, true);
        drawScrollableText(context, Component.translatable("gui.fbxplayermodels.shape_keys_menu").getString(),
                labelX, scrolledY(shapeKeysButtonY()) + 6, 0xFFFFFFFF, true);
        drawScrollableText(context, Component.translatable("gui.fbxplayermodels.voice_shape_menu").getString(),
                labelX, scrolledY(shapeKeysButtonY() + ROW_HEIGHT) + 6, 0xFFFFFFFF, true);
    }

    private int contentX() {
        return this.width / 2 - CONTENT_WIDTH / 2;
    }

    private int controlX() {
        return contentX() + LABEL_WIDTH + COLUMN_GAP;
    }

    private int scrollTop() {
        return 40;
    }

    private int scrollBottom() {
        return this.height - 64;
    }

    private int scrollHeight() {
        return Math.max(1, scrollBottom() - scrollTop());
    }

    private int shapeKeysButtonY() {
        int y = 56;
        y += LogicalBodyPart.values().length * ROW_HEIGHT;
        y += SECTION_SPACING;
        y += ROW_HEIGHT + SECTION_SPACING * 2;
        y += ROW_HEIGHT;
        y += 4 * ROW_HEIGHT;
        y += SECTION_SPACING * 2;
        y += 4 * ROW_HEIGHT;
        return y + SECTION_SPACING * 2;
    }

    private int contentBottom() {
        return shapeKeysButtonY() + ROW_HEIGHT + 20;
    }

    private int maxScrollOffset() {
        return Math.max(0, contentBottom() - scrollBottom());
    }

    private int scrolledY(int baseY) {
        return baseY - scrollOffset;
    }

    private void clampScrollOffset() {
        scrollOffset = clamp(scrollOffset, 0, maxScrollOffset());
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private <T extends AbstractWidget> T addScrollableWidget(T widget, int baseY) {
        scrollableControls.add(new ScrollableControl(widget, baseY));
        updateScrollableWidget(widget, baseY);
        return addRenderableWidget(widget);
    }

    private void updateScrollableWidgets() {
        for (ScrollableControl control : scrollableControls) {
            updateScrollableWidget(control.widget, control.baseY);
        }
    }

    private void updateScrollableWidget(AbstractWidget widget, int baseY) {
        int y = scrolledY(baseY);
        widget.setY(y);
        widget.visible = y >= scrollTop() && y + widget.getHeight() <= scrollBottom();
    }

    private void drawScrollableText(GuiGraphicsExtractor context, String text, int x, int y, int color, boolean shadow) {
        if (y >= scrollTop() && y + font.lineHeight <= scrollBottom()) {
            context.text(font, Component.nullToEmpty(text), x, y, color, shadow);
        }
    }

    private void renderScrollbar(GuiGraphicsExtractor context) {
        if (maxScrollOffset() <= 0) {
            return;
        }

        int x = this.width / 2 + CONTENT_WIDTH / 2 + 12;
        int top = scrollTop();
        int bottom = scrollBottom();
        context.fill(x, top, x + SCROLLBAR_WIDTH, bottom, 0x66000000);
        int thumbTop = scrollbarThumbTop();
        int thumbBottom = thumbTop + scrollbarThumbHeight();
        context.fill(x, thumbTop, x + SCROLLBAR_WIDTH, thumbBottom, 0xFFAAAAAA);
        context.fill(x + 1, thumbTop + 1, x + SCROLLBAR_WIDTH - 1, thumbBottom - 1, 0xFF666666);
    }

    private int scrollbarThumbHeight() {
        return Math.max(24, scrollHeight() * scrollHeight() / contentBottom());
    }

    private int scrollbarThumbTop() {
        int track = scrollHeight() - scrollbarThumbHeight();
        if (track <= 0 || maxScrollOffset() <= 0) {
            return scrollTop();
        }
        return scrollTop() + scrollOffset * track / maxScrollOffset();
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        if (maxScrollOffset() <= 0) {
            return false;
        }
        int x = this.width / 2 + CONTENT_WIDTH / 2 + 12;
        return mouseX >= x && mouseX <= x + SCROLLBAR_WIDTH
                && mouseY >= scrollTop() && mouseY <= scrollBottom();
    }

    private void updateScrollFromScrollbar(int thumbTop) {
        int track = scrollHeight() - scrollbarThumbHeight();
        if (track <= 0) {
            scrollOffset = 0;
        } else {
            int relativeTop = clamp(thumbTop - scrollTop(), 0, track);
            scrollOffset = relativeTop * maxScrollOffset() / track;
        }
        updateScrollableWidgets();
    }

    private List<String> currentBones() {
        CacheSkin cache = currentCache();
        if (cache == null || cache.skinnedModel == null) {
            return List.of();
        }

        List<String> names = new ArrayList<>();
        for (SkinnedModel.Bone bone : cache.skinnedModel.bones) {
            names.add(bone.name());
        }
        return names;
    }

    private List<String> currentClips() {
        CacheSkin cache = currentCache();
        if (cache == null || cache.skinnedModel == null) {
            return List.of();
        }
        List<String> clips = new ArrayList<>();
        for (var entry : cache.skinnedModel.animations.entrySet()) {
            if (!entry.getValue().logicalRigDriven()) {
                clips.add(entry.getKey());
            }
        }
        clips.remove("Idle");
        clips.remove("Walk");
        clips.remove("Sneak");
        return clips;
    }

    private String pivotText(LogicalBodyPart part) {
        String bound = FBXPlayerModelsClient.options().selectedSkin.binding().firstName(part);
        if (bound.isBlank()) {
            return "Unbound";
        }

        CacheSkin cache = currentCache();
        if (cache == null || cache.skinnedModel == null) {
            return "Unavailable";
        }

        String normalized = LogicalRigBinding.normalize(bound);
        for (SkinnedModel.Bone bone : cache.skinnedModel.bones) {
            if (LogicalRigBinding.normalize(bone.name()).equals(normalized)) {
                Vector3f pivot = bone.localBind().getTranslation(new Vector3f());
                return String.format(Locale.ROOT, "%.3f, %.3f, %.3f", pivot.x, pivot.y, pivot.z);
            }
        }

        return "Missing bound bone";
    }

    private CacheSkin currentCache() {
        var uuid = Minecraft.getInstance().getUser().getProfileId();
        CacheSkin cache = uuid == null ? null : SkinManager.skinCache.get(uuid.toString());
        var lookup = uuid == null ? null : SkinManager.skinLookup.get(uuid.toString());
        var selected = FBXPlayerModelsClient.options().selectedSkin;
        return cache != null && lookup != null && selected != null && Objects.equals(lookup.hash, selected.hash)
                ? cache : selectedCache;
    }

    private static final class CameraOffsetSlider extends AbstractSliderButton {
        private final String axis;
        private final DoubleSupplier getter;
        private final DoubleConsumer setter;

        private CameraOffsetSlider(int x, int y, int width, int height, String axis, DoubleSupplier getter, DoubleConsumer setter) {
            super(x, y, width, height, Component.empty(), toSliderValue(getter.getAsDouble()));
            this.axis = axis;
            this.getter = getter;
            this.setter = setter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("gui.fbxplayermodels.first_person_camera_" + axis.toLowerCase(Locale.ROOT), formattedValue()));
        }

        @Override
        protected void applyValue() {
            setter.accept(roundedOffsetValue());
            FileUtil.writeSave(FBXPlayerModelsClient.options());
            updateMessage();
        }

        private static double toSliderValue(double value) {
            return Math.max(0.0, Math.min(1.0, (value - CAMERA_OFFSET_MIN) / (CAMERA_OFFSET_MAX - CAMERA_OFFSET_MIN)));
        }

        private static double fromSliderValue(double value) {
            return CAMERA_OFFSET_MIN + (CAMERA_OFFSET_MAX - CAMERA_OFFSET_MIN) * value;
        }

        private String formattedValue() {
            return String.format(Locale.ROOT, "%.2f", getter.getAsDouble());
        }

        private double roundedOffsetValue() {
            return Math.round(fromSliderValue(value) * 100.0) / 100.0;
        }
    }

    private static final class ScrollableControl {
        private final AbstractWidget widget;
        private final int baseY;

        private ScrollableControl(AbstractWidget widget, int baseY) {
            this.widget = widget;
            this.baseY = baseY;
        }
    }
}
