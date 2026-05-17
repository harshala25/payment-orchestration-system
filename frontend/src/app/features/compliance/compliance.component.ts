import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/services/api.service';

@Component({
  selector: 'pgw-compliance',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page-header">
      <h2>Compliance & Audit</h2>
      <p>Security posture and audit trail</p>
    </div>

    <!-- Security Report -->
    <div class="card fade-in" style="margin-bottom: 24px;" *ngIf="securityReport">
      <div class="card-header"><h3>🛡️ Security Posture Report</h3></div>
      <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px;">
        <div style="text-align: center; padding: 16px;">
          <div style="font-size: 28px;">{{ securityReport.cardDataMasking ? '✅' : '❌' }}</div>
          <div style="font-size: 13px; color: var(--text-secondary); margin-top: 8px;">Card Masking</div>
        </div>
        <div style="text-align: center; padding: 16px;">
          <div style="font-size: 28px;">{{ securityReport.auditTrailComplete ? '✅' : '❌' }}</div>
          <div style="font-size: 13px; color: var(--text-secondary); margin-top: 8px;">Audit Trail</div>
        </div>
        <div style="text-align: center; padding: 16px;">
          <div style="font-size: 28px;">{{ securityReport.owaspHeadersEnabled ? '✅' : '❌' }}</div>
          <div style="font-size: 13px; color: var(--text-secondary); margin-top: 8px;">OWASP Headers</div>
        </div>
        <div style="text-align: center; padding: 16px;">
          <div style="font-size: 28px;">{{ securityReport.apiKeyAuthEnabled ? '✅' : '❌' }}</div>
          <div style="font-size: 13px; color: var(--text-secondary); margin-top: 8px;">API Key Auth</div>
        </div>
      </div>
      <div style="text-align: center; margin-top: 16px; padding-top: 16px; border-top: 1px solid var(--border-color);">
        <span class="badge success" style="font-size: 14px; padding: 8px 24px;">{{ securityReport.complianceStatus }}</span>
        <div style="font-size: 13px; color: var(--text-muted); margin-top: 8px;">
          {{ securityReport.totalAuditEvents }} audit events recorded
        </div>
      </div>
    </div>

    <!-- Audit Logs -->
    <div class="card fade-in">
      <div class="card-header">
        <h3>Audit Trail</h3>
        <button class="btn btn-outline" (click)="loadAuditLogs()">🔄 Refresh</button>
      </div>
      <table class="data-table">
        <thead>
          <tr><th>Timestamp</th><th>Entity</th><th>Action</th><th>Details</th><th>Merchant</th><th>Trace ID</th></tr>
        </thead>
        <tbody>
          <tr *ngFor="let log of auditLogs">
            <td style="font-size: 12px; white-space: nowrap;">{{ log.timestamp | date:'medium' }}</td>
            <td>
              <span style="font-size: 11px; color: var(--accent-cyan);">{{ log.entityType }}</span>
              <div style="font-family: monospace; font-size: 11px; color: var(--text-muted);">{{ log.entityId?.substring(0, 8) }}...</div>
            </td>
            <td><span class="badge processing">{{ log.action }}</span></td>
            <td style="font-size: 12px; max-width: 300px; overflow: hidden; text-overflow: ellipsis;">
              {{ log.newValue || log.oldValue || '—' }}
            </td>
            <td style="font-size: 12px;">{{ log.merchantId || '—' }}</td>
            <td style="font-family: monospace; font-size: 10px; color: var(--text-muted);">{{ log.traceId?.substring(0, 8) }}</td>
          </tr>
          <tr *ngIf="auditLogs.length === 0">
            <td colspan="6" style="text-align: center; color: var(--text-muted); padding: 40px;">
              No audit logs yet. Create payments to generate audit entries.
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  `
})
export class ComplianceComponent implements OnInit {
  auditLogs: any[] = [];
  securityReport: any = null;

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.loadAuditLogs();
    this.api.getSecurityReport().subscribe(data => this.securityReport = data);
  }

  loadAuditLogs() {
    this.api.getAuditLogs().subscribe(data => this.auditLogs = data.content || []);
  }
}
