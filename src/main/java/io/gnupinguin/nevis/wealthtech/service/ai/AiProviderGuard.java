package io.gnupinguin.nevis.wealthtech.service.ai;

import lombok.experimental.UtilityClass;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@UtilityClass
public final class AiProviderGuard {

    private static final int MAX_DETAIL_LENGTH = 256;

    public static <T> T call(@NonNull AiProviderOperation operation, @NonNull Supplier<T> call) {
        try {
            return call.get();
        } catch (AiProviderException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new AiProviderException(operation, classify(e), detail(e), e);
        }
    }

    public static @NonNull AiProviderException invalidResponse(@NonNull AiProviderOperation operation, @NonNull String detail) {
        return new AiProviderException(operation, AiProviderErrorType.INVALID_RESPONSE, sanitize(detail));
    }

    public static float[] requireEmbedding(@NonNull AiProviderOperation operation,
                                           float[] embedding,
                                           @NonNull String detail) {
        if (embedding == null || embedding.length == 0) {
            throw invalidResponse(operation, detail);
        }
        return embedding;
    }

    public static @NonNull List<Embedding> requireEmbeddings(@NonNull AiProviderOperation operation,
                                                             @Nullable EmbeddingResponse response,
                                                             int expectedCount,
                                                             @NonNull String detail) {
        if (response == null) {
            throw invalidResponse(operation, detail + ": missing embedding response");
        }
        var results = response.getResults();
        if (results.size() != expectedCount) {
            throw invalidResponse(operation, detail + ": expected " + expectedCount + " embeddings but received " + results.size());
        }
        return results;
    }

    public static @NonNull Embedding requireEmbeddingResult(@NonNull AiProviderOperation operation,
                                                            @Nullable Embedding embedding,
                                                            @NonNull String detail) {
        if (embedding == null) {
            throw invalidResponse(operation, detail + ": missing embedding result");
        }
        return embedding;
    }

    public static int requireEmbeddingIndex(@NonNull AiProviderOperation operation,
                                            @NonNull Embedding embedding,
                                            int expectedCount,
                                            @NonNull String detail) {
        int index = embedding.getIndex();
        if (index < 0 || index >= expectedCount) {
            throw invalidResponse(operation, detail + ": invalid embedding index " + index);
        }
        return index;
    }

    private static @NonNull AiProviderErrorType classify(@NonNull Throwable e) {
        if (hasCause(e, TransientAiException.class) || hasCause(e, TimeoutException.class) || hasCause(e, SocketTimeoutException.class)) {
            return AiProviderErrorType.TRANSIENT;
        }
        if (hasCause(e, NonTransientAiException.class)) {
            return AiProviderErrorType.PERMANENT;
        }
        return AiProviderErrorType.UNKNOWN;
    }

    private static boolean hasCause(@NonNull Throwable e, @NonNull Class<? extends Throwable> type) {
        Throwable current = e;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static @NonNull String detail(@NonNull Throwable e) {
        var cause = rootCause(e);
        var message = cause.getMessage();
        if (message == null || message.isBlank()) {
            message = cause.getClass().getSimpleName();
        }
        return sanitize(message);
    }

    private static @NonNull Throwable rootCause(@NonNull Throwable e) {
        Throwable current = e;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static @NonNull String sanitize(@NonNull String message) {
        var sanitized = message
                .replaceAll("sk-proj-[A-Za-z0-9_-]+", "sk-proj-***")
                .replaceAll("sk-(?!proj-)[A-Za-z0-9_-]+", "sk-***")
                .replaceAll("(?i)bearer\\s+[A-Za-z0-9._-]+", "Bearer ***")
                .replaceAll("[\\r\\n\\t]+", " ")
                .trim();
        if (sanitized.isEmpty()) {
            return "No provider detail";
        }
        if (sanitized.length() <= MAX_DETAIL_LENGTH) {
            return sanitized;
        }
        return sanitized.substring(0, MAX_DETAIL_LENGTH) + "...";
    }
}
