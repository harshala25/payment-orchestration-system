package com.yuno.gateway.controller;

import com.yuno.gateway.entity.AuditLog;
import com.yuno.gateway.service.ComplianceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/compliance")
@RequiredArgsConstructor
@Tag(name = "Compliance", description = "Audit trail and compliance APIs")
public class ComplianceController {

    private final ComplianceService complianceService;

    @GetMapping("/audit-log")
    @Operation(summary = "Query audit logs", description = "Paginated compliance audit trail")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return ResponseEntity.ok(complianceService.getAuditLogs(pageable));
    }

    @GetMapping("/audit-log/{entityType}/{entityId}")
    @Operation(summary = "Get audit logs for a specific entity")
    public ResponseEntity<Page<AuditLog>> getEntityAuditLogs(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(complianceService.getAuditLogsByEntity(entityType, entityId, pageable));
    }

    @GetMapping("/security-report")
    @Operation(summary = "Generate security posture report")
    public ResponseEntity<ComplianceService.SecurityReport> getSecurityReport(
            @RequestParam(defaultValue = "168") int hoursBack) {
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusHours(hoursBack);
        return ResponseEntity.ok(complianceService.generateSecurityReport(from, to));
    }
}
