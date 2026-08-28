package com.zyelectric.ocpp.controller;

import com.zyelectric.ocpp.model.ChargeBox;
import com.zyelectric.ocpp.request.ChargerRegistrationRequest;
import com.zyelectric.ocpp.response.ChargerTransactionDTO;
import com.zyelectric.ocpp.service.ChargerService;
import com.zyelectric.ocpp.service.TransactionHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/chargers")
@RequiredArgsConstructor
public class ChargerController {

    private final ChargerService chargerService;
    private final TransactionHistoryService transactionHistoryService;

    // Pre-registers a charger and sets the password it must use for the OCPP WebSocket
    // handshake (HTTP Basic Auth, username = charger id). Body, not query params - a
    // password in the URL ends up in access logs, proxy logs, and browser/shell history.
    @PostMapping
    public ResponseEntity<String> createCharger(@Valid @RequestBody ChargerRegistrationRequest request) {
        chargerService.preRegisterCharger(request.getCharger(), request.getPassword());
        return ResponseEntity.ok("Charger pre-registered successfully!");
    }

    @GetMapping("/{serialNumber}")
    public ResponseEntity<ChargeBox> getChargerBySerialNumber(@PathVariable String serialNumber) {
        Optional<ChargeBox> chargerOpt = chargerService.getChargerBySerialNumber(serialNumber);
        return chargerOpt.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<?> getAllChargers() {
        return ResponseEntity.ok(chargerService.getAllChargers());
    }

    @GetMapping("/charger/history")
    public ResponseEntity<ChargerTransactionDTO> getHistory(
            @RequestParam String chargeBoxId,
            @RequestParam Long startTime,
            @RequestParam Long endTime) {

        ChargerTransactionDTO history = transactionHistoryService.getTransactionHistory(chargeBoxId, startTime, endTime);
        return ResponseEntity.ok(history);
    }
}
