import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';

@Component({
  selector: 'pgw-payments',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  template: `
    <div class="page-header">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <h2>Payments</h2>
          <p>View and manage all payment transactions</p>
        </div>
        <button class="btn btn-primary" (click)="showCreateModal = true">+ Create Payment</button>
      </div>
    </div>

    <!-- Filters -->
    <div class="card" style="margin-bottom: 24px;">
      <div style="display: flex; gap: 16px; align-items: center;">
        <select [(ngModel)]="statusFilter" (change)="loadPayments()" style="background: var(--bg-primary); color: var(--text-primary); border: 1px solid var(--border-color); padding: 8px 12px; border-radius: var(--radius-sm);">
          <option value="">All Statuses</option>
          <option value="SUCCESS">Success</option>
          <option value="FAILED">Failed</option>
          <option value="PROCESSING">Processing</option>
          <option value="REFUNDED">Refunded</option>
        </select>
        <button class="btn btn-outline" (click)="loadPayments()">🔄 Refresh</button>
      </div>
    </div>

    <!-- Payments Table -->
    <div class="card fade-in">
      <table class="data-table">
        <thead>
          <tr>
            <th>Payment ID</th>
            <th>Amount</th>
            <th>Method</th>
            <th>Status</th>
            <th>Provider</th>
            <th>Attempts</th>
            <th>Created</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let p of payments">
            <td><a [routerLink]="['/payments', p.id]" style="font-family: monospace; font-size: 12px;">{{ p.id?.substring(0, 8) }}...</a></td>
            <td><strong>{{ p.currency }} {{ p.amount | number:'1.2-2' }}</strong></td>
            <td><span class="badge processing">{{ p.paymentMethod }}</span></td>
            <td><span class="badge" [ngClass]="p.status?.toLowerCase()">{{ p.status }}</span></td>
            <td>{{ p.providerUsed || '—' }}</td>
            <td>{{ p.attemptCount }}</td>
            <td style="font-size: 12px; color: var(--text-secondary);">{{ p.createdAt | date:'short' }}</td>
            <td>
              <button *ngIf="p.status === 'SUCCESS'" class="btn btn-danger" style="font-size: 12px; padding: 4px 10px;"
                      (click)="refund(p.id)">Refund</button>
            </td>
          </tr>
          <tr *ngIf="payments.length === 0">
            <td colspan="8" style="text-align: center; color: var(--text-muted); padding: 40px;">
              No payments found. Create one to get started!
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 16px; padding-top: 16px; border-top: 1px solid var(--border-color);">
        <span style="font-size: 13px; color: var(--text-muted);">Page {{ currentPage + 1 }} of {{ totalPages || 1 }}</span>
        <div style="display: flex; gap: 8px;">
          <button class="btn btn-outline" [disabled]="currentPage === 0" (click)="currentPage = currentPage - 1; loadPayments()">← Prev</button>
          <button class="btn btn-outline" [disabled]="currentPage >= totalPages - 1" (click)="currentPage = currentPage + 1; loadPayments()">Next →</button>
        </div>
      </div>
    </div>

    <!-- Create Payment Modal -->
    <div *ngIf="showCreateModal" style="position: fixed; inset: 0; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 1000;">
      <div class="card" style="width: 500px; max-height: 80vh; overflow-y: auto;">
        <div class="card-header" style="border-bottom: 1px solid #f1f5f9; padding-bottom: 16px; margin-bottom: 20px;">
          <h3 style="font-size: 16px; font-weight: 700; color: #0f172a;">Create Payment</h3>
          <button class="btn-close" (click)="showCreateModal = false">✕</button>
        </div>

        <div style="display: flex; flex-direction: column; gap: 16px;">
          <div>
            <label style="font-size: 12px; color: var(--text-muted); display: block; margin-bottom: 4px;">Amount (INR)</label>
            <input type="number" [(ngModel)]="newPayment.amount" placeholder="1500.00"
                   style="width: 100%; background: var(--bg-primary); color: var(--text-primary); border: 1px solid var(--border-color); padding: 10px; border-radius: var(--radius-sm);">
          </div>
          <div>
            <label style="font-size: 12px; color: var(--text-muted); display: block; margin-bottom: 4px;">Payment Method</label>
            <select [(ngModel)]="newPayment.paymentMethod" style="width: 100%; background: var(--bg-primary); color: var(--text-primary); border: 1px solid var(--border-color); padding: 10px; border-radius: var(--radius-sm);">
              <option value="CARD">💳 Card</option>
              <option value="UPI">📱 UPI</option>
            </select>
          </div>

          <div *ngIf="newPayment.paymentMethod === 'CARD'">
            <label style="font-size: 12px; color: var(--text-muted); display: block; margin-bottom: 4px;">Card Number</label>
            <input type="text" [(ngModel)]="newPayment.cardNumber" placeholder="4242424242424242"
                   style="width: 100%; background: var(--bg-primary); color: var(--text-primary); border: 1px solid var(--border-color); padding: 10px; border-radius: var(--radius-sm);">
          </div>

          <div *ngIf="newPayment.paymentMethod === 'UPI'">
            <label style="font-size: 12px; color: var(--text-muted); display: block; margin-bottom: 4px;">UPI VPA</label>
            <input type="text" [(ngModel)]="newPayment.upiVpa" placeholder="user@paytm"
                   style="width: 100%; background: var(--bg-primary); color: var(--text-primary); border: 1px solid var(--border-color); padding: 10px; border-radius: var(--radius-sm);">
          </div>

          <div>
            <label style="font-size: 12px; color: var(--text-muted); display: block; margin-bottom: 4px;">Description</label>
            <input type="text" [(ngModel)]="newPayment.description" placeholder="Order #12345"
                   style="width: 100%; background: var(--bg-primary); color: var(--text-primary); border: 1px solid var(--border-color); padding: 10px; border-radius: var(--radius-sm);">
          </div>

          <button class="btn btn-primary" style="width: 100%; padding: 12px;" (click)="createPayment()" [disabled]="creating">
            {{ creating ? 'Processing...' : 'Create Payment' }}
          </button>

          <div *ngIf="createResult" class="card" style="background: var(--bg-primary); padding: 12px;">
            <div style="font-size: 12px; margin-bottom: 4px;">Result:</div>
            <span class="badge" [ngClass]="createResult.status?.toLowerCase()">{{ createResult.status }}</span>
            <span style="margin-left: 8px; font-size: 13px;">via {{ createResult.providerUsed || 'N/A' }}</span>
          </div>
        </div>
      </div>
    </div>
  `
})
export class PaymentsComponent implements OnInit {
  payments: any[] = [];
  currentPage = 0;
  totalPages = 1;
  statusFilter = '';
  showCreateModal = false;
  creating = false;
  createResult: any = null;

