package me.onethecrazy.util.objects.save;

public class FBXPlayerModelsSave {
    public ClientSkin selectedSkin;
    public boolean isEnabled;
    public boolean renderSelfModelInFirstPerson;
    public float firstPersonCameraOffsetX;
    public float firstPersonCameraOffsetY;
    public float firstPersonCameraOffsetZ;

    public FBXPlayerModelsSave(){
        this.selectedSkin = new ClientSkin();
        this.isEnabled = true;
        this.renderSelfModelInFirstPerson = false;
        this.firstPersonCameraOffsetX = 0f;
        this.firstPersonCameraOffsetY = 0f;
        this.firstPersonCameraOffsetZ = 0f;
    }
}
