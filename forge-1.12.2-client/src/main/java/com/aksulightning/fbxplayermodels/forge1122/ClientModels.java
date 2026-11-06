package com.aksulightning.fbxplayermodels.forge1122;

import com.aksulightning.fbxplayermodels.forge1122.client.ClientConfig;
import com.aksulightning.fbxplayermodels.forge1122.client.ClientEvents;
import com.aksulightning.fbxplayermodels.forge1122.client.ModelScreen;
import com.aksulightning.fbxplayermodels.forge1122.util.ModelNormalizer;
import com.aksulightning.fbxplayermodels.forge1122.util.model.animation.LogicalRigAnimator;
import com.aksulightning.fbxplayermodels.forge1122.util.model.rig.LogicalRigBinding;
import com.aksulightning.fbxplayermodels.forge1122.util.objects.SkinnedModel;
import com.aksulightning.fbxplayermodels.forge1122.util.objects.Vertex;
import com.aksulightning.fbxplayermodels.forge1122.util.parsing.DynamicTextureLoader;
import com.aksulightning.fbxplayermodels.forge1122.util.parsing.FBXParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public final class ClientModels {
    public static final KeyBinding SETTINGS = new KeyBinding("key.fbxplayermodels.settings", Keyboard.KEY_O,
            "key.categories.fbxplayermodels");
    private static final AtomicBoolean PICKER_OPEN = new AtomicBoolean();
    public static ClientConfig config;
    public static String status = "Select an FBX model to begin.";
    private static SkinnedModel imported;
    private static SkinnedModel animated;
    private static List<Vertex> staticVertices = Collections.emptyList();

    private ClientModels() {}

    public static void initialize() {
        config = ClientConfig.load();
        ClientRegistry.registerKeyBinding(SETTINGS);
        MinecraftForge.EVENT_BUS.register(new ClientEvents());
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager())
                .registerReloadListener(resources -> reload());
    }

    public static void openSettings() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen instanceof ModelScreen) return;
        mc.displayGuiScreen(new ModelScreen(mc.currentScreen));
    }

    public static SkinnedModel model() { return animated; }
    public static SkinnedModel importedModel() { return imported; }
    public static List<Vertex> staticVertices() { return staticVertices; }
    public static boolean hasModel() { return animated != null || !staticVertices.isEmpty(); }

    public static void reload() {
        DynamicTextureLoader.clear();
        imported = null;
        animated = null;
        staticVertices = Collections.emptyList();
        if (config != null && !config.modelHash.isEmpty()) {
            load(ClientConfig.models().resolve(config.modelHash + ".fbx"), false);
        }
    }

    public static boolean select(Path source) {
        try {
            source = source.toAbsolutePath().normalize();
            if (!source.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".fbx")) {
                throw new IllegalArgumentException("Choose an .fbx model.");
            }
            if (!Files.isRegularFile(source) || Files.size(source) == 0 || Files.size(source) > 128L * 1024 * 1024) {
                throw new IllegalArgumentException("The model must be a readable FBX file smaller than 128 MiB.");
            }
            byte[] bytes = Files.readAllBytes(source);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder hex = new StringBuilder();
            for (byte value : digest) hex.append(String.format(Locale.ROOT, "%02x", value & 255));
            String hash = hex.toString();
            Files.createDirectories(ClientConfig.models());
            Path cached = ClientConfig.models().resolve(hash + ".fbx");
            Files.write(cached, bytes);
            cacheTextures(source, hash);
            if (!load(cached, true)) return false;
            config.modelHash = hash;
            config.modelName = source.getFileName().toString();
            config.scale = 1f;
            config.save();
            return true;
        } catch (Exception exception) {
            status = "Import failed: " + exception.getMessage();
            FBXPlayerModelsMod.LOGGER.warn("Could not import local FBX model", exception);
            return false;
        }
    }

    private static boolean load(Path path, boolean newSelection) {
        try {
            FBXParser parser = new FBXParser();
            Optional<SkinnedModel> skin = parser.parseSkinned(path);
            SkinnedModel next = skin.map(ModelNormalizer::normalize).orElse(null);
            List<Vertex> vertices = next == null
                    ? ModelNormalizer.normalize(parser.parse(path)) : Collections.emptyList();
            if (next == null && vertices.isEmpty()) {
                throw new IllegalArgumentException("No renderable mesh was found in the FBX file.");
            }
            imported = next;
            staticVertices = vertices;
            if (newSelection) {
                config.binding = next == null ? new LogicalRigBinding()
                        : LogicalRigBinding.autoBind(next.bones.stream().map(SkinnedModel.Bone::name)
                                .collect(Collectors.toList()));
                config.clips.clear();
            }
            applySettings();
            retainModelTextures();
            status = next == null ? "Static FBX loaded." : "FBX loaded: " + next.bones.size() + " bones, "
                    + next.weightedVertexCount() + " weighted vertices.";
            return true;
        } catch (Exception exception) {
            retainModelTextures();
            status = "Import failed: " + exception.getMessage();
            FBXPlayerModelsMod.LOGGER.warn("Could not load local FBX model {}", path, exception);
            return false;
        }
    }

    public static void applySettings() {
        if (imported == null) { animated = null; return; }
        Map<String, SkinnedModel.Animation> animations = new LinkedHashMap<>(imported.animations);
        LogicalRigAnimator.proceduralAnimations(imported.bones, config.binding).forEach(animations::putIfAbsent);
        config.clips.forEach((state, clip) -> {
            if (!"Idle".equals(state) && imported.animations.containsKey(clip)) {
                animations.put(state, imported.animations.get(clip));
            }
        });
        animated = imported.withLogicalRigBinding(config.binding).withAnimations(animations)
                .withAnimationsEnabled(config.animations);
    }

    public static void saveSettings() { applySettings(); config.save(); }

    private static void retainModelTextures() {
        Set<Integer> used = new HashSet<>();
        List<Vertex> vertices = imported == null ? staticVertices : imported.staticVertices();
        for (Vertex vertex : vertices) used.add(vertex.texture);
        DynamicTextureLoader.retain(used);
    }

    private static void cacheTextures(Path source, String hash) throws java.io.IOException {
        Path parent = source.getParent();
        Path assets = ClientConfig.models().resolve(hash + ".assets").toAbsolutePath().normalize();
        for (Path image : FBXParser.externalTextureFiles(source)) {
            Path absolute = image.toAbsolutePath().normalize();
            Path relative = absolute.startsWith(parent) ? parent.relativize(absolute) : absolute.getFileName();
            Path output = assets.resolve(relative).normalize();
            if (!output.startsWith(assets)) continue;
            if (absolute.equals(output)) continue;
            Files.createDirectories(output.getParent());
            Files.copy(absolute, output, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void reset() {
        imported = null;
        animated = null;
        staticVertices = Collections.emptyList();
        DynamicTextureLoader.clear();
        config.modelHash = "";
        config.modelName = "";
        config.binding = new LogicalRigBinding();
        config.clips.clear();
        config.save();
        status = "Model reset.";
    }

    public static void chooseFile() {
        if (!PICKER_OPEN.compareAndSet(false, true)) return;
        status = "Choosing a model...";
        SwingUtilities.invokeLater(() -> {
            Path selected = null;
            String error = null;
            try {
                JFileChooser chooser = new JFileChooser(ClientConfig.models().toFile());
                chooser.setDialogTitle("Choose an FBX player model");
                chooser.setAcceptAllFileFilterUsed(false);
                chooser.setFileFilter(new FileNameExtensionFilter("FBX Model (*.fbx)", "fbx"));
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    selected = chooser.getSelectedFile().toPath();
                }
            } catch (Exception exception) {
                error = "File picker unavailable. Enter a path in the model menu.";
                FBXPlayerModelsMod.LOGGER.warn("Could not open file picker", exception);
            } finally {
                Path result = selected;
                String failure = error;
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    PICKER_OPEN.set(false);
                    if (result != null) select(result);
                    else status = failure == null ? "Selection cancelled." : failure;
                });
            }
        });
    }
}
