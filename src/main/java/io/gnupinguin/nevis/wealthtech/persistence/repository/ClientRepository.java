package io.gnupinguin.nevis.wealthtech.persistence.repository;

import io.gnupinguin.nevis.wealthtech.persistence.entity.ClientEntity;
import io.gnupinguin.nevis.wealthtech.persistence.projection.ClientSearchProjection;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ClientRepository extends CrudRepository<ClientEntity, UUID> {

    @Query("""
            SELECT
                id,
                GREATEST(
                    CASE WHEN email ILIKE :prefix ESCAPE '\\' THEN 2.0 ELSE 0.0 END,
                    CASE WHEN first_name ILIKE :prefix ESCAPE '\\' THEN 1.5 ELSE 0.0 END,
                    CASE WHEN last_name ILIKE :prefix ESCAPE '\\' THEN 1.5 ELSE 0.0 END,
            
                    similarity(email, :query) * 1.8,
                    similarity(first_name, :query) * 1.2,
                    similarity(last_name, :query) * 1.2,
                    similarity(COALESCE(description, ''), :query) * 0.4
                ) AS score
            FROM clients
            WHERE email ILIKE :prefix ESCAPE '\\'
               OR first_name ILIKE :prefix ESCAPE '\\'
               OR last_name ILIKE :prefix ESCAPE '\\'
            
               OR email % :query
               OR first_name % :query
               OR last_name % :query
               OR COALESCE(description, '') % :query
            
            ORDER BY score DESC, id
            LIMIT :limit;
            """)
    List<ClientSearchProjection> findMatching(@Param("query") String query, @Param("prefix") String prefix, @Param("limit") int limit);
}
