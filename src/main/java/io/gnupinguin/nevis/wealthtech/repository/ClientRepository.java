package io.gnupinguin.nevis.wealthtech.repository;

import io.gnupinguin.nevis.wealthtech.model.ClientScoreResult;
import io.gnupinguin.nevis.wealthtech.persistence.ClientEntity;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ClientRepository extends CrudRepository<ClientEntity, UUID> {

    @Query("""
            SELECT id,
                   GREATEST(
                       similarity(email, :query),
                       similarity(first_name, :query),
                       similarity(last_name, :query),
                       COALESCE(similarity(description, :query), 0.0)
                   ) AS score
            FROM clients
            WHERE email % :query OR first_name % :query OR last_name % :query OR description % :query
            ORDER BY score DESC
            LIMIT :limit
            """)
    List<ClientScoreResult> findMatching(@Param("query") String query, @Param("limit") int limit);
}
