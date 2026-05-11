package io.gnupinguin.nevis.wealthtech.service.enrichment.processor;

import io.gnupinguin.nevis.wealthtech.persistence.entity.DocumentChunkEntity;
import io.gnupinguin.nevis.wealthtech.persistence.entity.DocumentEntity;
import io.gnupinguin.nevis.wealthtech.persistence.entity.JobType;
import io.gnupinguin.nevis.wealthtech.persistence.repository.DocumentChunkRepository;
import io.gnupinguin.nevis.wealthtech.persistence.repository.DocumentRepository;
import io.gnupinguin.nevis.wealthtech.service.ai.AiProviderErrorType;
import io.gnupinguin.nevis.wealthtech.service.ai.AiProviderException;
import io.gnupinguin.nevis.wealthtech.service.ai.AiProviderOperation;
import io.gnupinguin.nevis.wealthtech.service.enrichment.DocumentEnrichmentEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.retry.TransientAiException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChunkingJobProcessorTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private DocumentChunkRepository chunkRepository;

    private ChunkingJobProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ChunkingJobProcessor(documentRepository, embeddingModel, chunkRepository);
    }

    @Test
    void testProcessSplitsEmbedsAndSavesChunks() {
        var documentId = UUID.randomUUID();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document(documentId)));
        when(embeddingModel.embedForResponse(anyList())).thenAnswer(invocation -> {
            List<String> texts = invocation.getArgument(0);
            return new EmbeddingResponse(IntStream.range(0, texts.size())
                    .mapToObj(index -> new Embedding(new float[]{index + 0.1f}, index))
                    .toList());
        });

        processor.process(event(documentId));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DocumentChunkEntity>> chunksCaptor = ArgumentCaptor.forClass(List.class);
        verify(chunkRepository).saveAll(chunksCaptor.capture());
        List<DocumentChunkEntity> chunks = chunksCaptor.getValue();
        assertThat(chunks).isNotEmpty();
        assertThat(chunks)
                .allSatisfy(chunk -> {
                    assertThat(chunk.documentId()).isEqualTo(documentId);
                    assertThat(chunk.content()).isNotBlank();
                    assertThat(chunk.embedding()).isNotEmpty();
                });
    }

    @Test
    void testProcessRejectsEmbeddingCountMismatch() {
        var documentId = UUID.randomUUID();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document(documentId)));
        when(embeddingModel.embedForResponse(anyList())).thenReturn(new EmbeddingResponse(List.of()));

        assertThatThrownBy(() -> processor.process(event(documentId)))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.operation()).isEqualTo(AiProviderOperation.DOCUMENT_CHUNKING);
                    assertThat(exception.type()).isEqualTo(AiProviderErrorType.INVALID_RESPONSE);
                    assertThat(exception.detail()).contains("expected").contains("embeddings");
                });

        verify(chunkRepository, never()).saveAll(anyList());
    }

    @Test
    void testProcessRejectsInvalidEmbeddingIndex() {
        var documentId = UUID.randomUUID();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document(documentId)));
        when(embeddingModel.embedForResponse(anyList()))
                .thenReturn(new EmbeddingResponse(List.of(new Embedding(new float[]{0.1f}, 99))));

        assertThatThrownBy(() -> processor.process(event(documentId)))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.operation()).isEqualTo(AiProviderOperation.DOCUMENT_CHUNKING);
                    assertThat(exception.type()).isEqualTo(AiProviderErrorType.INVALID_RESPONSE);
                    assertThat(exception.detail()).contains("invalid embedding index 99");
                });

        verify(chunkRepository, never()).saveAll(anyList());
    }

    @Test
    void testProcessWrapsEmbeddingProviderFailure() {
        var documentId = UUID.randomUUID();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document(documentId)));
        when(embeddingModel.embedForResponse(anyList())).thenThrow(new TransientAiException("timeout from provider"));

        assertThatThrownBy(() -> processor.process(event(documentId)))
                .isInstanceOfSatisfying(AiProviderException.class, exception -> {
                    assertThat(exception.operation()).isEqualTo(AiProviderOperation.DOCUMENT_CHUNKING);
                    assertThat(exception.type()).isEqualTo(AiProviderErrorType.TRANSIENT);
                    assertThat(exception.detail()).contains("timeout from provider");
                });

        verify(chunkRepository, never()).saveAll(anyList());
    }

    private static DocumentEntity document(UUID documentId) {
        return new DocumentEntity(
                documentId,
                UUID.randomUUID(),
                "Proof of Address",
                "Client provided a utility bill as proof of address for onboarding.",
                null,
                Instant.EPOCH,
                Instant.EPOCH
        );
    }

    private static DocumentEnrichmentEvent event(UUID documentId) {
        return new DocumentEnrichmentEvent(UUID.randomUUID(), documentId, JobType.CHUNKING, Instant.now());
    }

}
