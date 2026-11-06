package com.aksulightning.fbxplayermodels;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;

public final class ViewEntityModelPath {
    private static final int MAX_MODEL_LENGTH = 128;
    private static final Pattern SAFE_FILE_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._ -]{0,127}\\.fbx", Pattern.CASE_INSENSITIVE);

    private ViewEntityModelPath() {
    }

    public static String safeModelOrEmpty(String rawModel) {
        if (rawModel == null) {
            return "";
        }

        String model = rawModel.trim();
        if (model.isBlank() || model.length() > MAX_MODEL_LENGTH || !model.toLowerCase(Locale.ROOT).endsWith(".fbx")) {
            return "";
        }
        if (!SAFE_FILE_NAME.matcher(model).matches()) {
            return "";
        }

        try {
            Path path = Path.of(model);
            if (path.isAbsolute() || path.getNameCount() != 1 || !path.getFileName().toString().equals(model)) {
                return "";
            }
        } catch (InvalidPathException e) {
            return "";
        }

        return model;
    }
}
