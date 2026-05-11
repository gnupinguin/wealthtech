package io.gnupinguin.nevis.wealthtech.service.search.document;

import io.gnupinguin.nevis.wealthtech.exception.ServiceUnavailableException;
import io.gnupinguin.nevis.wealthtech.persistence.projection.DocumentSearchProjection;
import io.gnupinguin.nevis.wealthtech.persistence.repository.DocumentChunkSearchRepository;
import io.gnupinguin.nevis.wealthtech.persistence.repository.SqlQueryHelper;
import io.gnupinguin.nevis.wealthtech.service.ai.AiProviderErrorType;
import io.gnupinguin.nevis.wealthtech.service.ai.AiProviderException;
import io.gnupinguin.nevis.wealthtech.service.ai.AiProviderOperation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.retry.TransientAiException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SemanticDocumentSearchServiceTest {

    @Mock
    private DocumentChunkSearchRepository repository;

    @Mock
    private EmbeddingModel embeddingModel;

    @InjectMocks
    private SemanticDocumentSearchService searchService;

    @Test
    void testSearchEmbedsQueryAndDelegatesToRepository() {
        var embedding = new float[]{0.1f, 0.2f, 0.3f};
        var vectorString = SqlQueryHelper.toVectorString(embedding);
        when(embeddingModel.embed("growth")).thenReturn(embedding);
        when(repository.findSimilar(vectorString, 5)).thenReturn(List.of());

        var result = searchService.search("growth", 5);

        assertThat(result).isEmpty();
        verify(embeddingModel).embed("growth");
        verify(repository).findSimilar(vectorString, 5);
    }

    @Test
    void testSearchMapsProjectionsToDocumentSearchResults() {
        var documentId = UUID.randomUUID();
        var clientId = UUID.randomUUID();
        var embedding = new float[]{0.5f};
        var vectorString = SqlQueryHelper.toVectorString(embedding);
        var projection = new DocumentSearchProjection(documentId, clientId, "Portfolio Q1", "Growth allocation", "Summary text", Instant.EPOCH, 0.88f);
        when(embeddingModel.embed("growth")).thenReturn(embedding);
        when(repository.findSimilar(vectorString, 3)).thenReturn(List.of(projection));

        var result = searchService.search("growth", 3);

        assertThat(result).singleElement()
                .satisfies(doc -> {
                    assertThat(doc.id()).isEqualTo(documentId);
                    assertThat(doc.clientId()).isEqualTo(clientId);
                    assertThat(doc.title()).isEqualTo("Portfolio Q1");
                    assertThat(doc.matchedChunk()).isEqualTo("Growth allocation");
                    assertThat(doc.summary()).isEqualTo("Summary text");
                    assertThat(doc.score()).isEqualTo(0.88f);
                });
    }

    @Test
    void testSearchPassesLimitToRepository() {
        var embedding = new float[]{0.1f};
        var vectorString = SqlQueryHelper.toVectorString(embedding);
        when(embeddingModel.embed("tech")).thenReturn(embedding);
        when(repository.findSimilar(vectorString, 20)).thenReturn(List.of());

        searchService.search("tech", 20);

        verify(repository).findSimilar(vectorString, 20);
    }

    @Test
    void testSearchWrapsEmbeddingProviderFailure() {
        when(embeddingModel.embed("growth")).thenThrow(new TransientAiException("rate limit for key sk-proj-secret"));

        assertThatThrownBy(() -> searchService.search("growth", 5))
                .isInstanceOfSatisfying(ServiceUnavailableException.class, exception -> {
                    assertThat(exception).hasMessage("Third party service is unavailable");
                    assertThat(exception.getCause()).isInstanceOfSatisfying(AiProviderException.class, cause -> {
                        assertThat(cause.operation()).isEqualTo(AiProviderOperation.EMBEDDING_SEARCH);
                        assertThat(cause.type()).isEqualTo(AiProviderErrorType.TRANSIENT);
                        assertThat(cause.detail()).contains("rate limit").doesNotContain("secret");
                    });
                });

        verifyNoInteractions(repository);
    }

    @Test
    void testSearchRejectsEmptyEmbeddingResponse() {
        when(embeddingModel.embed("growth")).thenReturn(new float[0]);

        assertThatThrownBy(() -> searchService.search("growth", 5))
                .isInstanceOfSatisfying(ServiceUnavailableException.class, exception -> {
                    assertThat(exception).hasMessage("Third party service is unavailable");
                    assertThat(exception.getCause()).isInstanceOfSatisfying(AiProviderException.class, cause -> {
                        assertThat(cause.operation()).isEqualTo(AiProviderOperation.EMBEDDING_SEARCH);
                        assertThat(cause.type()).isEqualTo(AiProviderErrorType.INVALID_RESPONSE);
                        assertThat(cause.detail()).isEqualTo("empty search embedding");
                    });
                });

        verifyNoInteractions(repository);
    }

}