  newPayment: any = {
    merchantId: 'merchant_001',
    amount: 1500,
    currency: 'INR',
    paymentMethod: 'CARD',
    cardNumber: '4242424242424242',
    cardExpiryMonth: '12',
    cardExpiryYear: '2028',
    cardCvv: '123',
    upiVpa: 'user@paytm',
    description: 'Test Payment',
    customerEmail: 'test@example.com'
  };

  constructor(private api: ApiService) {}

  ngOnInit() { this.loadPayments(); }

  loadPayments() {
    this.api.listPayments(this.currentPage, 20, this.statusFilter || undefined).subscribe({
      next: (data) => {
        this.payments = data.content || [];
        this.totalPages = data.totalPages || 1;
      },
      error: (err) => console.error('Failed to load payments:', err)
    });
  }

  createPayment() {
    this.creating = true;
    this.createResult = null;
    const key = 'idem-' + Date.now() + '-' + Math.random().toString(36).substring(7);
    this.api.createPayment(this.newPayment, key).subscribe({
      next: (res) => { this.createResult = res; this.creating = false; this.loadPayments(); },
      error: (err) => { this.creating = false; console.error('Create failed:', err); }
    });
  }

  refund(id: string) {
    if (confirm('Are you sure you want to refund this payment?')) {
      this.api.refundPayment(id).subscribe({
        next: () => this.loadPayments(),
        error: (err) => console.error('Refund failed:', err)
      });
    }
  }
}
