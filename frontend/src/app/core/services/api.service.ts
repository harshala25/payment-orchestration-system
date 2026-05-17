import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

const API_BASE = 'http://localhost:8080/api/v1';
const API_KEY = 'pgw_live_sk_yuno_2026_assessment_key';

@Injectable({ providedIn: 'root' })
export class ApiService {

  private headers = new HttpHeaders({
    'X-API-Key': API_KEY,
    'Content-Type': 'application/json'
  });

  constructor(private http: HttpClient) {}

  // === Payments ===
  createPayment(payment: any, idempotencyKey: string): Observable<any> {
    return this.http.post(`${API_BASE}/payments`, payment, {
      headers: this.headers.set('Idempotency-Key', idempotencyKey)
    });
  }

  getPayment(id: string): Observable<any> {
    return this.http.get(`${API_BASE}/payments/${id}`, { headers: this.headers });
  }

  listPayments(page = 0, size = 20, status?: string, merchantId?: string): Observable<any> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    if (merchantId) params = params.set('merchantId', merchantId);
    return this.http.get(`${API_BASE}/payments`, { headers: this.headers, params });
  }

  refundPayment(id: string): Observable<any> {
    return this.http.post(`${API_BASE}/payments/${id}/refund`, {}, { headers: this.headers });
  }

  // === Routing ===
  getRoutingRules(): Observable<any> {
    return this.http.get(`${API_BASE}/routing/rules`, { headers: this.headers });
  }

  getProviders(): Observable<any> {
    return this.http.get(`${API_BASE}/routing/providers`, { headers: this.headers });
  }

  toggleProvider(code: string, enabled: boolean): Observable<any> {
    return this.http.put(`${API_BASE}/routing/providers/${code}/toggle?enabled=${enabled}`, {}, 
      { headers: this.headers });
  }

  getApprovalRates(): Observable<any> {
    return this.http.get(`${API_BASE}/routing/approval-rates`, { headers: this.headers });
  }

  // === Analytics ===
  getDashboard(hoursBack = 24): Observable<any> {
    return this.http.get(`${API_BASE}/analytics/dashboard?hoursBack=${hoursBack}`, 
      { headers: this.headers });
  }

  // === Compliance ===
  getAuditLogs(page = 0, size = 50): Observable<any> {
    return this.http.get(`${API_BASE}/compliance/audit-log?page=${page}&size=${size}`, 
      { headers: this.headers });
  }

  getSecurityReport(): Observable<any> {
    return this.http.get(`${API_BASE}/compliance/security-report`, { headers: this.headers });
  }
}
