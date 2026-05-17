import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';

@Component({
  selector: 'pgw-payment-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="page-header">
      <a routerLink="/payments" style="font-size: 14px; color: var(--text-muted);">← Back to Payments</a>
      <h2 style="margin-top: 8px;">Payment Details</h2>
    </div>

    <div *ngIf="payment" class="fade-in">
      <!-- Status + Summary -->
      <div class="card" style="margin-bottom: 24px;">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <div>
            <span class="badge" [ngClass]="payment.status?.toLowerCase()" style="font-size: 14px; padding: 6px 16px;">
              {{ payment.status }}
            </span>
            <span style="margin-left: 16px; font-size: 24px; font-weight: 700;">
              {{ payment.currency }} {{ payment.amount | number:'1.2-2' }}
            </span>
          </div>
          <div style="text-align: right; font-size: 13px; color: var(--text-secondary);">
            <div>Created: {{ payment.createdAt | date:'medium' }}</div>
            <div>ID: <span style="font-family: monospace;">{{ payment.id }}</span></div>
          </div>
        </div>
      </div>

      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 24px;">
        <!-- Payment Info -->
        <div class="card">
          <div class="card-header"><h3>Payment Information</h3></div>
          <div class="detail-grid">
            <div class="detail-row"><span>Merchant</span><strong>{{ payment.merchantId }}</strong></div>
            <div class="detail-row"><span>Method</span><strong>{{ payment.paymentMethod }}</strong></div>
            <div class="detail-row"><span>Provider</span><strong>{{ payment.providerUsed || '—' }}</strong></div>
            <div class="detail-row"><span>Provider Txn ID</span><strong style="font-family: monospace; font-size: 12px;">{{ payment.providerTransactionId || '—' }}</strong></div>
            <div class="detail-row" *ngIf="payment.maskedCardNumber"><span>Card</span><strong>{{ payment.maskedCardNumber }} ({{ payment.cardBrand }})</strong></div>
            <div class="detail-row" *ngIf="payment.upiVpa"><span>UPI VPA</span><strong>{{ payment.upiVpa }}</strong></div>
            <div class="detail-row"><span>Description</span><strong>{{ payment.description || '—' }}</strong></div>
            <div class="detail-row"><span>Attempts</span><strong>{{ payment.attemptCount }}</strong></div>
          </div>
        </div>

        <!-- Decline Info (if failed) -->
        <div class="card">
          <div class="card-header"><h3>{{ payment.status === 'FAILED' ? 'Decline Information' : 'Customer Info' }}</h3></div>
          <div class="detail-grid" *ngIf="payment.status === 'FAILED'">
            <div class="detail-row"><span>Decline Reason</span><span class="badge failed">{{ payment.declineReason }}</span></div>
            <div class="detail-row"><span>Message</span><strong>{{ payment.declineMessage || '—' }}</strong></div>
          </div>
          <div class="detail-grid">
            <div class="detail-row"><span>Customer</span><strong>{{ payment.customerName || '—' }}</strong></div>
            <div class="detail-row"><span>Email</span><strong>{{ payment.customerEmail || '—' }}</strong></div>
          </div>
        </div>
      </div>

      <!-- Routing Attempts Timeline -->
      <div class="card">
        <div class="card-header"><h3>Routing Attempts Timeline</h3></div>
        <table class="data-table" *ngIf="payment.routingAttempts?.length">
          <thead>
            <tr><th>#</th><th>Provider</th><th>Result</th><th>Decline Reason</th><th>Latency</th><th>Time</th></tr>
          </thead>
          <tbody>
            <tr *ngFor="let a of payment.routingAttempts">
              <td>{{ a.attemptNumber }}</td>
              <td><strong>{{ a.providerCode }}</strong></td>
              <td><span class="badge" [ngClass]="a.success ? 'success' : 'failed'">{{ a.success ? 'SUCCESS' : 'FAILED' }}</span></td>
              <td>{{ a.declineReason || '—' }}</td>
              <td>{{ a.latencyMs }}ms</td>
              <td style="font-size: 12px; color: var(--text-secondary);">{{ a.createdAt | date:'medium' }}</td>
            </tr>
          </tbody>
        </table>
        <div *ngIf="!payment.routingAttempts?.length" style="color: var(--text-muted); text-align: center; padding: 20px;">
          No routing attempts recorded.
        </div>
      </div>
    </div>

    <div *ngIf="!payment" style="text-align: center; padding: 60px; color: var(--text-muted);">
      Loading payment details...
    </div>
  `,
  styles: [`
    .detail-grid { display: flex; flex-direction: column; gap: 12px; }
    .detail-row {
      display: flex; justify-content: space-between; align-items: center;
      padding: 8px 0; border-bottom: 1px solid var(--border-color);
      span:first-child { color: var(--text-muted); font-size: 13px; }
    }
  `]
})
export class PaymentDetailComponent implements OnInit {
  payment: any = null;

  constructor(private api: ApiService, private route: ActivatedRoute) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.api.getPayment(id).subscribe({
      next: (data) => this.payment = data,
      error: (err) => console.error('Failed to load payment:', err)
    });
  }
}
