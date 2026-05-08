package io.gnupinguin.nevis.wealthtech.service.enrichment;

import io.gnupinguin.nevis.wealthtech.persistence.JobType;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public interface JobService {

    void publishJob(@NonNull UUID documentId, @NonNull JobType type);

}
