package com.zyelectric.ocpp.service;

import com.zyelectric.ocpp.model.IdTag;
import com.zyelectric.ocpp.request.IdTagRequest;

import java.util.List;
import java.util.Optional;

public interface IdTagService {
    Optional<IdTag> getTagById(String idTag);

    String validateTag(String idTag);

    IdTag saveTag(IdTagRequest tag);

    void updateTag(IdTag tag);

    List<IdTag> getAllTags();

    void deleteTag(IdTag idTag);
}
