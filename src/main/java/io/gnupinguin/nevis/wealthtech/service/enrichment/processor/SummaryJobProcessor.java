package io.gnupinguin.nevis.wealthtech.service.enrichment.processor;

import io.gnupinguin.nevis.wealthtech.config.EnrichmentProperties;
import io.gnupinguin.nevis.wealthtech.persistence.entity.DocumentEntity;
import io.gnupinguin.nevis.wealthtech.persistence.entity.JobType;
import io.gnupinguin.nevis.wealthtech.persistence.repository.DocumentRepository;
import io.gnupinguin.nevis.wealthtech.service.enrichment.DocumentEnrichmentEvent;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
public class SummaryJobProcessor implements DocumentEnrichmentProcessor {

    private static final String SYSTEM_PROMPT = """
            You summarize client documents for a wealth management application.
            Write a concise, neutral summary in 2-4 sentences.
            Focus on investment goals, constraints, risks, timelines, and client preferences.
            Do not add facts or recommendations that are not present in the document.
            Return only the summary text.
            """;

    private static final String USER_PROMPT = """
            Document title:
            %s

            Document content:
            %s
            """;

    private final DocumentRepository documentRepository;
    private final ChatModel chatModel;
    private final EnrichmentProperties enrichmentProperties;

    public SummaryJobProcessor(@NonNull DocumentRepository documentRepository,
                               @NonNull ChatModel chatModel,
                               @NonNull EnrichmentProperties enrichmentProperties) {
        this.documentRepository = documentRepository;
        this.chatModel = chatModel;
        this.enrichmentProperties = enrichmentProperties;
    }

    @Override
    public @NonNull JobType type() {
        return JobType.SUMMARY;
    }

    @Override
    public void process(@NonNull DocumentEnrichmentEvent event) {
        log.info("Processing SUMMARY event {} for document {}", event.id(), event.documentId());
        var document = getDocument(event);
        var summary = generateSummary(document);
        saveSummary(document, summary);
        log.info("Stored summary for document {}", event.documentId());
    }

    private @NonNull DocumentEntity getDocument(@NonNull DocumentEnrichmentEvent event) {
        return documentRepository.findById(event.documentId())
                .orElseThrow(() -> new IllegalStateException("Document not found: " + event.documentId()));
    }

    private @NonNull String generateSummary(@NonNull DocumentEntity document) {
        var summaryProperties = enrichmentProperties.summary();
        var prompt = new Prompt(
                List.of(
                        new SystemMessage(SYSTEM_PROMPT),
                        new UserMessage(USER_PROMPT.formatted(document.title(), document.content()))
                ),
                OpenAiChatOptions.builder()
                        .model(summaryProperties.model())
                        .maxCompletionTokens(summaryProperties.maxCompletionTokens())
                        .build()
        );

        var response = chatModel.call(prompt);
        var summary = getSummary(response);
        if (!StringUtils.hasText(summary)) {
            throw new IllegalStateException("OpenAI returned an empty summary for document: " + document.id());
        }
        return summary.trim();
    }

    private static @Nullable String getSummary(ChatResponse response) {
        var generation = response.getResult();
        var output = generation == null ? null : generation.getOutput();
        return output == null ? null : output.getText();
    }

    private void saveSummary(@NonNull DocumentEntity document, @NonNull String summary) {
        documentRepository.save(new DocumentEntity(
                document.id(),
                document.clientId(),
                document.title(),
                document.content(),
                summary,
                document.createdAt(),
                Instant.now()
        ));
    }
}
