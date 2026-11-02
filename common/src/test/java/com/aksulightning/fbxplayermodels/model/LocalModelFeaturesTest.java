package com.aksulightning.fbxplayermodels.model;

import com.aksulightning.fbxplayermodels.model.shape.ShapeKey;
import com.aksulightning.fbxplayermodels.model.shape.ShapeKeyProfile;
import com.aksulightning.fbxplayermodels.voice.VoiceShapePose;
import com.aksulightning.fbxplayermodels.voice.VoiceShapeSettings;
import me.onethecrazy.util.objects.save.ClientSkin;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/** Headless contracts shared by full Fabric and the client-only model pipeline. */
public final class LocalModelFeaturesTest {
    @Test
    public void everySignedFileUpAxisMapsToPositiveModelY() {
        for (int axis = 0; axis < 3; axis++) {
            for (int sign : new int[]{-1, 1}) {
                Vector3f up = new Vector3f();
                up.setComponent(axis, sign);
                Matrix4f conversion = FbxCoordinateSpace.fromUpAxis(axis, sign);
                Vector3f result = conversion.transformDirection(up);
                assertTrue("axis=" + axis + ", sign=" + sign, result.distance(new Vector3f(0, 1, 0)) < 0.00001f);
                assertEquals(1f, conversion.determinant(), 0.00001f);
            }
        }
    }

    @Test
    public void yUpGeometryKeepsItsOriginalOrientation() {
        Vector3f point = new Vector3f(2, 3, 4);
        assertEquals(point, FbxCoordinateSpace.fromUpAxis(1, 1).transformPosition(new Vector3f(point)));
    }

    @Test
    public void invalidAxisMetadataLeavesGeometryUnchanged() {
        assertEquals(new Matrix4f(), FbxCoordinateSpace.fromUpAxis(4, 1));
        assertEquals(new Matrix4f(), FbxCoordinateSpace.fromUpAxis(2, 0));
    }

    @Test
    public void shapeNormalizationScalesDeltasWithoutAddingGroundTranslation() {
        float[] positions = {1, 0, -1};
        float[] normals = {0, 0.25f, 0};
        ShapeKey key = new ShapeKey("smile", "Smile",
                List.of(new ShapeKey.DeltaBlock(3, positions, normals)));
        ShapeKey normalized = key.normalized(new Matrix4f().translation(50, -20, 100).scale(2));
        assertArrayEquals(new float[]{2, 0, -2}, normalized.blocks().getFirst().positions(), 0.00001f);
        assertArrayEquals(normals, normalized.blocks().getFirst().normals(), 0f);
        assertArrayEquals(new float[]{1, 0, -1}, positions, 0f);
        assertEquals(3, normalized.blocks().getFirst().firstVertex());
    }

    @Test
    public void defaultWeightsKeepOtherKeysAndRejectInvalidValues() {
        ShapeKeyProfile profile = new ShapeKeyProfile();
        profile.setWeight("smile", 0.35f);
        profile.setWeight("blink", 2f);
        profile.setWeight("invalid", Float.NaN);
        assertEquals(0.35f, profile.weight("smile"), 0f);
        assertEquals(1f, profile.weight("blink"), 0f);
        assertEquals(0f, profile.weight("invalid"), 0f);
        profile.reset();
        assertEquals(0f, profile.weight("smile"), 0f);
    }

    @Test
    public void legacyModelSettingsInitializeShapeAndVoiceSafely() {
        ClientSkin skin = new ClientSkin();
        skin.shapeKeyProfiles = null;
        skin.voiceShapeSettings = null;
        assertEquals(0f, skin.defaultShapeKeyProfile().weight("smile"), 0f);
        assertFalse(skin.voiceShapeSettings().enabled());
    }

    @Test
    public void voiceBoneMappingInterpolatesAllAxesAndClampsInput() {
        VoiceShapeSettings settings = new VoiceShapeSettings();
        settings.target = VoiceShapeSettings.Target.BONE;
        settings.name = "Jaw";
        settings.startX = -20; settings.startY = 10; settings.startZ = 30;
        settings.endX = 20; settings.endY = -10; settings.endZ = -30;
        VoiceShapePose half = VoiceShapePose.from(settings, 0.5f);
        assertEquals("Jaw", half.boneName());
        assertEquals(0f, half.x(), 0f);
        assertEquals(0f, half.y(), 0f);
        assertEquals(0f, half.z(), 0f);
        assertEquals(-20f, VoiceShapePose.from(settings, -1).x(), 0f);
        assertEquals(20f, VoiceShapePose.from(settings, 2).x(), 0f);
    }

    @Test
    public void voiceShapeMappingSupportsReversedEndpoints() {
        VoiceShapeSettings settings = new VoiceShapeSettings();
        settings.target = VoiceShapeSettings.Target.SHAPE_KEY;
        settings.name = "Mouth";
        settings.shapeKeyId = "mesh:mouth";
        settings.startWeight = 0.9f;
        settings.endWeight = 0.1f;
        VoiceShapePose pose = VoiceShapePose.from(settings, 0.25f);
        assertEquals("mesh:mouth", pose.shapeKeyId());
        assertEquals(0.7f, pose.weight(), 0.00001f);
        settings.shapeKeyId = "";
        assertTrue(VoiceShapePose.from(settings, 1).isNone());
    }
}
