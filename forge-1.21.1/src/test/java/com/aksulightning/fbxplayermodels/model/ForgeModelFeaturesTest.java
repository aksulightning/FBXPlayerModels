package com.aksulightning.fbxplayermodels.model;

import com.aksulightning.fbxplayermodels.model.shape.ShapeKey;
import com.aksulightning.fbxplayermodels.model.shape.ShapeKeyProfile;
import com.aksulightning.fbxplayermodels.voice.VoiceShapePose;
import com.google.gson.Gson;
import me.onethecrazy.util.ModelNormalizer;
import me.onethecrazy.util.model.animation.CustomModelPose;
import me.onethecrazy.util.objects.*;
import me.onethecrazy.util.objects.save.FBXPlayerModelsSave;
import org.joml.Matrix4f;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/** Exercises the production Forge geometry without a game, display or microphone. */
public final class ForgeModelFeaturesTest {
    @Test
    public void profileEditsAndResetAlwaysStartFromBaseGeometry() {
        SkinnedModel base = model();
        ShapeKeyProfile profile = new ShapeKeyProfile();
        profile.setWeight("mouth", 0.5f);
        SkinnedModel half = base.withShapeKeyProfile(profile);
        assertPosition(half.staticVertices().getFirst(), 1, 0, 0);
        profile.setWeight("mouth", 1f);
        SkinnedModel full = half.withShapeKeyProfile(profile);
        assertPosition(full.staticVertices().getFirst(), 2, 0, 0);
        profile.reset();
        assertPosition(full.withShapeKeyProfile(profile).staticVertices().getFirst(), 0, 0, 0);
        assertPosition(base.vertices.getFirst().vertex, 0, 0, 0);
    }

    @Test
    public void voiceReplacesOnlyItsTargetAndRestoresDefaultAfterwards() {
        ShapeKeyProfile profile = new ShapeKeyProfile();
        profile.setWeight("mouth", 0.25f);
        profile.setWeight("blink", 0.5f);
        SkinnedModel model = model().withShapeKeyProfile(profile);
        VoiceShapePose voice = new VoiceShapePose("", "mouth", 0, 0, 0, 0.75f);
        assertPosition(model.voicePreview(voice).getFirst(), 1.5f, 0.5f, 0);
        assertPosition(model.voicePreview(VoiceShapePose.NONE).getFirst(), 0.5f, 0.5f, 0);
        assertPosition(model.voicePreview(voice).getFirst(), 1.5f, 0.5f, 0);
    }

    @Test
    public void voiceBoneRotationPropagatesToDescendantsWithAnimationsDisabled() {
        SkinnedModel model = new SkinnedModel(List.of(
                new SkinnedModel.Bone("Head", -1, new Matrix4f(), new Matrix4f()),
                new SkinnedModel.Bone("Jaw", 0, new Matrix4f().translation(1, 0, 0), new Matrix4f().translation(-1, 0, 0)),
                new SkinnedModel.Bone("Tip", 1, new Matrix4f().translation(1, 0, 0), new Matrix4f().translation(-2, 0, 0))),
                List.of(vertex(2, 0, 0, 2)), Map.of()).withAnimationsEnabled(false);
        VoiceShapePose voice = new VoiceShapePose("Jaw", "", 0, 0, 90, 0);
        assertPosition(model.render("Idle", 0, CustomModelPose.HeadLookRotation.NONE,
                CustomModelPose.LimbPose.NONE, voice).getFirst(), 1, 1, 0);
        assertPosition(model.staticVertices().getFirst(), 2, 0, 0);
    }

    @Test
    public void firstPersonStillHidesHeadTrianglesDuringVoiceAnimation() {
        SkinnedModel model = new SkinnedModel(
                List.of(new SkinnedModel.Bone("Head", -1, new Matrix4f(), new Matrix4f())),
                List.of(vertex(0, 0, 0, 0), vertex(1, 0, 0, 0), vertex(0, 1, 0, 0)), Map.of());
        VoiceShapePose voice = new VoiceShapePose("Head", "", 0, 25, 0, 0);
        assertEquals(3, model.render("Idle", 0, CustomModelPose.HeadLookRotation.NONE,
                CustomModelPose.LimbPose.NONE, voice).size());
        assertTrue(model.renderWithHiddenHead("Idle", 0, CustomModelPose.HeadLookRotation.NONE,
                CustomModelPose.LimbPose.NONE, voice).isEmpty());
    }

    @Test
    public void normalizationScalesShapeDeltasExactlyOnce() {
        SkinnedModel normalized = ModelNormalizer.normalize(model());
        ShapeKeyProfile profile = new ShapeKeyProfile();
        profile.setWeight("mouth", 1f);
        assertPosition(normalized.withShapeKeyProfile(profile).staticVertices().getFirst(), 1, 0, 0);
        assertSame(normalized, ModelNormalizer.normalize(normalized));
        assertPosition(normalized.vertices.get(1).vertex, 0, 2, 0);
    }

    @Test
    public void legacyJsonKeepsSelectionAndDisplayPreferences() {
        FBXPlayerModelsSave save = new Gson().fromJson("""
                {"isEnabled":false,"renderSelfModelInFirstPerson":true,
                 "selectedSkin":{"hash":"existing","name":"pony.fbx","format":"FBX","animationsEnabled":false}}
                """, FBXPlayerModelsSave.class);
        assertFalse(save.areFbxPlayerModelsEnabled());
        assertTrue(save.renderSelfModelInFirstPerson);
        assertEquals("existing", save.selectedSkin.hash);
        assertFalse(save.selectedSkin.animationsEnabled());
        assertEquals(0f, save.selectedSkin.defaultShapeKeyProfile().weight("mouth"), 0f);
        assertFalse(save.selectedSkin.voiceShapeSettings().enabled());
    }

    private static SkinnedModel model() {
        return new SkinnedModel(List.of(new SkinnedModel.Bone("Head", -1, new Matrix4f(), new Matrix4f())),
                List.of(vertex(0, 0, 0, 0), vertex(0, 4, 0, 0)), Map.of())
                .withShapeKeys(List.of(
                        new ShapeKey("mouth", "Mouth", List.of(new ShapeKey.DeltaBlock(0, new float[]{2, 0, 0}, new float[3]))),
                        new ShapeKey("blink", "Blink", List.of(new ShapeKey.DeltaBlock(0, new float[]{0, 1, 0}, new float[3])))));
    }

    private static SkinnedVertex vertex(float x, float y, float z, int bone) {
        return new SkinnedVertex(new Vertex(new Float3(x, y, z), new Float3(0, 1, 0),
                new Float2(0, 0), null, 0xFFFFFFFF), new int[]{bone}, new float[]{1});
    }

    private static void assertPosition(Vertex vertex, float x, float y, float z) {
        assertEquals(x, vertex.position.x, 0.00001f);
        assertEquals(y, vertex.position.y, 0.00001f);
        assertEquals(z, vertex.position.z, 0.00001f);
    }
}
