package com.zyelectric.ocpp.service;

import com.zyelectric.ocpp.dao.IdTagDao;
import com.zyelectric.ocpp.model.IdTag;
import com.zyelectric.ocpp.request.IdTagRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdTagServiceImpl implements IdTagService {

    private final IdTagDao idTagDao;

    @Override
    public IdTag saveTag(IdTagRequest request) {
        Optional<IdTag> idTag = getTagById(request.getIdTag());
        if (idTag.isPresent()) {
            throw new IllegalArgumentException("Tag Already Exists");
        }
        if (request.getExpiryDate() == null) {
            throw new IllegalArgumentException("Expiry date is required");
        }

        IdTag tag = new IdTag();
        tag.setIdTag(request.getIdTag());
        tag.setParentIdTag(request.getParentIdTag());
        tag.setExpiryDate(request.getExpiryDate());
        tag.setMaxActiveTransactionCount(request.getMaxActiveTransactionCount());
        tag.setNote(request.getNote());
        tag.setActiveTransactionCount(request.getActiveTransactionCount());
        tag.setInTransaction(request.getInTransaction());
        tag.setBlocked(request.getBlocked());

        return idTagDao.save(tag);
    }

    @Override
    public void updateTag(IdTag tag) {
        idTagDao.save(tag);
    }

    @Override
    public List<IdTag> getAllTags() {
        return idTagDao.getAll();
    }

    @Override
    public void deleteTag(IdTag tag) {
        idTagDao.delete(tag);
        log.info("Deleted ID Tag: {}", tag.getIdTag());
    }

    @Override
    public Optional<IdTag> getTagById(String idTag) {
        return idTagDao.findByIdTag(idTag);
    }

    @Override
    public String validateTag(String idTag) {
        Optional<IdTag> tagOpt = getTagById(idTag);

        if (tagOpt.isEmpty()) {
            log.warn("Invalid ID Tag: {}", idTag);
            return "Invalid";
        }

        IdTag tag = tagOpt.get();

        boolean isExpired = tag.getExpiryDate() < System.currentTimeMillis();
        int maxTx = Optional.ofNullable(tag.getMaxActiveTransactionCount()).orElse(-1);
        int currentTx = Optional.ofNullable(tag.getActiveTransactionCount()).orElse(0);

        if (Boolean.TRUE.equals(tag.getBlocked()) || maxTx == 0) {
            log.info("ID Tag is blocked or maxTx = 0 → Blocked: {}", idTag);
            return "Blocked";
        } else if (isExpired) {
            log.info("ID Tag expired: {}", idTag);
            return "Expired";
        } else if (maxTx > 0 && currentTx >= maxTx) {
            log.info("Max transaction limit reached for ID Tag: {}", idTag);
            return "Blocked";
        }
        log.info("ID Tag accepted: {}", idTag);
        return "Accepted";
    }


}
