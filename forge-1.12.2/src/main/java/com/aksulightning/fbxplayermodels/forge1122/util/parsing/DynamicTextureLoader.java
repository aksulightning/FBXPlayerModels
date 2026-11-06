package com.aksulightning.fbxplayermodels.forge1122.util.parsing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** All calls run on the Minecraft thread, which owns the GL context. */
public final class DynamicTextureLoader {
    private static final Map<String, ResourceLocation> LOCATIONS = new HashMap<>();
    private static final Map<String, Integer> TEXTURES = new HashMap<>();

    private DynamicTextureLoader() {}

    private static TextureManager manager() {
        return Minecraft.getMinecraft().getTextureManager();
    }

    public static int whiteTexture() {
        Integer texture = TEXTURES.get("white");
        if (texture != null) return texture;
        BufferedImage white = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        white.setRGB(0, 0, 0xFFFFFFFF);
        return register("white", white);
    }

    public static int load(Path path) throws IOException { return load(path, null); }

    public static int load(Path path, String stableName) throws IOException {
        return load(Files.readAllBytes(path), stableName);
    }

    public static int load(byte[] bytes) throws IOException { return load(bytes, null); }

    public static int load(byte[] bytes, String stableName) throws IOException {
        String key = stableName == null ? UUID.randomUUID().toString() : stableName;
        Integer cached = TEXTURES.get(key);
        if (cached != null) return cached;
        int offset = imageOffset(bytes);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes, offset, bytes.length - offset));
        if (image == null) throw new IOException("Unsupported FBX texture image");
        return register(key, image);
    }

    private static int register(String key, BufferedImage image) {
        DynamicTexture texture = new DynamicTexture(image);
        ResourceLocation location = manager().getDynamicTextureLocation("fbxplayermodels", texture);
        LOCATIONS.put(key, location);
        TEXTURES.put(key, texture.getGlTextureId());
        return texture.getGlTextureId();
    }

    private static int imageOffset(byte[] bytes) {
        for (int i = 0; i + 4 <= bytes.length; i++) {
            if ((bytes[i] & 255) == 137 && bytes[i + 1] == 80 && bytes[i + 2] == 78 && bytes[i + 3] == 71
                    || (bytes[i] & 255) == 255 && (bytes[i + 1] & 255) == 216 && (bytes[i + 2] & 255) == 255
                    || bytes[i] == 71 && bytes[i + 1] == 73 && bytes[i + 2] == 70 && bytes[i + 3] == 56) return i;
        }
        return 0;
    }

    public static void retain(Set<Integer> used) {
        Iterator<Map.Entry<String, Integer>> iterator = TEXTURES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();
            if (!used.contains(entry.getValue())) {
                manager().deleteTexture(LOCATIONS.remove(entry.getKey()));
                iterator.remove();
            }
        }
    }

    public static void clear() { retain(java.util.Collections.emptySet()); }
}
