package com.aksulightning.fbxplayermodels.forge1122.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable collection factories used by the Java 8 model pipeline. */
public final class Java8 {
    private Java8() {}

    @SafeVarargs
    public static <T> List<T> list(T... elements) {
        return Collections.unmodifiableList(Arrays.asList(elements.clone()));
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> map(Object... pairs) {
        if ((pairs.length & 1) != 0) {
            throw new IllegalArgumentException("Expected key/value pairs");
        }
        Map<K, V> values = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            values.put((K) pairs[i], (V) pairs[i + 1]);
        }
        return Collections.unmodifiableMap(values);
    }
}
