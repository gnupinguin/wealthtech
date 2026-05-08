package io.gnupinguin.nevis.wealthtech.service.client;

import io.gnupinguin.nevis.wealthtech.rest.dto.ClientResponse;
import io.gnupinguin.nevis.wealthtech.rest.dto.CreateClientRequest;
import org.jspecify.annotations.NonNull;

import java.util.Optional;
import java.util.UUID;

public interface ClientService {

    @NonNull
    ClientResponse createClient(@NonNull CreateClientRequest request);

    @NonNull
    Optional<ClientResponse> getClient(@NonNull UUID clientId);

}
