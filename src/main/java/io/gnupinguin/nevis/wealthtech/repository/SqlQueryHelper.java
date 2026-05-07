package io.gnupinguin.nevis.wealthtech.repository;

import lombok.experimental.UtilityClass;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@UtilityClass
public class SqlQueryHelper {

    @NonNull
    public static String toVectorString(float[] embedding) {
        if (embedding != null) {
            return Arrays.toString(embedding);
        }
        return "[]";
    }

}
