package com.zyelectric.ocpp.repository;

import com.zyelectric.ocpp.model.IdTag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies IdTagRepository#incrementActiveTransactionCountIfAllowed's conditional UPDATE at
 * the real SQL level (independent of the mocked service-layer tests), since this atomic query
 * is what actually closes the concurrent-StartTransaction race described in the plan.
 */
@DataJpaTest
// @DataJpaTest replaces the datasource with its own auto-configured embedded DB by default,
// which would ignore application-test.yml's NON_KEYWORDS=VALUE H2 setting (needed because
// MeterValue.value, a legitimate MySQL column name, is a reserved word in H2) and break
// schema creation for the whole entity model, not just this test's table.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class IdTagRepositoryIntegrationTest {

    @Autowired
    private IdTagRepository idTagRepository;

    private void seedTag(String id, Integer max, Integer active) {
        IdTag tag = new IdTag();
        tag.setIdTag(id);
        tag.setExpiryDate(System.currentTimeMillis() + 100_000);
        tag.setMaxActiveTransactionCount(max);
        tag.setActiveTransactionCount(active);
        tag.setBlocked(false);
        idTagRepository.saveAndFlush(tag);
    }

    @Test
    void underLimit_incrementsAndReturnsOne() {
        seedTag("TAG-A", 2, 0);

        int updated = idTagRepository.incrementActiveTransactionCountIfAllowed("TAG-A");

        assertThat(updated).isEqualTo(1);
        assertThat(idTagRepository.findByIdTag("TAG-A").orElseThrow().getActiveTransactionCount()).isEqualTo(1);
    }

    @Test
    void atLimit_rejectsAndLeavesCountUnchanged() {
        seedTag("TAG-B", 1, 1);

        int updated = idTagRepository.incrementActiveTransactionCountIfAllowed("TAG-B");

        assertThat(updated).isEqualTo(0);
        assertThat(idTagRepository.findByIdTag("TAG-B").orElseThrow().getActiveTransactionCount()).isEqualTo(1);
    }

    @Test
    void negativeMax_treatedAsUnlimited() {
        seedTag("TAG-C", -1, 50);

        assertThat(idTagRepository.incrementActiveTransactionCountIfAllowed("TAG-C")).isEqualTo(1);
    }

    @Test
    void nullMax_treatedAsUnlimited() {
        seedTag("TAG-D", null, 0);

        assertThat(idTagRepository.incrementActiveTransactionCountIfAllowed("TAG-D")).isEqualTo(1);
    }

    @Test
    void unknownTag_returnsZero() {
        assertThat(idTagRepository.incrementActiveTransactionCountIfAllowed("DOES-NOT-EXIST")).isEqualTo(0);
    }
}
