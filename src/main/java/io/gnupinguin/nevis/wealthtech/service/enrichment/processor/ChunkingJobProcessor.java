package io.gnupinguin.nevis.wealthtech.service.enrichment.processor;

import io.gnupinguin.nevis.wealthtech.persistence.entity.DocumentChunkEntity;
import io.gnupinguin.nevis.wealthtech.persistence.entity.DocumentEntity;
import io.gnupinguin.nevis.wealthtech.persistence.entity.JobType;
import io.gnupinguin.nevis.wealthtech.persistence.repository.DocumentChunkRepository;
import io.gnupinguin.nevis.wealthtech.persistence.repository.DocumentRepository;
import io.gnupinguin.nevis.wealthtech.service.ai.AiProviderGuard;
import io.gnupinguin.nevis.wealthtech.service.ai.AiProviderOperation;
import io.gnupinguin.nevis.wealthtech.service.enrichment.DocumentEnrichmentEvent;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
public class ChunkingJobProcessor implements DocumentEnrichmentProcessor {

    private final DocumentRepository documentRepository;
    private final EmbeddingModel embeddingModel;
    private final DocumentChunkRepository chunkRepository;
    private final TokenTextSplitter splitter;

    public ChunkingJobProcessor(DocumentRepository documentRepository,
                                EmbeddingModel embeddingModel,
                                DocumentChunkRepository chunkRepository) {
        this.documentRepository = documentRepository;
        this.embeddingModel = embeddingModel;
        this.chunkRepository = chunkRepository;
        this.splitter = TokenTextSplitter.builder().build();
    }

    @Override
    public @NonNull JobType type() {
        return JobType.CHUNKING;
    }

    @Override
    public void process(@NonNull DocumentEnrichmentEvent event) {
        log.info("Processing CHUNKING event {} for document {}", event.id(), event.documentId());
        var texts = splitDocumentContent(event);

        EmbeddingResponse response = AiProviderGuard.call(
                AiProviderOperation.DOCUMENT_CHUNKING,
                () -> embeddingModel.embedForResponse(texts)
        );
        var chunks = createChunks(event, response, texts);

        chunkRepository.saveAll(chunks);
        log.info("Stored {} chunks for document {}", texts.size(), event.documentId());
    }

    private @NonNull List<String> splitDocumentContent(@NonNull DocumentEnrichmentEvent event) {
        var document = getDocument(event);
        return splitter.apply(List.of(new Document(document.content())))
                .stream()
                .map(Document::getText)
                .toList();
    }

    private static @NonNull List<DocumentChunkEntity> createChunks(@NonNull DocumentEnrichmentEvent event,
                                                                   @Nullable EmbeddingResponse response,
                                                                   @NonNull List<String> texts) {
        var now = Instant.now();
        var embeddings = AiProviderGuard.requireEmbeddings(
                AiProviderOperation.DOCUMENT_CHUNKING,
                response,
                texts.size(),
                "invalid chunk embedding response for document: " + event.documentId()
        );
        return embeddings.stream()
                .map(embedding -> toChunk(event, texts, now, embedding))
                .toList();
    }

    private static @NonNull DocumentChunkEntity toChunk(@NonNull DocumentEnrichmentEvent event,
                                                        @NonNull List<String> texts,
                                                        @NonNull Instant now,
                                                        @Nullable Embedding embedding) {
        var detail = "invalid chunk embedding response for document: " + event.documentId();
        var embeddingResult = AiProviderGuard.requireEmbeddingResult(
                AiProviderOperation.DOCUMENT_CHUNKING,
                embedding,
                detail
        );
        var index = AiProviderGuard.requireEmbeddingIndex(
                AiProviderOperation.DOCUMENT_CHUNKING,
                embeddingResult,
                texts.size(),
                detail
        );
        var output = AiProviderGuard.requireEmbedding(
                AiProviderOperation.DOCUMENT_CHUNKING,
                embeddingResult.getOutput(),
                "empty chunk embedding for document: " + event.documentId() + ", chunk: " + index
        );
        return new DocumentChunkEntity(null, event.documentId(), index, texts.get(index), output, now);
    }

    private @NonNull DocumentEntity getDocument(@NonNull DocumentEnrichmentEvent event) {
        return documentRepository.findById(event.documentId())
                .orElseThrow(() -> new IllegalStateException("Document not found: " + event.documentId()));
    }

}
