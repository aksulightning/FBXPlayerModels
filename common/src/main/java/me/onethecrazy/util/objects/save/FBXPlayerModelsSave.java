package me.onethecrazy.util.objects.save;

public class FBXPlayerModelsSave {
    public ClientSkin selectedSkin;
    private boolean isEnabled;
    public boolean renderSelfModelInFirstPerson;
    public float firstPersonCameraOffsetX;
    public float firstPersonCameraOffsetY;
    public float firstPersonCameraOffsetZ;
    public boolean hideCommunityServerDisclaimer;

    public FBXPlayerModelsSave(){
        this.selectedSkin = new ClientSkin();
        this.isEnabled = false;
        this.renderSelfModelInFirstPerson = false;
        this.firstPersonCameraOffsetX = 0f;
        this.firstPersonCameraOffsetY = 0f;
        this.firstPersonCameraOffsetZ = 0f;
        this.hideCommunityServerDisclaimer = false;
    }

    public boolean areFbxPlayerModelsEnabled() {
        return isEnabled;
    }

    public void setFbxPlayerModelsEnabled(boolean enabled) {
        isEnabled = enabled;
    }
}
