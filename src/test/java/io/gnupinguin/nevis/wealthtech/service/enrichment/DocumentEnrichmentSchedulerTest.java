package io.gnupinguin.nevis.wealthtech.service.enrichment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DocumentEnrichmentSchedulerTest {

    @Mock
    private DocumentEnrichmentJobDispatcher jobDispatcher;

    @Test
    void testProcessNextJobDispatchesPendingJobs() {
        var scheduler = new DocumentEnrichmentScheduler(jobDispatcher);

        scheduler.processNextJob();

        verify(jobDispatcher).dispatchNextJobs();
    }

}
