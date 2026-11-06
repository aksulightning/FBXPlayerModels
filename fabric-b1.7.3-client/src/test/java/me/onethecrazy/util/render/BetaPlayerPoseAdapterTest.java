package me.onethecrazy.util.render;

import me.onethecrazy.util.model.animation.LogicalRigAnimator;
import me.onethecrazy.util.objects.CacheSkin;
import me.onethecrazy.util.objects.Float2;
import me.onethecrazy.util.objects.Float3;
import me.onethecrazy.util.objects.SkinnedModel;
import me.onethecrazy.util.objects.SkinnedVertex;
import me.onethecrazy.util.objects.Vertex;
import me.onethecrazy.util.parsing.ParsingFormat;
import net.minecraft.client.input.Input;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.joml.Matrix4f;
import org.junit.Before;
import org.junit.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/** Exercises the shared first-/third-person adapter with an actual Beta player and weighted limbs. */
public class BetaPlayerPoseAdapterTest {
    private ClientPlayerEntity player;
    private CacheSkin skin;

    @Before
    public void createSneakingPlayerAndRig() throws ReflectiveOperationException {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        player = (ClientPlayerEntity) ((Unsafe) field.get(null)).allocateInstance(ClientPlayerEntity.class);
        player.input = new Input();
        player.input.sneaking = true;
        DataTracker tracker = new DataTracker();
        tracker.startTracking(0, (byte) 0);
        Field trackerField = Entity.class.getDeclaredField("dataTracker");
        trackerField.setAccessible(true);
        trackerField.set(player, tracker);

        List<SkinnedModel.Bone> bones = new ArrayList<>();
        List<SkinnedVertex> vertices = new ArrayList<>();
        bones.add(new SkinnedModel.Bone("Body", -1, new Matrix4f(), new Matrix4f()));
        addLimb(bones, vertices, "Head", 0f, 1.6f);
        addLimb(bones, vertices, "RightArm", -0.4f, 1.3f);
        addLimb(bones, vertices, "LeftArm", 0.4f, 1.3f);
        addLimb(bones, vertices, "RightLeg", -0.2f, 0.8f);
        addLimb(bones, vertices, "LeftLeg", 0.2f, 0.8f);
        SkinnedModel model = new SkinnedModel(bones, vertices, Map.of())
                .withAnimations(LogicalRigAnimator.proceduralAnimations(bones, null));
        skin = new CacheSkin(model, ParsingFormat.FBX);
    }

    @Test
    public void stationarySneakKeepsItsPoseAcrossTicksAndPartialTicks() {
        assertEquals("Sneak", BetaPlayerPoseAdapter.currentAnimation(player));
        for (boolean hideHead : new boolean[]{false, true}) {
            player.age = 0;
            List<Vertex> stationary = pose(0f, hideHead);
            for (int age : new int[]{1, 7, 23, 80}) {
                player.age = age;
                for (float partialTick : new float[]{0f, 0.5f, 1f}) {
                    assertSamePositions(stationary, pose(partialTick, hideHead));
                }
            }
        }
    }

    @Test
    public void sneakWalkingFollowsNativeGaitProgress() {
        player.lastWalkAnimationSpeed = player.walkAnimationSpeed = 0.35f;
        for (boolean hideHead : new boolean[]{false, true}) {
            player.age = 0;
            player.walkAnimationProgress = 0f;
            List<Vertex> start = pose(0.5f, hideHead);
            player.age = 80;
            assertSamePositions(start, pose(0.5f, hideHead));
            player.walkAnimationProgress = 2f;
            assertTrue("Native walking progress must still move crouched limbs", differs(start, pose(0.5f, hideHead)));
        }
    }

    @Test
    public void stoppingSneakWalkSettlesIntoTheStationaryPose() {
        List<Vertex> stationary = pose(1f, false);
        player.lastWalkAnimationSpeed = player.walkAnimationSpeed = 0.4f;
        player.walkAnimationProgress = 2f;
        assertTrue(differs(stationary, pose(1f, false)));

        player.walkAnimationSpeed = 0f;
        assertTrue("The previous tick's walking amplitude should fade through interpolation", differs(stationary, pose(0.5f, false)));
        assertSamePositions(stationary, pose(1f, false));
        player.lastWalkAnimationSpeed = 0f;
        player.age = 100;
        assertSamePositions(stationary, pose(0.5f, false));
    }

    private List<Vertex> pose(float partialTick, boolean hideHead) {
        return BetaPlayerPoseAdapter.vertices(skin, player, partialTick, hideHead);
    }

    private static void addLimb(List<SkinnedModel.Bone> bones, List<SkinnedVertex> vertices, String name, float x, float y) {
        int index = bones.size();
        bones.add(new SkinnedModel.Bone(name, 0, new Matrix4f().translation(x, y, 0),
                new Matrix4f().translation(-x, -y, 0)));
        for (float offset : new float[]{0f, -0.4f}) {
            Vertex vertex = new Vertex(new Float3(x, y + offset, 0.1f), new Float3(0, 0, 1),
                    new Float2(0, 0), 7, 0xFFFFFFFF);
            vertices.add(new SkinnedVertex(vertex, new int[]{index}, new float[]{1}));
        }
    }

    private static boolean differs(List<Vertex> before, List<Vertex> after) {
        for (int i = 0; i < before.size(); i++) {
            Float3 a = before.get(i).position;
            Float3 b = after.get(i).position;
            if (Math.abs(a.x - b.x) + Math.abs(a.y - b.y) + Math.abs(a.z - b.z) > 0.001f) return true;
        }
        return false;
    }

    private static void assertSamePositions(List<Vertex> expected, List<Vertex> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            Float3 a = expected.get(i).position;
            Float3 b = actual.get(i).position;
            assertEquals("vertex " + i + " x", a.x, b.x, 0.00001f);
            assertEquals("vertex " + i + " y", a.y, b.y, 0.00001f);
            assertEquals("vertex " + i + " z", a.z, b.z, 0.00001f);
        }
    }
}
