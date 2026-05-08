package io.gnupinguin.nevis.wealthtech.service.enrichment.processor;

import io.gnupinguin.nevis.wealthtech.model.DocumentChunk;
import io.gnupinguin.nevis.wealthtech.persistence.DocumentEnrichmentJobEntity;
import io.gnupinguin.nevis.wealthtech.persistence.DocumentEntity;
import io.gnupinguin.nevis.wealthtech.persistence.JobType;
import io.gnupinguin.nevis.wealthtech.repository.DocumentChunkRepository;
import io.gnupinguin.nevis.wealthtech.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
public class ChunkingJobProcessor implements DocumentEnrichmentJobProcessor {

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
    public void process(@NonNull DocumentEnrichmentJobEntity job) {
        log.info("Processing CHUNKING job {} for document {}", job.id(), job.documentId());
        var texts = splitDocumentContent(job);

        EmbeddingResponse response = embeddingModel.embedForResponse(texts);
        var chunks = createChunks(job, response, texts);

        chunkRepository.saveAll(chunks);
        log.info("Stored {} chunks for document {}", texts.size(), job.documentId());
    }

    private @NonNull List<String> splitDocumentContent(@NonNull DocumentEnrichmentJobEntity job) {
        var document = getDocument(job);
        return splitter.apply(List.of(new Document(document.content())))
                .stream()
                .map(Document::getText)
                .toList();
    }

    private static @NonNull List<DocumentChunk> createChunks(@NonNull DocumentEnrichmentJobEntity job, @NonNull EmbeddingResponse response, @NonNull List<String> texts) {
        var now = Instant.now();
        return response.getResults().stream()
                .map(embedding -> new DocumentChunk(null, job.documentId(), embedding.getIndex(), texts.get(embedding.getIndex()), embedding.getOutput(), now))
                .toList();
    }

    private @NonNull DocumentEntity getDocument(@NonNull DocumentEnrichmentJobEntity job) {
        return documentRepository.findById(job.documentId())
                .orElseThrow(() -> new IllegalStateException("Document not found: " + job.documentId()));
    }

}
