import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/services/api.service';

@Component({
  selector: 'pgw-routing',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page-header">
      <h2>Routing Engine</h2>
      <p>Manage routing rules and provider health</p>
    </div>

    <!-- Provider Status Cards -->
    <div class="metrics-grid fade-in" style="margin-bottom: 32px;">
      <div class="card" *ngFor="let p of providers">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
          <strong>{{ p.name }}</strong>
          <span class="badge" [ngClass]="p.healthStatus?.toLowerCase()">{{ p.healthStatus }}</span>
        </div>
        <div style="font-size: 13px; color: var(--text-secondary); margin-bottom: 12px;">
          <div>Code: {{ p.code }}</div>
          <div>Methods: {{ p.supportedMethods }}</div>
          <div>Approval Rate: {{ p.currentApprovalRate | number:'1.1-1' }}%</div>
          <div>Avg Latency: {{ p.averageLatencyMs | number:'1.0-0' }}ms</div>
        </div>
        <div class="health-bar" style="margin-bottom: 12px;">
          <div class="fill" [style.width.%]="p.currentApprovalRate"
               [ngClass]="{ 'good': p.currentApprovalRate >= 80, 'warn': p.currentApprovalRate >= 60 && p.currentApprovalRate < 80, 'bad': p.currentApprovalRate < 60 }">
          </div>
        </div>
        <button class="btn" [ngClass]="p.enabled ? 'btn-danger' : 'btn-primary'"
                (click)="toggleProvider(p.code, !p.enabled)">
          {{ p.enabled ? '⏸ Disable' : '▶ Enable' }}
        </button>
      </div>
    </div>

    <!-- Routing Rules -->
    <div class="card fade-in">
      <div class="card-header"><h3>Active Routing Rules</h3></div>
      <table class="data-table">
        <thead>
          <tr><th>Payment Method</th><th>Provider</th><th>Priority</th><th>Weight</th><th>Description</th></tr>
        </thead>
        <tbody>
          <tr *ngFor="let r of rules">
            <td><span class="badge processing">{{ r.paymentMethod }}</span></td>
            <td><strong>{{ r.providerCode }}</strong></td>
            <td>{{ r.priority }}</td>
            <td>{{ r.weight }}%</td>
            <td style="font-size: 13px; color: var(--text-secondary);">{{ r.description || '—' }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Approval Rates -->
    <div class="card fade-in" style="margin-top: 24px;" *ngIf="approvalRates">
      <div class="card-header"><h3>Current Approval Rates (Rolling Window)</h3></div>
      <div style="display: flex; gap: 32px;">
        <div *ngFor="let entry of approvalRateEntries" style="flex: 1; text-align: center; padding: 20px;">
          <div style="font-size: 40px; font-weight: 700; margin-bottom: 8px;"
               [style.color]="entry[1] >= 80 ? 'var(--accent-green)' : entry[1] >= 60 ? 'var(--accent-yellow)' : 'var(--accent-red)'">
            {{ entry[1] | number:'1.1-1' }}%
          </div>
          <div style="color: var(--text-secondary);">{{ entry[0] }}</div>
        </div>
      </div>
    </div>
  `
})
export class RoutingComponent implements OnInit {
  providers: any[] = [];
  rules: any[] = [];
  approvalRates: any = null;

  get approvalRateEntries(): [string, number][] {
    return this.approvalRates ? Object.entries(this.approvalRates) as [string, number][] : [];
  }

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.api.getProviders().subscribe(data => this.providers = data);
    this.api.getRoutingRules().subscribe(data => this.rules = data);
    this.api.getApprovalRates().subscribe(data => this.approvalRates = data);
  }

  toggleProvider(code: string, enabled: boolean) {
    this.api.toggleProvider(code, enabled).subscribe(() => this.loadData());
  }
}
