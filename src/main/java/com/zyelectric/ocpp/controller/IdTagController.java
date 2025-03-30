package com.zyelectric.ocpp.controller;

import com.zyelectric.ocpp.model.IdTag;
import com.zyelectric.ocpp.request.IdTagRequest;
import com.zyelectric.ocpp.service.IdTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class IdTagController {

    private final IdTagService idTagService;

    @PostMapping
    public ResponseEntity<IdTag> saveTag(@RequestBody IdTagRequest request) {
        IdTag savedTag = idTagService.saveTag(request);
        return ResponseEntity.ok(savedTag);
    }

    @GetMapping
    public ResponseEntity<List<IdTag>> getAllTags() {
        return ResponseEntity.ok(idTagService.getAllTags());
    }

    @GetMapping("/{id}")
    public ResponseEntity<IdTag> getTagById(@PathVariable String id) {
        return idTagService.getTagById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTag(@PathVariable String id) {
        idTagService.deleteTag(idTagService.getTagById(id).orElseThrow());
        return ResponseEntity.noContent().build();
    }
}
