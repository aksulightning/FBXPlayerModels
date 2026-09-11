package me.onethecrazy.screens;

import me.onethecrazy.FBXPlayerModelsClient;
import me.onethecrazy.SkinManager;
import me.onethecrazy.screens.editor.ModelBindingEditorScreen;
import me.onethecrazy.screens.rendering.SkinPreviewRenderer;
import me.onethecrazy.util.FileUtil;
import me.onethecrazy.util.objects.CacheSkin;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;

public class ConfigScreen extends Screen {
    // Constants
    private static final int MAX_SKIN_PREVIEW_DIMENSIONS = 300;
    private static final int MIN_SKIN_PREVIEW_DIMENSIONS = 120;
    private static final int COMPACT_MIN_SKIN_PREVIEW_DIMENSIONS = 72;
    private static final int MAX_CONTENT_WIDTH = 300;
    private static final int MARGIN = 6;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 2;
    private static final int Y_SPACING = 24;
    private static final float YAW_SENS   = 0.6f;
    private static final float PITCH_SENS = 0.6f;

    // State stuff
    private SkinPreviewRenderer skinPreviewRenderer;
    private ButtonWidget selectSkinButton;
    private ButtonWidget resetButton;
    private ButtonWidget toggleButton;
    private ButtonWidget uploadAgainButton;
    private ButtonWidget editorButton;
    private ButtonWidget doneButton;
    private final Screen parent;
    private boolean rotating = false;

    public ConfigScreen() {
        this(null);
    }

    public ConfigScreen(Screen parent) {
        super(Text.of("FBX Player Models"));
        this.parent = parent;
    }

    public static Screen create(Screen parent) {
        ConfigScreen configScreen = new ConfigScreen(parent);
        return FBXPlayerModelsClient.options().hideCommunityServerDisclaimer
                ? configScreen
                : new CommunityServerDisclaimerScreen(parent, configScreen);
    }

