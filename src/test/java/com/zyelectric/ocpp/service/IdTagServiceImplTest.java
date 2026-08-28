package com.zyelectric.ocpp.service;

import com.zyelectric.ocpp.model.IdTag;
import com.zyelectric.ocpp.repository.IdTagRepository;
import com.zyelectric.ocpp.request.IdTagRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdTagServiceImplTest {

    @Mock
    private IdTagRepository idTagRepository;

    @InjectMocks
    private IdTagServiceImpl idTagService;

    @Test
    void saveTag_succeeds() {
        IdTagRequest request = new IdTagRequest("TAG001", null, System.currentTimeMillis() + 100_000, 1, null, 0, false, false);
        when(idTagRepository.findByIdTag("TAG001")).thenReturn(Optional.empty());
        when(idTagRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IdTag saved = idTagService.saveTag(request);

        assertThat(saved.getIdTag()).isEqualTo("TAG001");
        verify(idTagRepository).save(any());
    }

    @Test
    void saveTag_rejectsDuplicateIdTag() {
        IdTagRequest request = new IdTagRequest("TAG001", null, System.currentTimeMillis() + 100_000, 1, null, 0, false, false);
        when(idTagRepository.findByIdTag("TAG001")).thenReturn(Optional.of(new IdTag()));

        assertThatThrownBy(() -> idTagService.saveTag(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Already Exists");

        verify(idTagRepository, never()).save(any());
    }

    @Test
    void saveTag_missingExpiryDate_rejected() {
        IdTagRequest request = new IdTagRequest("TAG001", null, null, 1, null, 0, false, false);
        when(idTagRepository.findByIdTag("TAG001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> idTagService.saveTag(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expiry date");
    }

    @Test
    void getAllTags_delegatesToRepository() {
        when(idTagRepository.findAll()).thenReturn(List.of(new IdTag()));

        assertThat(idTagService.getAllTags()).hasSize(1);
    }

    @Test
    void deleteTag_delegatesToRepository() {
        IdTag tag = new IdTag();
        tag.setIdTag("TAG001");

        idTagService.deleteTag(tag);

        verify(idTagRepository).delete(tag);
    }

    @Test
    void validateTag_unknownTag_returnsInvalid() {
        when(idTagRepository.findByIdTag("UNKNOWN")).thenReturn(Optional.empty());

        assertThat(idTagService.validateTag("UNKNOWN")).isEqualTo("Invalid");
    }

    @Test
    void validateTag_blockedTag_returnsBlocked() {
        IdTag tag = new IdTag();
        tag.setIdTag("TAG001");
        tag.setBlocked(true);
        tag.setExpiryDate(System.currentTimeMillis() + 100_000);
        when(idTagRepository.findByIdTag("TAG001")).thenReturn(Optional.of(tag));

        assertThat(idTagService.validateTag("TAG001")).isEqualTo("Blocked");
    }

    @Test
    void validateTag_expiredTag_returnsExpired() {
        IdTag tag = new IdTag();
        tag.setIdTag("TAG001");
        tag.setBlocked(false);
        tag.setExpiryDate(System.currentTimeMillis() - 1);
        when(idTagRepository.findByIdTag("TAG001")).thenReturn(Optional.of(tag));

        assertThat(idTagService.validateTag("TAG001")).isEqualTo("Expired");
    }

    @Test
    void validateTag_underLimit_returnsAccepted() {
        IdTag tag = new IdTag();
        tag.setIdTag("TAG001");
        tag.setBlocked(false);
        tag.setExpiryDate(System.currentTimeMillis() + 100_000);
        tag.setMaxActiveTransactionCount(2);
        tag.setActiveTransactionCount(1);
        when(idTagRepository.findByIdTag("TAG001")).thenReturn(Optional.of(tag));

        assertThat(idTagService.validateTag("TAG001")).isEqualTo("Accepted");
    }
}
