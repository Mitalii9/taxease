package com.taxease.tax.controller;

import com.taxease.tax.dto.CreateTaxRecordRequest;
import com.taxease.tax.dto.TaxRecordDTO;
import com.taxease.tax.model.TaxStatus;
import com.taxease.tax.service.TaxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tax")
@RequiredArgsConstructor
@Tag(name = "Tax", description = "Tax record management endpoints")
public class TaxController {

    private final TaxService taxService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all tax records for a user")
    public ResponseEntity<List<TaxRecordDTO>> findByUser(@PathVariable String userId) {
        return ResponseEntity.ok(taxService.findByUser(userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a tax record by ID")
    public ResponseEntity<TaxRecordDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(taxService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new tax record")
    public ResponseEntity<TaxRecordDTO> create(@Valid @RequestBody CreateTaxRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taxService.create(request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update the status of a tax record")
    public ResponseEntity<TaxRecordDTO> updateStatus(
            @PathVariable String id,
            @RequestParam TaxStatus status) {
        return ResponseEntity.ok(taxService.updateStatus(id, status));
    }
}
