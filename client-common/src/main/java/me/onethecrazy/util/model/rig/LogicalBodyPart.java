package me.onethecrazy.util.model.rig;

public enum LogicalBodyPart {
    HEAD("Head"),
    CHEST("Chest"),
    RIGHT_ARM("Right Arm"),
    LEFT_ARM("Left Arm"),
    RIGHT_LEG("Right Leg"),
    LEFT_LEG("Left Leg");

    public final String displayName;

    LogicalBodyPart(String displayName) {
        this.displayName = displayName;
    }
}
