package com.zyelectric.ocpp.dao;

import com.zyelectric.ocpp.model.IdTag;
import com.zyelectric.ocpp.repository.IdTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IdTagDao {

    private final IdTagRepository idTagRepository;

    public IdTag save(IdTag idTag) {
        return idTagRepository.save(idTag);
    }

    public List<IdTag> getAll() {
        return idTagRepository.findAll();
    }

    public Optional<IdTag> findById(Long id) {
        return idTagRepository.findById(id);
    }

    public Optional<IdTag> findByIdTag(String idTag) {
        return idTagRepository.findByIdTag(idTag);
    }

    public void delete(IdTag idTag) {
        idTagRepository.delete(idTag);
    }

    public void deleteById(Long id) {
        idTagRepository.deleteById(id);
    }
}
