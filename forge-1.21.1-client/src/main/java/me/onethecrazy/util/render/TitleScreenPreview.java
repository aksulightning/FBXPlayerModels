package me.onethecrazy.util.render;

import me.onethecrazy.screens.ConfigScreen;
import me.onethecrazy.screens.rendering.SkinPreviewRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.Map;
import java.util.WeakHashMap;

public final class TitleScreenPreview {
    private static final int DIMENSIONS = 68;
    private static final Map<TitleScreen, SkinPreviewRenderer> PREVIEWS = new WeakHashMap<>();

    private TitleScreenPreview() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(TitleScreenPreview::initialize);
        MinecraftForge.EVENT_BUS.addListener(TitleScreenPreview::render);
        MinecraftForge.EVENT_BUS.addListener(TitleScreenPreview::mouseClicked);
    }

    private static void initialize(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen screen) {
            PREVIEWS.put(screen, new SkinPreviewRenderer(originX(screen), originY(screen), DIMENSIONS, 30f));
        }
    }

    private static void render(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof TitleScreen screen) {
            SkinPreviewRenderer preview = PREVIEWS.get(screen);
            if (preview != null) {
                preview.renderPreview(event.getGuiGraphics(), event.getPartialTick());
            }
        }
    }

    private static void mouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getScreen() instanceof TitleScreen screen && event.getButton() == 0
                && event.getMouseX() > originX(screen) && event.getMouseX() < originX(screen) + DIMENSIONS
                && event.getMouseY() > originY(screen) && event.getMouseY() < originY(screen) + DIMENSIONS) {
            Minecraft.getInstance().setScreen(ConfigScreen.create(screen));
            event.setCanceled(true);
        }
    }

    private static int originX(TitleScreen screen) {
        return screen.width / 2 - 6 - 98 - DIMENSIONS;
    }

    private static int originY(TitleScreen screen) {
        return screen.height / 4 + 24 * 2;
    }
}
