package com.aksulightning.fbxplayermodels;

public interface FbxModelEntity {
    String getModel();

    default String getFbxAnimation(float tickDelta) {
        return "Idle";
    }
}
