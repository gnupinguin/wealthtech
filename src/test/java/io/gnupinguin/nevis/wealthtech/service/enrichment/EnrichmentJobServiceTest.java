package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.gnupinguin.nevis.wealthtech.persistence.entity.DocumentEnrichmentJobEntity;
import io.gnupinguin.nevis.wealthtech.persistence.entity.JobStatus;
import io.gnupinguin.nevis.wealthtech.persistence.entity.JobType;
import io.gnupinguin.nevis.wealthtech.persistence.repository.DocumentEnrichmentJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrichmentJobServiceTest {

    @Mock
    private DocumentEnrichmentJobRepository repository;

    @InjectMocks
    private EnrichmentJobService jobService;

    @Test
    void testPublishJobSavesJobWithPendingStatusAndNullId() {
        var documentId = UUID.randomUUID();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        jobService.publishJob(documentId, JobType.CHUNKING);

        var captor = ArgumentCaptor.forClass(DocumentEnrichmentJobEntity.class);
        verify(repository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.id()).isNull();
        assertThat(saved.documentId()).isEqualTo(documentId);
        assertThat(saved.type()).isEqualTo(JobType.CHUNKING);
        assertThat(saved.status()).isEqualTo(JobStatus.PENDING);
        assertThat(saved.attempts()).isZero();
        assertThat(saved.maxAttempts()).isEqualTo(3);
        assertThat(saved.lastError()).isNull();
        assertThat(saved.lockedAt()).isNull();
        assertThat(saved.completedAt()).isNull();
    }

    @Test
    void testPublishJobSetsAvailableAtToCurrentTime() {
        var documentId = UUID.randomUUID();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        jobService.publishJob(documentId, JobType.SUMMARY);

        var captor = ArgumentCaptor.forClass(DocumentEnrichmentJobEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().availableAt()).isNotNull();
    }

    @Test
    void testPublishJobSavesCorrectJobTypeForSummary() {
        var documentId = UUID.randomUUID();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        jobService.publishJob(documentId, JobType.SUMMARY);

        var captor = ArgumentCaptor.forClass(DocumentEnrichmentJobEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(JobType.SUMMARY);
    }

}
