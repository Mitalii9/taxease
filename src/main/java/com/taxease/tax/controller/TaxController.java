package com.taxease.tax.controller;

import com.taxease.tax.dto.TaxCalculationRequest;
import com.taxease.tax.dto.TaxCalculationResponse;
import com.taxease.tax.dto.TaxRecordDTO;
import com.taxease.tax.service.TaxService;
import com.taxease.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tax")
@RequiredArgsConstructor
@Tag(name = "Tax", description = "Indian income tax calculation endpoints (FY 2025-26)")
public class TaxController {

    private final TaxService taxService;
    private final UserService userService;

    @PostMapping("/calculate")
    @Operation(summary = "Calculate income tax under old and new regime for FY 2025-26")
    public ResponseEntity<TaxCalculationResponse> calculate(
            @Valid @RequestBody TaxCalculationRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String userId = userService.getProfile(email).getId();
        return ResponseEntity.ok(taxService.calculate(request, userId));
    }

    @GetMapping("/history/{userId}")
    @Operation(summary = "Retrieve past tax calculations for a user")
    public ResponseEntity<List<TaxRecordDTO>> getHistory(@PathVariable String userId) {
        return ResponseEntity.ok(taxService.getHistory(userId));
    }
}
