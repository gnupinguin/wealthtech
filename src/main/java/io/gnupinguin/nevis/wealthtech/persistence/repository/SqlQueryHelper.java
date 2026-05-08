package io.gnupinguin.nevis.wealthtech.persistence.repository;

import lombok.experimental.UtilityClass;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;

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
