package me.onethecrazy.screens;

import com.mojang.blaze3d.platform.InputConstants;
import me.onethecrazy.FBXPlayerModelsClient;
import me.onethecrazy.util.FileUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

public class CommunityServerDisclaimerScreen extends Screen {
    private static final int MAX_MARGIN = 50;
    private static final int MIN_MARGIN = 12;
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 20;
    private static final int CHECKBOX_SIZE = 20;
    private static final int CONTROL_GAP = 12;

    private final Screen parent;
    private final Screen proceedScreen;
    private boolean hideNextTime = false;

    public CommunityServerDisclaimerScreen(Screen parent, Screen proceedScreen) {
        super(Component.translatable("gui.fbxplayermodels.title.community_server_disclaimer"));
        this.parent = parent;
        this.proceedScreen = proceedScreen;
    }

    @Override
    protected void init() {
        int margin = getMargin();
        int availableWidth = Math.max(1, this.width - margin * 2);
        int buttonY = getButtonY();
        int buttonGap = availableWidth >= BUTTON_WIDTH * 2 + BUTTON_GAP ? BUTTON_GAP : 8;
        int buttonWidth = Math.min(BUTTON_WIDTH, Math.max(80, (availableWidth - buttonGap) / 2));
        int totalButtonWidth = buttonWidth * 2 + buttonGap;
        int startX = this.width / 2 - totalButtonWidth / 2;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.fbxplayermodels.proceed"),
                        button -> proceed())
                .bounds(startX, buttonY, buttonWidth, BUTTON_HEIGHT)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.back"),
                        button -> goBack())
                .bounds(startX + buttonWidth + buttonGap, buttonY, buttonWidth, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        int margin = getMargin();
        int lineSpacing = getLineSpacing();

        String title = Component.translatable("gui.fbxplayermodels.title.community_server_disclaimer").getString();
        context.text(font, title, margin, margin, 0xFFFFFFFF, true);

        int textY = margin + lineSpacing * 3;
        int maxTextY = getCheckboxY() - lineSpacing;
        for (String line : wrap(Component.translatable("gui.fbxplayermodels.description.community_server_disclaimer").getString(), getTextWidth())) {
            if (textY > maxTextY) {
                break;
            }
            context.text(font, line, margin, textY, 0xFFFFFFFF, true);
            textY += lineSpacing;
        }

        drawCheckbox(context);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && isInsideCheckbox(event.x(), event.y())) {
            hideNextTime = !hideNextTime;
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        goBack();
    }

    private void proceed() {
        if (hideNextTime) {
            FBXPlayerModelsClient.options().hideCommunityServerDisclaimer = true;
            FileUtil.writeSave(FBXPlayerModelsClient.options());
        }

        Minecraft.getInstance().gui.setScreen(proceedScreen);
    }

    private void goBack() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

    private void drawCheckbox(GuiGraphicsExtractor context) {
        int checkboxX = getCheckboxX();
        int checkboxY = getCheckboxY();
        int textX = checkboxX + CHECKBOX_SIZE + 8;
        String mark = hideNextTime ? "[X]" : "[ ]";

        context.text(font, mark, checkboxX, checkboxY + 6, 0xFFFFFFFF, true);
        context.text(font, Component.translatable("gui.fbxplayermodels.do_not_show_again").getString(), textX, checkboxY + 6, 0xFFFFFFFF, true);
    }

    private boolean isInsideCheckbox(double mouseX, double mouseY) {
        int checkboxX = getCheckboxX();
        int checkboxY = getCheckboxY();
        int labelWidth = font.width(Component.translatable("gui.fbxplayermodels.do_not_show_again"));
        return mouseX >= checkboxX && mouseX <= checkboxX + CHECKBOX_SIZE + 8 + labelWidth
                && mouseY >= checkboxY && mouseY <= checkboxY + CHECKBOX_SIZE;
    }

    @Unique private int getTextWidth() {
        return Math.max(1, this.width - getMargin() * 2);
    }

    @Unique private int getCheckboxX() {
        int labelWidth = font.width(Component.translatable("gui.fbxplayermodels.do_not_show_again"));
        return this.width / 2 - (CHECKBOX_SIZE + 8 + labelWidth) / 2;
    }

    @Unique private int getCheckboxY() {
        return getButtonY() - CHECKBOX_SIZE - CONTROL_GAP;
    }

    @Unique private int getButtonY() {
        return this.height - BUTTON_HEIGHT - Math.max(12, getMargin() / 2);
    }

    @Unique private int getLineSpacing() {
        return font.lineHeight + 5;
    }

    @Unique private int getMargin() {
        return Math.max(MIN_MARGIN, Math.min(MAX_MARGIN, Math.min(this.width, this.height) / 8));
    }

    @Unique private List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        for (String word : text.split(" ")) {
            String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (font.width(candidate) <= maxWidth) {
                currentLine = new StringBuilder(candidate);
            } else {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        return lines;
    }
}
