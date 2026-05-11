package io.gnupinguin.nevis.wealthtech.service.ai;

import org.jspecify.annotations.NonNull;

public class AiProviderException extends RuntimeException {

    private final AiProviderOperation operation;
    private final AiProviderErrorType type;
    private final String detail;

    public AiProviderException(@NonNull AiProviderOperation operation,
                               @NonNull AiProviderErrorType type,
                               @NonNull String detail) {
        super(message(operation, type, detail));
        this.operation = operation;
        this.type = type;
        this.detail = detail;
    }

    public AiProviderException(@NonNull AiProviderOperation operation,
                               @NonNull AiProviderErrorType type,
                               @NonNull String detail,
                               @NonNull Throwable cause) {
        super(message(operation, type, detail), cause);
        this.operation = operation;
        this.type = type;
        this.detail = detail;
    }

    public @NonNull AiProviderOperation operation() {
        return operation;
    }

    public @NonNull AiProviderErrorType type() {
        return type;
    }

    public @NonNull String detail() {
        return detail;
    }

    private static @NonNull String message(@NonNull AiProviderOperation operation,
                                           @NonNull AiProviderErrorType type,
                                           @NonNull String detail) {
        return "AI provider " + operation.label() + " failed (" + type.name().toLowerCase() + "): " + detail;
    }
}
