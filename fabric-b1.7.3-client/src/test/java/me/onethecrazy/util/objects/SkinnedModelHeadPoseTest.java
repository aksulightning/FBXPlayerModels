package me.onethecrazy.util.objects;

import org.junit.Test;

/** Runs the imported head/neck and player-state regression without an OpenGL display. */
public final class SkinnedModelHeadPoseTest {
    @Test
    public void preservesHeadRigAndPlayerPoseBehavior() throws Exception {
        SkinnedModelHeadPoseRegression.main(new String[0]);
    }
}
