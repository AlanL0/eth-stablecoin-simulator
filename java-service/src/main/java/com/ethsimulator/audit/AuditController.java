package com.ethsimulator.audit;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/{address}")
    public AuditResponse audit(
            @PathVariable String address,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String token,
            @RequestParam(defaultValue = "false") boolean hideValues
    ) {
        return auditService.audit(address, from, to, token, hideValues);
    }

    @GetMapping(value = "/{address}/export.csv", produces = "text/csv")
    public ResponseEntity<String> exportCsv(
            @PathVariable String address,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String token,
            @RequestParam(defaultValue = "false") boolean hideValues
    ) {
        String csv = auditService.exportCsv(address, from, to, token, hideValues);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-" + address + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/{address}/export.json")
    public AuditResponse exportJson(
            @PathVariable String address,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String token,
            @RequestParam(defaultValue = "false") boolean hideValues
    ) {
        return auditService.audit(address, from, to, token, hideValues);
    }
}