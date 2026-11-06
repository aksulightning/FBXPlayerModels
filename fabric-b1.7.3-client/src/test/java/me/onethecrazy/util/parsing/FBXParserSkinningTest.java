package me.onethecrazy.util.parsing;

import me.onethecrazy.util.ModelNormalizer;
import me.onethecrazy.util.model.animation.CustomModelPose;
import me.onethecrazy.util.objects.SkinnedModel;
import me.onethecrazy.util.objects.Vertex;
import me.onethecrazy.util.render.LegacyModelRenderer;
import org.joml.Vector3f;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** Import/deformation contracts; texture IDs are supplied without opening an OpenGL context. */
public class FBXParserSkinningTest {
    private static final Class<?> NODE = nested("BinaryNode");
    private static int previousWhiteTexture;

    @BeforeClass
    public static void useHeadlessTextureId() throws ReflectiveOperationException {
        Field white = DynamicTextureLoader.class.getDeclaredField("whiteTexture");
        white.setAccessible(true);
        previousWhiteTexture = white.getInt(null);
        white.setInt(null, 7);
    }

    @AfterClass
    public static void restoreTextureId() throws ReflectiveOperationException {
        Field white = DynamicTextureLoader.class.getDeclaredField("whiteTexture");
        white.setAccessible(true);
        white.setInt(null, previousWhiteTexture);
    }

    @Test
    public void laterDominantBoneInfluencesSurviveImportAndDeformation() throws ReflectiveOperationException {
        Object objects = node("Objects");
        Object connections = node("Connections");
        Object geometry = node("Geometry", 10L, "Mesh", "Mesh");
        children(geometry).add(node("Vertices", new double[]{1, 0, 0, 0, 0, 0, 0, 0, 1}));
        children(geometry).add(node("PolygonVertexIndex", new int[]{0, 1, -3}));
        children(objects).add(geometry);
        children(objects).add(node("Deformer", 20L, "Skin", "Skin"));
        children(connections).add(node("C", "OO", 20L, 10L));
        for (int i = 0; i < 6; i++) {
            long bone = 100L + i, cluster = 200L + i;
            children(objects).add(node("Model", bone, i == 5 ? "Head" : "Helper" + i, "LimbNode"));
            Object skin = node("Deformer", cluster, "Cluster" + i, "Cluster");
            children(skin).add(node("Indexes", new int[]{0, 1, 2}));
            double weight = i == 5 ? 0.95 : 0.01;
            children(skin).add(node("Weights", new double[]{weight, weight, weight}));
            children(skin).add(node("Transform", identity()));
            children(skin).add(node("TransformLink", identity()));
            children(objects).add(skin);
            children(connections).add(node("C", "OO", cluster, 20L));
            children(connections).add(node("C", "OO", bone, cluster));
        }
        Object root = node("");
        children(root).addAll(List.of(objects, connections));
        SkinnedModel model = importGeometry(root, Path.of("headless.fbx"));
        assertEquals(6, model.vertices.get(0).boneIds.length);
        assertEquals(0.95f, model.vertices.get(0).weights[5], 0.00001f);
        Vertex posed = model.renderWithForcedHeadLook("Idle", 0,
                CustomModelPose.createMinecraftHeadLookRotation(60, 0), CustomModelPose.LimbPose.NONE).get(0);
        assertEquals(0.525f, posed.position.x, 0.00001f);
        assertEquals(0.8227241f, posed.position.z, 0.00001f);
    }

