import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'pgw-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="app-layout">
      <!-- Slim Sidebar Navigation -->
      <aside class="sidebar">
        <!-- Logo (Yuno letter-T purple-blue shield button) -->
        <div class="logo">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 2L2 7l10 5 10-5-10-5z"></path>
            <path d="M2 17l10 5 10-5"></path>
            <path d="M2 12l10 5 10-5"></path>
          </svg>
        </div>
        
        <nav>
          <!-- Dashboard link -->
          <a class="nav-link" routerLink="/dashboard" routerLinkActive="active" title="Dashboard">
            <svg viewBox="0 0 24 24"><rect x="3" y="3" width="7" height="9" rx="1"></rect><rect x="14" y="3" width="7" height="5" rx="1"></rect><rect x="14" y="12" width="7" height="9" rx="1"></rect><rect x="3" y="16" width="7" height="5" rx="1"></rect></svg>
          </a>
          <!-- Payments link -->
          <a class="nav-link" routerLink="/payments" routerLinkActive="active" title="Payments">
            <svg viewBox="0 0 24 24"><rect x="1" y="4" width="22" height="16" rx="2" ry="2"></rect><line x1="1" y1="10" x2="23" y2="10"></line></svg>
          </a>
          <!-- Routing link -->
          <a class="nav-link" routerLink="/routing" routerLinkActive="active" title="Routing & Gateways">
            <svg viewBox="0 0 24 24"><polyline points="16 3 21 3 21 8"></polyline><line x1="4" y1="20" x2="21" y2="3"></line><polyline points="21 16 21 21 16 21"></polyline><line x1="15" y1="15" x2="21" y2="21"></line><line x1="4" y1="4" x2="9" y2="9"></line></svg>
          </a>
          <!-- Analytics link -->
          <a class="nav-link" routerLink="/analytics" routerLinkActive="active" title="Insights">
            <svg viewBox="0 0 24 24"><line x1="18" y1="20" x2="18" y2="10"></line><line x1="12" y1="20" x2="12" y2="4"></line><line x1="6" y1="20" x2="6" y2="14"></line></svg>
          </a>
          <!-- Compliance link -->
          <a class="nav-link" routerLink="/compliance" routerLinkActive="active" title="Security & Compliance">
            <svg viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg>
          </a>
        </nav>
        
        <!-- User avatar bubble at bottom -->
        <div style="margin-top: auto; cursor: pointer; display: flex; flex-direction: column; gap: 16px; align-items: center;">
          <div style="width: 32px; height: 32px; border-radius: 50%; background: #eff6ff; color: #3b52f6; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 700;">A</div>
        </div>
      </aside>

      <!-- Main Wrapper -->
      <div class="main-wrapper">
        <header class="top-header">
          <div class="header-left">
            <!-- Dynamic context title or clean breadcrumb structure -->
            <div style="display: flex; align-items: center; gap: 8px; color: #64748b; font-size: 13.5px; font-weight: 500;">
              <span>Workspace</span>
              <span>/</span>
              <span style="color: #0f172a; font-weight: 600;">Active Session</span>
            </div>
          </div>
          <div class="header-right">
            <!-- Test Mode Switch Toggle (Yuno Style) -->
            <div style="display: flex; align-items: center; gap: 10px;">
              <span style="font-size: 13px; font-weight: 600; color: #555a6e;">Test mode</span>
              <label class="switch">
                <input type="checkbox" checked>
                <span class="slider"></span>
              </label>
            </div>
            
            <span style="width: 1px; height: 20px; background: #e2e8f0;"></span>
            
            <!-- Search Icon -->
            <button style="background: none; border: none; cursor: pointer; color: #64748b;" title="Search">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
            </button>
            
            <!-- Grid Layout Selector Icon -->
            <button style="background: none; border: none; cursor: pointer; color: #64748b;" title="Grid view">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
            </button>

            <!-- Alerts Notification Bell -->
            <div style="position: relative; cursor: pointer; color: #64748b;" title="Notifications">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>
              <span style="position: absolute; top: 0; right: 0; background: #3b52f6; border: 2px solid white; border-radius: 50%; width: 9px; height: 9px;"></span>
            </div>
            
            <!-- Globe Globe Profile -->
            <button style="background: none; border: none; cursor: pointer; color: #3b52f6;" title="Active Environment">
              <div style="width: 28px; height: 28px; border-radius: 50%; background: #eff6ff; display: flex; align-items: center; justify-content: center;">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="2" y1="12" x2="22" y2="12"></line><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"></path></svg>
              </div>
            </button>
          </div>
        </header>
        
        <main class="main-content">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
  styles: [`
    :host { display: block; min-height: 100vh; }
  `]
})
export class AppComponent {}
