import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/services/api.service';

@Component({
  selector: 'pgw-analytics',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h2 style="font-size: 28px; font-weight: 800; letter-spacing: -0.5px; margin-bottom: 24px;" class="fade-in">Insights</h2>

    <!-- Insights Sub-Header Tabs (Yuno Style) -->
    <div class="tabs-bar fade-in">
      <div class="tab-item">Overview</div>
      <div class="tab-item active">Volume</div>
      <div class="tab-item">Conversion rate</div>
      <div class="tab-item">Fraud</div>
    </div>

    <!-- Filter Actions Bar (Yuno Style) -->
    <div class="filters-bar fade-in" style="animation-delay: 0.05s;">
      <div class="filters-left">
        <button class="pill-btn">
          <svg viewBox="0 0 24 24"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"></polygon></svg>
          Add filter
        </button>
        <button class="pill-btn">
          <svg viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
          Last 3 days
        </button>
        <button class="pill-btn">
          Daily
          <svg viewBox="0 0 24 24" style="width: 10px; height: 10px;"><polyline points="6 9 12 15 18 9"></polyline></svg>
        </button>
      </div>

      <div class="filters-right">
        <button class="pill-btn" style="border-color: #3b52f6; color: #3b52f6;">
          <svg viewBox="0 0 24 24" style="stroke: #3b52f6;"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
          Add chart
        </button>
        <button class="pill-btn">
          <svg viewBox="0 0 24 24"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect><line x1="9" y1="3" x2="9" y2="21"></line></svg>
          Customize
        </button>
        <button class="pill-btn">
          <svg viewBox="0 0 24 24"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
          Download
        </button>
      </div>
    </div>

    <!-- Main Visual Insights Dashboard Layout (Yuno Replicated) -->
    <div *ngIf="dashboard" class="insights-dashboard fade-in" style="animation-delay: 0.1s;">
      
      <!-- Top Grid: Left (Total Volume & Retries Box), Right (Standard KPIs Panel) -->
      <div class="top-insights-row">
        
        <!-- Left Box: Total Volume & Retries Comparison Graph Overlay -->
        <div class="card volume-comparison-card">
          <div class="card-title-row">
            <span class="card-label">ⓘ Total volume</span>
            <a style="cursor: pointer;" class="view-more">View more</a>
          </div>
          
          <div class="volume-display-box">
            <div class="volume-large-val">
              ₹ {{ (dashboard.totalVolume / 1000) | number:'1.1-1' }}K <span class="currency-label">INR</span>
            </div>
            <div class="volume-sub-val">
              Total successful payments: <strong>{{ dashboard.successfulTransactions }}</strong>
            </div>
          </div>

          <!-- FLOATING SUCCESS RATE RETRIES COMPARISON WIDGET -->
          <div class="retries-floating-card">
            <div class="comparison-header">
              <!-- With retries -->
              <div class="comp-col">
                <div class="comp-legend">
                  <span class="dot lime"></span>
                  <span class="comp-label">With retries</span>
                </div>
                <div class="comp-val">96% <span class="comp-sub">(6.15K succeeded)</span></div>
              </div>
              
              <!-- Without retries -->
              <div class="comp-col">
                <div class="comp-legend">
                  <span class="dot blue"></span>
                  <span class="comp-label">Without retries</span>
                </div>
                <div class="comp-val">67% <span class="comp-sub">(5.06K succeeded)</span></div>
              </div>
            </div>

            <!-- CSS Bar Chart Graph representation -->
            <div class="chart-canvas-mock">
              <!-- Y-Axis Labels -->
              <div class="y-labels">
                <span>100%</span>
                <span>75%</span>
                <span>50%</span>
                <span>25%</span>
                <span>0</span>
              </div>
              
              <!-- Bars Area -->
              <div class="bars-container">
                <!-- Bar 1: With Retries (96% Height, Lime-Green Color) -->
                <div class="bar-wrapper">
                  <div class="bar lime-bar" [style.height.%]="96">
                    <div class="bar-glow"></div>
                  </div>
                </div>
                
                <!-- Bar 2: Without Retries (67% Height, Royal-Blue Color) -->
                <div class="bar-wrapper">
                  <div class="bar blue-bar" [style.height.%]="67">
                    <div class="bar-glow"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Right Box: Clean Key-Value KPIs Grid (Yuno Style) -->
        <div class="card kpi-panel-card">
          <div class="kpi-container">
            <div class="kpi-row">
              <div class="kpi-label">
                <span>ⓘ Total refunds volume</span>
              </div>
              <div class="kpi-value">₹ {{ (dashboard.totalVolume * 0.05) | number:'1.2-2' }} INR</div>
            </div>
            
            <div class="kpi-row">
              <div class="kpi-label">
                <span>ⓘ Successful refunds</span>
              </div>
              <div class="kpi-value">{{ (dashboard.totalTransactions * 0.03) | number:'1.0-0' }}</div>
            </div>

            <div class="kpi-row">
              <div class="kpi-label">
                <span>ⓘ Total chargebacks volume</span>
              </div>
              <div class="kpi-value">₹ {{ (dashboard.totalVolume * 0.015) | number:'1.2-2' }} INR</div>
            </div>

            <div class="kpi-row">
              <div class="kpi-label">
                <span>ⓘ Total chargebacks</span>
              </div>
              <div class="kpi-value">{{ (dashboard.totalTransactions * 0.01) | number:'1.0-0' }}</div>
            </div>
          </div>
        </div>

      </div>

      <!-- Bottom Grid: Three Cards (Top Methods, Top Providers, Top Countries) -->
      <div class="bottom-insights-row">
        
        <!-- Card 1: Top methods -->
        <div class="card distribution-card">
          <div class="card-title-row">
            <span class="card-label">ⓘ Top methods</span>
            <a style="cursor: pointer;" class="view-more">View more</a>
          </div>
          
          <div class="donut-visual-container">
            <div class="custom-pie-chart" style="background: conic-gradient(#3b52f6 0% 70%, #eff6ff 70% 100%);">
              <div class="inner-cutout"></div>
            </div>
            <div class="distribution-legend">
              <div class="legend-row" *ngFor="let m of dashboard.paymentMethodBreakdown">
                <span class="dot" [ngClass]="m.paymentMethod === 'CARD' ? 'blue' : 'light-blue'"></span>
                <span class="dist-name">{{ m.paymentMethod }}</span>
                <span class="dist-val">{{ m.approvalRate | number:'1.0-0' }}%</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Card 2: Top providers -->
        <div class="card distribution-card">
          <div class="card-title-row">
            <span class="card-label">ⓘ Top providers</span>
            <a style="cursor: pointer;" class="view-more">View more</a>
          </div>
          
          <div class="donut-visual-container">
            <div class="custom-pie-chart" style="background: conic-gradient(#f97316 0% 55%, #10b981 55% 90%, #d4ec6b 90% 100%);">
              <div class="inner-cutout"></div>
            </div>
            <div class="distribution-legend">
              <div class="legend-row" *ngFor="let p of dashboard.providerBreakdown">
                <span class="dot" [ngClass]="p.providerCode === 'PROVIDER_A' ? 'orange' : 'green'"></span>
                <span class="dist-name">{{ p.providerName || p.providerCode }}</span>
                <span class="dist-val">{{ p.approvalRate | number:'1.0-0' }}%</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Card 3: Top countries -->
        <div class="card distribution-card">
          <div class="card-title-row">
            <span class="card-label">ⓘ Top countries</span>
            <a style="cursor: pointer;" class="view-more">View more</a>
          </div>
          
          <div class="donut-visual-container">
            <div class="custom-pie-chart" style="background: conic-gradient(#3b52f6 0% 80%, #94a3b8 80% 100%);">
              <div class="inner-cutout"></div>
            </div>
            <div class="distribution-legend">
              <div class="legend-row">
                <span class="dot blue"></span>
                <span class="dist-name">India (IN)</span>
                <span class="dist-val">90%</span>
              </div>
              <div class="legend-row">
                <span class="dot gray"></span>
                <span class="dist-name">Others</span>
                <span class="dist-val">10%</span>
              </div>
            </div>
          </div>
        </div>

      </div>

    </div>

    <!-- Loading State -->
    <div *ngIf="!dashboard" style="display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 100px 0; color: var(--text-muted);">
      <div class="spinner"></div>
      <p style="margin-top: 16px; font-weight: 500;">Loading Insights...</p>
    </div>
  `,
  styles: [`
    .insights-dashboard {
      display: flex;
      flex-direction: column;
      gap: 32px;
    }

    .top-insights-row {
      display: grid;
      grid-template-columns: 1.4fr 1fr;
      gap: 32px;
      align-items: stretch;
    }

    .bottom-insights-row {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 32px;
    }

    .card-title-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
      .card-label { font-size: 13.5px; font-weight: 600; color: #555a6e; }
      .view-more { font-size: 12.5px; font-weight: 600; color: #3b52f6; text-decoration: none; }
    }

    // Left Volume & Comparison Card styling
    .volume-comparison-card {
      position: relative;
      padding: 32px;
      min-height: 380px;
      background: #ffffff;
      border: 1px solid #eef2f6;
      border-radius: var(--radius);
    }

    .volume-display-box {
      margin-bottom: 24px;
      .volume-large-val { font-size: 36px; font-weight: 800; color: #0f172a; letter-spacing: -1px; }
      .currency-label { font-size: 16px; font-weight: 600; color: #64748b; margin-left: 4px; }
      .volume-sub-val { font-size: 13.5px; color: #64748b; margin-top: 4px; }
    }

    // Custom Retries Floating comparison chart
    .retries-floating-card {
      background: #ffffff;
      border: 1px solid #eef2f6;
      border-radius: 16px;
      box-shadow: 0 10px 25px rgba(0,0,0,0.03);
      padding: 20px;
      position: absolute;
      bottom: 24px;
      right: 24px;
      width: 320px;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .comparison-header {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }

    .comp-col {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .comp-legend {
      display: flex;
      align-items: center;
      gap: 6px;
      .dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
      .dot.lime { background: #cbe850; }
      .dot.blue { background: #3b52f6; }
      .comp-label { font-size: 11px; font-weight: 600; color: #64748b; }
    }

    .comp-val {
      font-size: 16px;
      font-weight: 800;
      color: #0f172a;
      .comp-sub { font-size: 9.5px; color: #94a3b8; font-weight: 500; display: block; }
    }

    // Chart Canvas Mock Grid lines & Bars
    .chart-canvas-mock {
      height: 120px;
      border-top: 1px dashed #eef2f6;
      border-bottom: 1px solid #eef2f6;
      position: relative;
      display: flex;
      padding-left: 36px;
    }

    .y-labels {
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      font-size: 9px;
      font-weight: 600;
      color: #94a3b8;
      text-align: right;
      width: 24px;
      padding: 4px 0;
    }

    .bars-container {
      flex: 1;
      display: flex;
      justify-content: space-around;
      align-items: flex-end;
      padding: 0 20px;
    }

    .bar-wrapper {
      width: 38px;
      height: 100%;
      display: flex;
      align-items: flex-end;
      justify-content: center;
    }

    .bar {
      width: 100%;
      border-radius: 6px 6px 0 0;
      position: relative;
      transition: height 0.5s ease;
      cursor: pointer;

      &.lime-bar { background: #cbe850; }
      &.blue-bar { background: #3b52f6; }

      .bar-glow {
        position: absolute;
        top: 0; left: 0; right: 0; bottom: 0;
        border-radius: 6px 6px 0 0;
        background: linear-gradient(to bottom, rgba(255,255,255,0.15), transparent);
      }
    }

    // KPI Panel card styling
    .kpi-panel-card {
      padding: 32px;
      display: flex;
      flex-direction: column;
      justify-content: center;
    }

    // Distribution pie chart graphics
    .distribution-card {
      padding: 24px;
      display: flex;
      flex-direction: column;
    }

    .donut-visual-container {
      display: flex;
      align-items: center;
      gap: 24px;
      margin-top: 8px;
    }

    .custom-pie-chart {
      width: 90px;
      height: 90px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
      flex-shrink: 0;

      .inner-cutout {
        width: 54px;
        height: 54px;
        border-radius: 50%;
        background: #ffffff;
      }
    }

    .distribution-legend {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .legend-row {
      display: flex;
      align-items: center;
      font-size: 12.5px;
      .dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; margin-right: 8px; }
      .dot.blue { background: #3b52f6; }
      .dot.light-blue { background: #eff6ff; border: 1px solid #3b52f6; }
      .dot.orange { background: #f97316; }
      .dot.green { background: #10b981; }
      .dot.gray { background: #94a3b8; }
      .dist-name { color: #64748b; font-weight: 500; }
      .dist-val { margin-left: auto; font-weight: 700; color: #0f172a; }
    }

    .spinner {
      border: 3px solid rgba(59, 82, 246, 0.05);
      width: 40px;
      height: 40px;
      border-radius: 50%;
      border-left-color: #3b52f6;
      animation: spin 1s linear infinite;
    }
    @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
  `]
})
export class AnalyticsComponent implements OnInit {
  dashboard: any = null;

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.api.getDashboard(24).subscribe({
      next: (data) => this.dashboard = data,
      error: (err) => console.error('Insights load failed:', err)
    });
  }
}