    @Test
    public void suppliedModelKeepsItsBindPoseAndAllInfluences() throws Exception {
        Path asset = Path.of(System.getProperty("fbx.modelFixture", "../.temp/a model.fbx"));
        assumeTrue("Optional user-supplied mesh is not installed", Files.isRegularFile(asset));
        Class<?> readerType = nested("BinaryFbxReader");
        Object reader = construct(readerType, new Class<?>[]{byte[].class}, (Object) Files.readAllBytes(asset));
        Method readRoot = readerType.getDeclaredMethod("readRoot");
        readRoot.setAccessible(true);
        SkinnedModel model = ModelNormalizer.normalize(importGeometry(readRoot.invoke(reader), asset));
        List<Vertex> base = model.staticVertices();
        List<Vertex> bind = model.render("Idle", 0, CustomModelPose.HeadLookRotation.NONE);
        float maxBindError = 0f;
        for (int i = 0; i < base.size(); i++) {
            maxBindError = Math.max(maxBindError, position(base.get(i)).distance(position(bind.get(i))));
        }
        List<Vertex> turned = model.renderWithForcedHeadLook("Idle", 0,
                CustomModelPose.createMinecraftHeadLookRotation(60, 30), CustomModelPose.LimbPose.NONE);
        float maxStretch = 0f;
        int stretchedEdges = 0;
        for (int i = 0; i + 3 < base.size(); i += 4) {
            for (int j = 0; j < 3; j++) {
                int a = i + j, b = i + (j + 1) % 3;
                float original = position(base.get(a)).distance(position(base.get(b)));
                if (original < 0.001f) continue;
                float ratio = position(turned.get(a)).distance(position(turned.get(b))) / original;
                maxStretch = Math.max(maxStretch, ratio);
                if (ratio > 5f) stretchedEdges++;
            }
        }
        int influences = model.vertices.stream().mapToInt(v -> v.boneIds.length).max().orElse(0);
        System.out.println("a model: vertices=" + base.size() + " bones=" + model.bones.size()
                + " maxInfluences=" + influences + " bindError=" + maxBindError
                + " headTurnMaxEdgeStretch=" + maxStretch + " severelyStretchedEdges=" + stretchedEdges);
        assertTrue("Enabling animation without a pose must preserve geometry", maxBindError < 0.0001f);
        assertEquals("The supplied mesh has up to nine authored influences per control point", 9, influences);
        assertEquals("Static mesh normals must survive GPU attribute packing", 0, normalErrors(base));
        assertEquals("Animated bind-pose normals must survive GPU attribute packing", 0, normalErrors(bind));
        assertEquals("Turned head normals must survive GPU attribute packing", 0, normalErrors(turned));
    }

    private static Vector3f position(Vertex v) {
        return new Vector3f(v.position.x, v.position.y, v.position.z);
    }

    private static int normalErrors(List<Vertex> vertices) {
        int errors = 0;
        for (Vertex v : vertices) {
            int packed = LegacyModelRenderer.packNormal(v.normals.x, v.normals.y, v.normals.z);
            Vector3f actual = new Vector3f((byte) packed, (byte) (packed >> 8), (byte) (packed >> 16)).normalize();
            Vector3f expected = new Vector3f(v.normals.x, v.normals.y, v.normals.z).normalize();
            if (actual.distance(expected) > 0.02f) errors++;
        }
        return errors;
    }

    private static double[] identity() {
        return new double[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
    }

    private static Object node(String name, Object... properties) throws ReflectiveOperationException {
        Object node = construct(NODE, new Class<?>[]{String.class}, name);
        list(node, "properties").addAll(Arrays.asList(properties));
        return node;
    }

    private static List<Object> children(Object node) throws ReflectiveOperationException {
        return list(node, "children");
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object object, String name) throws ReflectiveOperationException {
        return (List<Object>) field(object, name);
    }

    private static Object field(Object object, String name) throws ReflectiveOperationException {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static Class<?> nested(String name) {
        try {
            return Class.forName(FBXParser.class.getName() + "$" + name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    private static Object construct(Class<?> type, Class<?>[] parameters, Object... arguments) throws ReflectiveOperationException {
        Constructor<?> constructor = type.getDeclaredConstructor(parameters);
        constructor.setAccessible(true);
        return constructor.newInstance(arguments);
    }

    @SuppressWarnings("unchecked")
    private static SkinnedModel importGeometry(Object root, Path source) throws ReflectiveOperationException {
        Class<?> indexType = nested("SceneIndex"), materialsType = nested("MaterialResolver");
        Object index = construct(indexType, new Class<?>[]{NODE}, root);
        Object materials = construct(materialsType, new Class<?>[]{NODE, Path.class}, root, source);
        List<Object> meshes = new ArrayList<>();
        for (Object group : children(root)) {
            if (!"Objects".equals(field(group, "name"))) continue;
            for (Object object : children(group)) {
                String name = (String) field(object, "name");
                List<Object> properties = list(object, "properties");
                if (name.equals("Geometry") && properties.size() >= 3 && properties.get(2).equals("Mesh")) meshes.add(object);
                if (name.equals("Texture")) ((Map<Long, Integer>) field(materials, "textureCache")).put(((Number) properties.get(0)).longValue(), 7);
                if (name.equals("Video")) ((Map<Long, Integer>) field(materials, "mediaCache")).put(((Number) properties.get(0)).longValue(), 7);
            }
        }
        Method build = FBXParser.class.getDeclaredMethod("buildSkinnedModel", indexType, List.class, materialsType);
        build.setAccessible(true);
        return ((Optional<SkinnedModel>) build.invoke(new FBXParser(), index, meshes, materials)).orElseThrow();
    }
}
