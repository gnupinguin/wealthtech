package io.gnupinguin.nevis.wealthtech.service.enrichment.processor;

import io.gnupinguin.nevis.wealthtech.config.EnrichmentProperties;
import io.gnupinguin.nevis.wealthtech.persistence.entity.DocumentEntity;
import io.gnupinguin.nevis.wealthtech.persistence.entity.JobType;
import io.gnupinguin.nevis.wealthtech.persistence.repository.DocumentRepository;
import io.gnupinguin.nevis.wealthtech.service.enrichment.DocumentEnrichmentEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SummaryJobProcessorTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ChatModel chatModel;

    private SummaryJobProcessor processor;

    @BeforeEach
    void setUp() {
        var properties = new EnrichmentProperties(
                null,
                null,
                new EnrichmentProperties.Summary("gpt-5-nano", 300)
        );
        processor = new SummaryJobProcessor(documentRepository, chatModel, properties);
    }

    @Test
    void testProcessGeneratesSummaryWithGpt5NanoAndPersistsIt() {
        var documentId = UUID.randomUUID();
        var document = document(documentId);
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse(" Client wants moderate growth. "));

        processor.process(event(documentId));

        var promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        var prompt = promptCaptor.getValue();
        assertThat(prompt.getContents())
                .contains("Portfolio Policy")
                .contains("Client prefers a moderate growth allocation.");
        assertThat(prompt.getOptions()).isInstanceOfSatisfying(OpenAiChatOptions.class, options -> {
            assertThat(options.getModel()).isEqualTo("gpt-5-nano");
            assertThat(options.getMaxCompletionTokens()).isEqualTo(300);
        });

        var documentCaptor = ArgumentCaptor.forClass(DocumentEntity.class);
        verify(documentRepository).save(documentCaptor.capture());
        var saved = documentCaptor.getValue();
        assertThat(saved.id()).isEqualTo(documentId);
        assertThat(saved.clientId()).isEqualTo(document.clientId());
        assertThat(saved.title()).isEqualTo(document.title());
        assertThat(saved.content()).isEqualTo(document.content());
        assertThat(saved.summary()).isEqualTo("Client wants moderate growth.");
        assertThat(saved.createdAt()).isEqualTo(document.createdAt());
        assertThat(saved.updatedAt()).isAfter(document.updatedAt());
    }

    @Test
    void testProcessThrowsWhenDocumentDoesNotExist() {
        var documentId = UUID.randomUUID();
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processor.process(event(documentId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Document not found: " + documentId);

        verifyNoInteractions(chatModel);
        verify(documentRepository, never()).save(any());
    }

    @Test
    void testProcessThrowsWhenOpenAiReturnsBlankSummary() {
        var documentId = UUID.randomUUID();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document(documentId)));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse(" "));

        assertThatThrownBy(() -> processor.process(event(documentId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OpenAI returned an empty summary for document: " + documentId);

        verify(documentRepository, never()).save(any());
    }

    private static DocumentEntity document(UUID documentId) {
        return new DocumentEntity(
                documentId,
                UUID.randomUUID(),
                "Portfolio Policy",
                "Client prefers a moderate growth allocation.",
                null,
                Instant.EPOCH,
                Instant.EPOCH
        );
    }

    private static DocumentEnrichmentEvent event(UUID documentId) {
        return new DocumentEnrichmentEvent(UUID.randomUUID(), documentId, JobType.SUMMARY, Instant.now());
    }

    private static ChatResponse chatResponse(String summary) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(summary))));
    }

}
