import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';

@Component({
  selector: 'pgw-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <!-- Top Row: Overview Title and Quick Stats -->
    <div class="filters-bar fade-in">
      <div class="filters-left">
        <h2 style="font-size: 24px; font-weight: 800; letter-spacing: -0.5px;">Dashboard</h2>
      </div>
      <div class="filters-right">
        <button class="pill-btn">
          <svg viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
          Select Date Range
        </button>
      </div>
    </div>

    <!-- Quick KPIs Cards Grid -->
    <div class="dashboard-grid fade-in" style="animation-delay: 0.05s;">
      
      <!-- Metrics row -->
      <div class="metrics-row" style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 24px; margin-bottom: 32px;">
        <!-- Card 1: Total Transactions -->
        <div class="card metric-card">
          <div class="metric-info">
            <div class="metric-label" style="font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px; color: var(--text-secondary); font-weight: 700;">Total Transactions</div>
            <div class="metric-value" style="font-size: 28px; font-weight: 800; color: #0f172a; margin-top: 8px;">{{ dashboard?.totalTransactions || 0 }}</div>
            <div class="metric-sub" style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">Last 24 Hours</div>
          </div>
        </div>

        <!-- Card 2: Success Rate -->
        <div class="card metric-card">
          <div class="metric-info">
            <div class="metric-label" style="font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px; color: var(--text-secondary); font-weight: 700;">Success Rate</div>
            <div class="metric-value" style="font-size: 28px; font-weight: 800; color: #10b981; margin-top: 8px;">{{ (dashboard?.overallSuccessRate || 0) | number:'1.1-1' }}%</div>
            <div class="metric-sub" style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">
              {{ (dashboard?.overallSuccessRate || 0) >= 80 ? '✓ Exceeds 80% Target' : '⚠️ Action Required' }}
            </div>
          </div>
        </div>

        <!-- Card 3: Approved Volume -->
        <div class="card metric-card">
          <div class="metric-info">
            <div class="metric-label" style="font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px; color: var(--text-secondary); font-weight: 700;">Total Volume</div>
            <div class="metric-value" style="font-size: 28px; font-weight: 800; color: #3b52f6; margin-top: 8px;">₹ {{ (dashboard?.totalVolume || 0) | number:'1.0-0' }}</div>
            <div class="metric-sub" style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">Currency: INR</div>
          </div>
        </div>

        <!-- Card 4: Failed Transactions -->
        <div class="card metric-card">
          <div class="metric-info">
            <div class="metric-label" style="font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px; color: var(--text-secondary); font-weight: 700;">Declined / Failed</div>
            <div class="metric-value" style="font-size: 28px; font-weight: 800; color: #ef4444; margin-top: 8px;">{{ dashboard?.failedTransactions || 0 }}</div>
            <div class="metric-sub" style="font-size: 12px; color: var(--text-muted); margin-top: 4px;">Declines / Timeouts</div>
          </div>
        </div>
      </div>

      <!-- Provider Comparison & Decline Reasons -->
      <div style="display: grid; grid-template-columns: 1.3fr 1fr; gap: 32px; margin-bottom: 32px;">
        
        <!-- Providers Card -->
        <div class="card fade-in" style="animation-delay: 0.1s; padding: 32px;">
          <div class="card-title-row" style="display: flex; justify-content: space-between; margin-bottom: 24px; border-bottom: 1px solid #eef2f6; padding-bottom: 12px;">
            <h3 style="font-size: 13px; text-transform: uppercase; letter-spacing: 0.8px; color: var(--text-secondary); font-weight: 700;">Provider Health & Performance</h3>
          </div>
          
          <div *ngFor="let p of dashboard?.providerBreakdown" style="margin-bottom: 24px; &:last-child { margin-bottom: 0; }">
            <div style="display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 14px;">
              <div>
                <strong>{{ p.providerName || p.providerCode }}</strong>
                <span class="badge" [ngClass]="p.healthStatus?.toLowerCase()" style="margin-left: 12px; border-radius: 4px;">
                  {{ p.healthStatus }}
                </span>
              </div>
              <span style="font-weight: 700; color: #3b52f6;">{{ p.approvalRate | number:'1.1-1' }}% approval</span>
            </div>
            
            <!-- Health progress bar -->
            <div class="health-bar" style="height: 6px; background: #eef2f6; border-radius: 10px; overflow: hidden; margin: 8px 0;">
              <div class="fill" [style.width.%]="p.approvalRate"
                   style="height: 100%; border-radius: 10px; transition: width 0.4s ease;"
                   [ngClass]="{ 'good': p.approvalRate >= 80, 'warn': p.approvalRate >= 60 && p.approvalRate < 80, 'bad': p.approvalRate < 60 }">
              </div>
            </div>
            
            <div style="display: flex; gap: 24px; margin-top: 8px; font-size: 12px; color: var(--text-muted); font-weight: 500;">
              <span>Total: <strong>{{ p.totalCount }}</strong> txns</span>
              <span>Avg Latency: <strong>{{ p.avgLatencyMs | number:'1.0-0' }}ms</strong></span>
              <span>p95 Speed: <strong>{{ p.p95LatencyMs | number:'1.0-0' }}ms</strong></span>
            </div>
          </div>
          <div *ngIf="!dashboard?.providerBreakdown?.length" style="color: var(--text-muted); text-align: center; padding: 40px; font-size: 14px;">
            No transaction data yet. Create some payments to see analytics.
          </div>
        </div>

        <!-- Decline Reasons Card -->
        <div class="card fade-in" style="animation-delay: 0.15s; padding: 32px;">
          <div class="card-title-row" style="display: flex; justify-content: space-between; margin-bottom: 24px; border-bottom: 1px solid #eef2f6; padding-bottom: 12px;">
            <h3 style="font-size: 13px; text-transform: uppercase; letter-spacing: 0.8px; color: var(--text-secondary); font-weight: 700;">Top Decline Reasons</h3>
          </div>
          
          <table class="data-table" *ngIf="dashboard?.topDeclineReasons?.length" style="width: 100%;">
            <thead>
              <tr>
                <th style="padding: 10px 8px; font-size: 10px;">Reason</th>
                <th style="padding: 10px 8px; font-size: 10px; text-align: center;">Count</th>
                <th style="padding: 10px 8px; font-size: 10px; text-align: right;">% Share</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let d of dashboard?.topDeclineReasons">
                <td style="padding: 12px 8px; font-size: 13px;"><strong>{{ d.reason?.replace('_', ' ') }}</strong></td>
                <td style="padding: 12px 8px; font-size: 13px; text-align: center; color: var(--text-secondary);">{{ d.count }}</td>
                <td style="padding: 12px 8px; font-size: 13px; text-align: right; font-weight: 700;">{{ d.percentage | number:'1.1-1' }}%</td>
              </tr>
            </tbody>
          </table>
          <div *ngIf="!dashboard?.topDeclineReasons?.length" style="color: var(--text-muted); text-align: center; padding: 40px; font-size: 14px;">
            No decline data available yet. Healthy payment pipelines.
          </div>
        </div>
      </div>

      <!-- Payment Method breakdown card -->
      <div class="card fade-in" *ngIf="dashboard?.paymentMethodBreakdown?.length" style="animation-delay: 0.2s; padding: 32px;">
        <div class="card-title-row" style="display: flex; justify-content: space-between; margin-bottom: 24px; border-bottom: 1px solid #eef2f6; padding-bottom: 12px;">
          <h3 style="font-size: 13px; text-transform: uppercase; letter-spacing: 0.8px; color: var(--text-secondary); font-weight: 700;">Payment Method Metrics</h3>
        </div>
        
        <table class="data-table" style="width: 100%;">
          <thead>
            <tr>
              <th>Method</th>
              <th style="text-align: center;">Total Attempts</th>
              <th style="text-align: center;">Success count</th>
              <th style="text-align: right;">Approval Rate</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let m of dashboard?.paymentMethodBreakdown">
              <td><strong>💳 {{ m.paymentMethod }}</strong></td>
              <td style="text-align: center; color: var(--text-secondary);">{{ m.totalCount }}</td>
              <td style="text-align: center; color: var(--text-secondary);">{{ m.successCount }}</td>
              <td style="text-align: right;">
                <span class="badge" [ngClass]="m.approvalRate >= 80 ? 'success' : m.approvalRate >= 60 ? 'retry' : 'failed'" style="border-radius: 4px;">
                  {{ m.approvalRate | number:'1.1-1' }}%
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

    </div>
  `,
  styles: [`
    :host { display: block; }
    
    // Status colors for health bar fills
    .fill.good { background: #10b981 !important; }
    .fill.warn { background: #f59e0b !important; }
    .fill.bad { background: #ef4444 !important; }
  `]
})
export class DashboardComponent implements OnInit {
  dashboard: any = null;

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.api.getDashboard(24).subscribe({
      next: (data) => this.dashboard = data,
      error: (err) => console.error('Dashboard load failed:', err)
    });
  }
}