    @Override
    protected void init(){
        // init skinPreviewRenderer
        skinPreviewRenderer = new SkinPreviewRenderer(getCellOriginX(), getCellOriginY(), getScreenFriendlyDimensions(), getScreenFriendlyScale());

        // Init Buttons
        selectSkinButton = ButtonWidget.builder(Text.empty(),
                (button) -> SkinManager.pickClientSkin()
        ).dimensions(getContentOriginX(), getButtonsStartY(), getContentWidth(), BUTTON_HEIGHT).build();

        resetButton = ButtonWidget.builder(
                    Text.empty(),
                    (button) -> SkinManager.resetSelfSkin())
                    .dimensions(getContentOriginX(), selectSkinButton.getY() + BUTTON_HEIGHT + BUTTON_SPACING, getHalfButtonWidth(), BUTTON_HEIGHT).build();

        toggleButton = ButtonWidget.builder(Text.empty(), (button) -> {
            var options = FBXPlayerModelsClient.options();
            options.setFbxPlayerModelsEnabled(!options.areFbxPlayerModelsEnabled());
            FileUtil.writeSave(options);

            // Update Text
            updateFbxModelsButtonText();
        }).dimensions(getContentOriginX() + getHalfButtonWidth() + BUTTON_SPACING, selectSkinButton.getY() + BUTTON_HEIGHT + BUTTON_SPACING, getHalfButtonWidth(), BUTTON_HEIGHT).build();

        uploadAgainButton = ButtonWidget.builder(Text.empty(), (button) ->
                SkinManager.uploadSelectedSkin(false)
        ).dimensions(getContentOriginX(), resetButton.getY() + BUTTON_HEIGHT + BUTTON_SPACING, getHalfButtonWidth(), BUTTON_HEIGHT).build();

        editorButton = ButtonWidget.builder(Text.of("Settings"), (button) ->
                MinecraftClient.getInstance().setScreen(new ModelBindingEditorScreen(this))
        ).dimensions(getContentOriginX() + getHalfButtonWidth() + BUTTON_SPACING, resetButton.getY() + BUTTON_HEIGHT + BUTTON_SPACING, getHalfButtonWidth(), BUTTON_HEIGHT).build();

        doneButton = ButtonWidget.builder(
                Text.translatable("gui.done"),
                (button) -> close())
                .dimensions(getContentOriginX(), getDoneButtonY(), getContentWidth(), BUTTON_HEIGHT).build();

        this.addDrawable((context, mouseX, mouseY, delta) -> skinPreviewRenderer.renderPreview(context, delta));
        this.addDrawableChild(selectSkinButton);
        this.addDrawableChild(resetButton);
        this.addDrawableChild(toggleButton);
        this.addDrawableChild(uploadAgainButton);
        this.addDrawableChild(editorButton);
        this.addDrawableChild(doneButton);
        this.addDrawable(this::renderText);

        // Set Button Texts
        updateSelectButtonText();
        updateResetButtonText();
        updateUploadAgainButtonText();
        updateFbxModelsButtonText();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Update Text
        updateSelectButtonText();
        updateUploadAgainButtonText();

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderText(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render the banner text
        context.drawText(textRenderer, trimmed(FBXPlayerModelsClient.bannerText, getContentWidth()), getContentOriginX(), getCellOriginY() + getScreenFriendlyDimensions() + MARGIN, 0xFFFFFFFF, true);

        CacheSkin cacheSkin = getSelfCacheSkin();
        int statusY = editorButton.getY() + BUTTON_HEIGHT + MARGIN;
        if(cacheSkin != null && statusY + textRenderer.fontHeight < doneButton.getY())
            context.drawText(textRenderer, trimmed(cacheSkin.debugStatus(), getContentWidth()), getContentOriginX(), statusY, 0xFFFFFFFF, true);

        // Render Mod Title
        String title = "FBX Player Models";
        context.drawText(textRenderer, title, this.width / 2 - textRenderer.getWidth(title) / 2, MARGIN, 0xFFFFFFFF, true);
    }

    @Override
    public void close() {
        if (this.client != null && parent != null) {
            this.client.setScreen(parent);
            return;
        }

        super.close();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInsideCell(mouseX, mouseY)) {
            rotating = true;
            return true; // start drag mode
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (rotating && button == 0) {
            rotating = false;
            return true; // stop rotation mode
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (rotating && button == 0) {
            // convert mouse motion to yaw/pitch deltas
            float yawDelta   = (float) (deltaX * YAW_SENS);
            float pitchDelta = (float) (-deltaY * PITCH_SENS);

            // apply directly
            skinPreviewRenderer.addRotation(yawDelta, pitchDelta);
            return true; // consume drag
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    // Position Helpers
    @Unique
    private int getCellOriginY(){
        return MARGIN * 4;
    }

    @Unique private int getCellOriginX(){
        return this.width / 2 - getScreenFriendlyDimensions() / 2;
    }

    @Unique private int getScreenFriendlyDimensions(){
        int availableHeight = this.height - getCellOriginY() - getControlsHeight() - MARGIN;
        int availableWidth = this.width - 2 * MARGIN;
        int available = Math.min(MAX_SKIN_PREVIEW_DIMENSIONS, Math.min(availableHeight, availableWidth));
        int minimum = availableHeight >= MIN_SKIN_PREVIEW_DIMENSIONS
                ? MIN_SKIN_PREVIEW_DIMENSIONS
                : COMPACT_MIN_SKIN_PREVIEW_DIMENSIONS;
        return Math.max(minimum, available);
    }

    @Unique private float getScreenFriendlyScale(){
        return getScreenFriendlyDimensions() / 2f - 10;
    }

    @Unique private int getContentWidth(){
        int availableWidth = Math.max(1, this.width - 2 * MARGIN);
        return Math.min(MAX_CONTENT_WIDTH, availableWidth);
    }

    @Unique private int getContentOriginX(){
        return this.width / 2 - getContentWidth() / 2;
    }

    @Unique private int getButtonsStartY(){
        return getCellOriginY() + getScreenFriendlyDimensions() + getVerticalSpacing();
    }

    @Unique private int getHalfButtonWidth(){
        return (getContentWidth() - BUTTON_SPACING) / 2;
    }

    @Unique private int getControlsHeight(){
        int spacing = getVerticalSpacing();
        return spacing
                + BUTTON_HEIGHT
                + BUTTON_SPACING
                + BUTTON_HEIGHT
                + BUTTON_SPACING
                + BUTTON_HEIGHT
                + spacing
                + textRenderer.fontHeight
                + 2 * MARGIN
                + BUTTON_HEIGHT;
    }

    @Unique private int getVerticalSpacing() {
        return this.height < 520 ? Math.max(14, Y_SPACING - (520 - this.height) / 12) : Y_SPACING;
    }

    @Unique private int getDoneButtonY() {
        int flowY = editorButton.getY() + getVerticalSpacing() + textRenderer.fontHeight + 2 * MARGIN;
        int footerY = this.height - BUTTON_HEIGHT - MARGIN;
        return Math.min(flowY, footerY);
    }

    private boolean isInsideCell(double x, double y) {
        int x0 = getCellOriginX();
        int y0 = getCellOriginY();
        int s  = getScreenFriendlyDimensions();
        return x >= x0 && x <= x0 + s && y >= y0 && y <= y0 + s;
    }

    // Button Text helpers
    @Unique private void updateSelectButtonText(){
        Text text = Objects.equals(FBXPlayerModelsClient.options().selectedSkin.hash, "")
                ? Text.translatable("gui.fbxplayermodels.select_skin")
                : Text.of(trimmed(FBXPlayerModelsClient.options().selectedSkin.name, selectSkinButton.getWidth() - 12));

        selectSkinButton.setMessage(text);
    }

    @Unique private void updateResetButtonText(){
        resetButton.setMessage(Text.of(trimmed(Text.translatable("gui.fbxplayermodels.reset").getString(), resetButton.getWidth() - 12)));
    }

    @Unique private void updateUploadAgainButtonText(){
        uploadAgainButton.active = !Objects.equals(FBXPlayerModelsClient.options().selectedSkin.hash, "");
        uploadAgainButton.setMessage(Text.of(trimmed(Text.translatable("gui.fbxplayermodels.upload_again").getString(), uploadAgainButton.getWidth() - 12)));
    }

    @Unique private void updateFbxModelsButtonText(){
        Text text = FBXPlayerModelsClient.options().areFbxPlayerModelsEnabled()
                ? Text.translatable("gui.fbxplayermodels.fbx_models_enabled")
                : Text.translatable("gui.fbxplayermodels.fbx_models_disabled");

        toggleButton.setMessage(text);
    }

    @Unique private String trimmed(String text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }

        return textRenderer.trimToWidth(text, Math.max(0, maxWidth - textRenderer.getWidth("..."))) + "...";
    }

    @Unique private CacheSkin getSelfCacheSkin() {
        var uuid = MinecraftClient.getInstance().getSession().getUuidOrNull();
        return uuid == null ? null : SkinManager.skinCache.get(uuid.toString());
    }

}
