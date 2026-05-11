package io.gnupinguin.nevis.wealthtech.service.ai;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;

import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiProviderGuardTest {

    @Test
    void testCallReturnsSupplierResult() {
        var result = AiProviderGuard.call(AiProviderOperation.EMBEDDING_SEARCH, () -> "ok");

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void testCallKeepsExistingAiProviderException() {
        var exception = AiProviderGuard.invalidResponse(AiProviderOperation.SUMMARY_GENERATION, "blank response");

        assertThatThrownBy(() -> AiProviderGuard.call(AiProviderOperation.SUMMARY_GENERATION, () -> {
            throw exception;
        })).isSameAs(exception);
    }

    @Test
    void testCallClassifiesTransientProviderFailureAndSanitizesDetails() {
        assertThatThrownBy(() -> AiProviderGuard.call(
                AiProviderOperation.EMBEDDING_SEARCH,
                () -> {
                    throw new TransientAiException("rate limit for sk-proj-secret\nBearer abc.def");
                }
        )).isInstanceOfSatisfying(AiProviderException.class, exception -> {
            assertThat(exception.operation()).isEqualTo(AiProviderOperation.EMBEDDING_SEARCH);
            assertThat(exception.type()).isEqualTo(AiProviderErrorType.TRANSIENT);
            assertThat(exception.detail())
                    .contains("rate limit")
                    .contains("sk-proj-***")
                    .contains("Bearer ***")
                    .doesNotContain("secret")
                    .doesNotContain("abc.def")
                    .doesNotContain("\n");
        });
    }

    @Test
    void testCallClassifiesPermanentProviderFailure() {
        assertThatThrownBy(() -> AiProviderGuard.call(
                AiProviderOperation.SUMMARY_GENERATION,
                () -> {
                    throw new NonTransientAiException("invalid request");
                }
        )).isInstanceOfSatisfying(AiProviderException.class, exception -> {
            assertThat(exception.operation()).isEqualTo(AiProviderOperation.SUMMARY_GENERATION);
            assertThat(exception.type()).isEqualTo(AiProviderErrorType.PERMANENT);
            assertThat(exception.detail()).isEqualTo("invalid request");
        });
    }

    @Test
    void testCallClassifiesTimeoutCauseAsTransient() {
        assertThatThrownBy(() -> AiProviderGuard.call(
                AiProviderOperation.DOCUMENT_CHUNKING,
                () -> {
                    throw new RuntimeException("wrapped", new TimeoutException("slow provider"));
                }
        )).isInstanceOfSatisfying(AiProviderException.class, exception -> {
            assertThat(exception.operation()).isEqualTo(AiProviderOperation.DOCUMENT_CHUNKING);
            assertThat(exception.type()).isEqualTo(AiProviderErrorType.TRANSIENT);
            assertThat(exception.detail()).isEqualTo("slow provider");
        });
    }

    @Test
    void testCallClassifiesUnknownRuntimeFailure() {
        assertThatThrownBy(() -> AiProviderGuard.call(
                AiProviderOperation.EMBEDDING_SEARCH,
                () -> {
                    throw new IllegalStateException("unexpected provider failure");
                }
        )).isInstanceOfSatisfying(AiProviderException.class, exception -> {
            assertThat(exception.operation()).isEqualTo(AiProviderOperation.EMBEDDING_SEARCH);
            assertThat(exception.type()).isEqualTo(AiProviderErrorType.UNKNOWN);
            assertThat(exception.detail()).isEqualTo("unexpected provider failure");
        });
    }

    @Test
    void testInvalidResponseTruncatesDetails() {
        var exception = AiProviderGuard.invalidResponse(AiProviderOperation.SUMMARY_GENERATION, "x".repeat(300));

        assertThat(exception.type()).isEqualTo(AiProviderErrorType.INVALID_RESPONSE);
        assertThat(exception.detail()).hasSize(259).endsWith("...");
    }

    @Test
    void testRequireEmbeddingRejectsEmptyVector() {
        assertThatThrownBy(() -> AiProviderGuard.requireEmbedding(
                AiProviderOperation.EMBEDDING_SEARCH,
                new float[0],
                "empty search embedding"
        )).isInstanceOfSatisfying(AiProviderException.class, exception -> {
            assertThat(exception.operation()).isEqualTo(AiProviderOperation.EMBEDDING_SEARCH);
            assertThat(exception.type()).isEqualTo(AiProviderErrorType.INVALID_RESPONSE);
            assertThat(exception.detail()).isEqualTo("empty search embedding");
        });
    }

    @Test
    void testRequireEmbeddingsReturnsResults() {
        var embedding = new Embedding(new float[]{0.1f}, 0);
        var response = new EmbeddingResponse(List.of(embedding));

        var result = AiProviderGuard.requireEmbeddings(
                AiProviderOperation.DOCUMENT_CHUNKING,
                response,
                1,
                "invalid chunk embedding response"
        );

        assertThat(result).containsExactly(embedding);
    }

    @Test
    void testRequireEmbeddingsRejectsCountMismatch() {
        var response = new EmbeddingResponse(List.of(new Embedding(new float[]{0.1f}, 0)));

        assertThatThrownBy(() -> AiProviderGuard.requireEmbeddings(
                AiProviderOperation.DOCUMENT_CHUNKING,
                response,
                2,
                "invalid chunk embedding response"
        )).isInstanceOfSatisfying(AiProviderException.class, exception -> {
            assertThat(exception.type()).isEqualTo(AiProviderErrorType.INVALID_RESPONSE);
            assertThat(exception.detail()).isEqualTo("invalid chunk embedding response: expected 2 embeddings but received 1");
        });
    }

    @Test
    void testRequireEmbeddingIndexReturnsIndex() {
        var embedding = new Embedding(new float[]{0.1f}, 1);

        var index = AiProviderGuard.requireEmbeddingIndex(
                AiProviderOperation.DOCUMENT_CHUNKING,
                embedding,
                2,
                "invalid chunk embedding response"
        );

        assertThat(index).isEqualTo(1);
    }

    @Test
    void testRequireEmbeddingIndexRejectsInvalidIndex() {
        var embedding = new Embedding(new float[]{0.1f}, 3);

        assertThatThrownBy(() -> AiProviderGuard.requireEmbeddingIndex(
                AiProviderOperation.DOCUMENT_CHUNKING,
                embedding,
                2,
                "invalid chunk embedding response"
        )).isInstanceOfSatisfying(AiProviderException.class, exception -> {
            assertThat(exception.type()).isEqualTo(AiProviderErrorType.INVALID_RESPONSE);
            assertThat(exception.detail()).isEqualTo("invalid chunk embedding response: invalid embedding index 3");
        });
    }

}
