package me.onethecrazy.util.render;

import org.joml.Vector3f;
import org.junit.Test;

import static org.junit.Assert.*;

/** Checks the signed-byte normals OpenGL actually reads, without creating a display. */
public class LegacyModelRendererTest {
    @Test
    public void axisNormalsKeepTheirDirectionAndOtherComponentsZero() {
        for (Vector3f expected : new Vector3f[]{
                new Vector3f(1, 0, 0), new Vector3f(-1, 0, 0),
                new Vector3f(0, 1, 0), new Vector3f(0, -1, 0),
                new Vector3f(0, 0, 1), new Vector3f(0, 0, -1)}) {
            assertEquals(expected, unpack(LegacyModelRenderer.packNormal(expected.x, expected.y, expected.z)));
        }
    }

    @Test
    public void mixedSignedNormalsPreserveDirectionWithinBytePrecision() {
        for (Vector3f value : new Vector3f[]{new Vector3f(-0.6f, 0.8f, 0),
                new Vector3f(0, -0.6f, 0.8f), new Vector3f(-0.2f, -0.4f, -0.8f),
                new Vector3f(1, 1, -1)}) {
            Vector3f expected = value.normalize();
            Vector3f actual = unpack(LegacyModelRenderer.packNormal(expected.x, expected.y, expected.z)).normalize();
            assertTrue(expected.distance(actual) < 0.02f);
        }
    }

    private static Vector3f unpack(int value) {
        return new Vector3f((byte) value, (byte) (value >> 8), (byte) (value >> 16)).div(127f);
    }
}
