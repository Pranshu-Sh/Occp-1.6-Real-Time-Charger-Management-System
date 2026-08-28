package com.zyelectric.ocpp.repository;

import com.zyelectric.ocpp.model.IdTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdTagRepository extends JpaRepository<IdTag, Long> {

    Optional<IdTag> findByIdTag(String idTag);

    /**
     * Atomically increments activeTransactionCount only if the tag isn't already at its
     * configured limit, avoiding the read-modify-write race a plain get+set has under
     * concurrent/duplicate StartTransaction requests. A null maxActiveTransactionCount
     * means "unlimited" (matches IdTagServiceImpl#validateTag's semantics).
     *
     * @return number of rows updated: 1 if allowed, 0 if the tag is at/over its limit or unknown.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
                UPDATE IdTag t
                SET t.activeTransactionCount = COALESCE(t.activeTransactionCount, 0) + 1, t.inTransaction = true
                WHERE t.idTag = :idTag
                  AND (COALESCE(t.maxActiveTransactionCount, -1) < 0
                       OR COALESCE(t.activeTransactionCount, 0) < t.maxActiveTransactionCount)
            """)
    int incrementActiveTransactionCountIfAllowed(@Param("idTag") String idTag);
}
