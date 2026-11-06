package com.aksulightning.fbxplayermodels.forge1122.util.render;

import com.aksulightning.fbxplayermodels.forge1122.util.objects.Vertex;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.List;

public final class LegacyModelRenderer {
    private LegacyModelRenderer() {}

    public static void render(List<Vertex> vertices) {
        if (vertices == null || vertices.isEmpty()) return;
        int texture = GlStateManager.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GlStateManager.pushMatrix();
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushClientAttrib(-1);
        try {
            // Raw GL is confined to this scope so Minecraft's cached state stays consistent.
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_NORMALIZE);
            // Standard item lighting selects GL_FLAT; interpolate the imported vertex normals' lighting.
            // Keep Minecraft's cached shade mode unchanged and restore native state with glPopAttrib().
            GL11.glShadeModel(GL11.GL_SMOOTH);
            GL11.glDisable(GL11.GL_CULL_FACE);
            int start = 0;
            while (start < vertices.size()) {
                int next = start + 1;
                int id = vertices.get(start).texture;
                while (next < vertices.size() && vertices.get(next).texture == id) next++;
                GlStateManager.bindTexture(id);
                BufferBuilder buffer = Tessellator.getInstance().getBuffer();
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);
                for (int i = start; i < next; i++) {
                    Vertex vertex = vertices.get(i);
                    int color = vertex.color;
                    buffer.pos(vertex.position.x, vertex.position.y, vertex.position.z)
                            .tex(vertex.textureUV.u, vertex.textureUV.v)
                            .color(color >>> 16 & 255, color >>> 8 & 255, color & 255, color >>> 24 & 255)
                            .normal(vertex.normals.x, vertex.normals.y, vertex.normals.z).endVertex();
                }
                Tessellator.getInstance().draw();
                start = next;
            }
        } finally {
            // Restore through the state manager before popping the native GL state.
            GlStateManager.bindTexture(texture);
            GL11.glPopClientAttrib();
            GL11.glPopAttrib();
            GlStateManager.resetColor();
            GlStateManager.popMatrix();
        }
    }
}
