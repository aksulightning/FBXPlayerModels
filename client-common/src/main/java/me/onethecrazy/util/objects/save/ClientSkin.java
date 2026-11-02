package me.onethecrazy.util.objects.save;

import me.onethecrazy.util.parsing.ParsingFormat;
import me.onethecrazy.util.model.rig.LogicalRigBinding;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClientSkin {
    public String hash;
    public String name;
    public ParsingFormat format;
    public String sourceFormat;
    public float scale;
    public Boolean animationsEnabled;
    public LogicalRigBinding logicalRigBinding;
    public Map<String, String> animationClipMappings;
    public List<String> importWarnings;

    public ClientSkin(){
        this.hash = "";
        this.name = "";
        this.format = null;
        this.sourceFormat = "";
        this.scale = 1f;
        this.animationsEnabled = true;
        this.logicalRigBinding = new LogicalRigBinding();
        this.animationClipMappings = new LinkedHashMap<>();
        this.importWarnings = new ArrayList<>();
    }

    public ClientSkin(String hash, String name, @Nullable ParsingFormat format){
        this.hash = hash;
        this.name = name;
        this.format = format;
        this.sourceFormat = format == null ? "" : format.name();
        this.scale = 1f;
        this.animationsEnabled = true;
        this.logicalRigBinding = new LogicalRigBinding();
        this.animationClipMappings = new LinkedHashMap<>();
        this.importWarnings = new ArrayList<>();
    }

    public LogicalRigBinding binding() {
        if (logicalRigBinding == null) {
            logicalRigBinding = new LogicalRigBinding();
        }
        return logicalRigBinding;
    }

    public Map<String, String> clipMappings() {
        if (animationClipMappings == null) {
            animationClipMappings = new LinkedHashMap<>();
        }
        return animationClipMappings;
    }

    public List<String> warnings() {
        if (importWarnings == null) {
            importWarnings = new ArrayList<>();
        }
        return importWarnings;
    }

    public boolean animationsEnabled() {
        if (animationsEnabled == null) {
            animationsEnabled = true;
        }
        return animationsEnabled;
    }

    public void setAnimationsEnabled(boolean animationsEnabled) {
        this.animationsEnabled = animationsEnabled;
    }
}
