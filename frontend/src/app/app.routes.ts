import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },
  {
    path: 'payments',
    loadComponent: () => import('./features/payments/payments.component').then(m => m.PaymentsComponent)
  },
  {
    path: 'payments/:id',
    loadComponent: () => import('./features/payments/payment-detail.component').then(m => m.PaymentDetailComponent)
  },
  {
    path: 'routing',
    loadComponent: () => import('./features/routing/routing.component').then(m => m.RoutingComponent)
  },
  {
    path: 'analytics',
    loadComponent: () => import('./features/analytics/analytics.component').then(m => m.AnalyticsComponent)
  },
  {
    path: 'compliance',
    loadComponent: () => import('./features/compliance/compliance.component').then(m => m.ComplianceComponent)
  }
];
