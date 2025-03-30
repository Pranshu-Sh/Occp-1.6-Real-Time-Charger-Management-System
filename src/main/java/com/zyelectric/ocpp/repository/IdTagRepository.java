package com.zyelectric.ocpp.repository;

import com.zyelectric.ocpp.model.IdTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdTagRepository extends JpaRepository<IdTag, Long> {

    Optional<IdTag> findByIdTag(String idTag);
}
