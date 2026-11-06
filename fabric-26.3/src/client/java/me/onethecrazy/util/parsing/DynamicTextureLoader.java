package me.onethecrazy.util.parsing;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import me.onethecrazy.FBXPlayerModels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

// This is FULLY vibe coded
// This texture shit is a PAIN
public final class DynamicTextureLoader {

    private DynamicTextureLoader() {}

    public static Identifier load(Path imagePath) throws Exception {
        try (InputStream in = Files.newInputStream(imagePath)) {
            return readDecodeAndRegister(in, null);
        }
    }

    public static Identifier load(Path imagePath, String stableName) throws Exception {
        try (InputStream in = Files.newInputStream(imagePath)) {
            return readDecodeAndRegister(in, stableName);
        }
    }

    public static Identifier load(byte[] imageBytes) throws Exception {
        try (InputStream in = new ByteArrayInputStream(imageBytes)) {
            return readDecodeAndRegister(in, null);
        }
    }

    public static Identifier load(byte[] imageBytes, String stableName) throws Exception {
        try (InputStream in = new ByteArrayInputStream(imageBytes)) {
            return readDecodeAndRegister(in, stableName);
        }
    }

    private static Identifier readDecodeAndRegister(InputStream in, String stableName) throws Exception {
        byte[] raw = trimToImageHeader(readAll(in));
        NativeImage image;
        try {
            image = NativeImage.read(new ByteArrayInputStream(raw));
        } catch (Exception pngFail) {
            BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(raw));
            if (buffered == null) throw pngFail;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(buffered, "PNG", baos);
            image = NativeImage.read(new ByteArrayInputStream(baos.toByteArray()));
        }
        return uploadOnRenderThread(image, stableName);
    }

    private static Identifier uploadOnRenderThread(NativeImage image, String stableName) throws Exception {
        final Identifier id = Identifier.fromNamespaceAndPath(FBXPlayerModels.MOD_ID, "dynamic/" + identifierPath(stableName));
        final Minecraft mc = Minecraft.getInstance();

        if (RenderSystem.isOnRenderThread()) {
            DynamicTexture tex = new DynamicTexture(id::toString, image);
            mc.getTextureManager().register(id, tex);
            return id;
        }

        try {
            return mc.submit(() -> {
                DynamicTexture tex = new DynamicTexture(id::toString, image);
                mc.getTextureManager().register(id, tex);
                return id;
            }).get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception ex) throw ex;
            throw new RuntimeException(cause);
        }
    }

    private static byte[] trimToImageHeader(byte[] raw) {
        int offset = imageHeaderOffset(raw);
        if (offset <= 0) {
            return raw;
        }

        byte[] trimmed = new byte[raw.length - offset];
        System.arraycopy(raw, offset, trimmed, 0, trimmed.length);
        return trimmed;
    }

    private static int imageHeaderOffset(byte[] raw) {
        for (int i = 0; i < raw.length - 4; i++) {
            if (i + 8 <= raw.length
                    && (raw[i] & 0xFF) == 0x89
                    && raw[i + 1] == 0x50
                    && raw[i + 2] == 0x4E
                    && raw[i + 3] == 0x47
                    && raw[i + 4] == 0x0D
                    && raw[i + 5] == 0x0A
                    && raw[i + 6] == 0x1A
                    && raw[i + 7] == 0x0A) {
                return i;
            }
            if ((raw[i] & 0xFF) == 0xFF
                    && (raw[i + 1] & 0xFF) == 0xD8
                    && (raw[i + 2] & 0xFF) == 0xFF) {
                return i;
            }
            if (i + 6 <= raw.length
                    && raw[i] == 0x47
                    && raw[i + 1] == 0x49
                    && raw[i + 2] == 0x46
                    && raw[i + 3] == 0x38) {
                return i;
            }
        }
        return 0;
    }

    private static String identifierPath(String stableName) {
        if (stableName == null || stableName.isBlank()) {
            return UUID.randomUUID().toString();
        }

        String normalized = stableName.toLowerCase(Locale.ROOT).replace('\\', '/');
        normalized = normalized.replaceAll("[^a-z0-9_./-]", "_");
        normalized = normalized.replaceAll("/+", "/");
        normalized = normalized.replaceAll("^/+", "");
        return normalized.isBlank() ? UUID.randomUUID().toString() : normalized;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        in.transferTo(baos);
        return baos.toByteArray();
    }
}
