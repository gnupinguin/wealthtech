package io.gnupinguin.nevis.wealthtech.service.access;

import io.gnupinguin.nevis.wealthtech.model.Client;
import io.gnupinguin.nevis.wealthtech.rest.model.CreateClientRequest;
import org.jspecify.annotations.NonNull;

import java.util.Optional;
import java.util.UUID;

public interface ClientService {

    @NonNull
    Client createClient(@NonNull CreateClientRequest request);

    @NonNull
    Optional<Client> getClient(@NonNull UUID clientId);

}
