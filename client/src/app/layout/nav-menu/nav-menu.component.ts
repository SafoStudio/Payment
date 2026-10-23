import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  signal,
} from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { NAV_ITEMS, type NavItem } from './config/nav.config';
import { MobMenuComponent } from './mob-menu/mob-menu.component';

@Component({
  selector: 'app-nav-menu',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, MobMenuComponent],
  templateUrl: './nav-menu.component.html',
  styleUrl: './nav-menu.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NavMenuComponent {
  readonly items: readonly NavItem[] = NAV_ITEMS;

  readonly activeMenu = signal<string | null>(null);
  readonly mobileOpen = signal(false);

  private closeTimeout: ReturnType<typeof setTimeout> | null = null;

  onMenuEnter(id: string): void {
    this.cancelClose();
    this.activeMenu.set(id);
  }

  onMenuLeave(): void {
    this.cancelClose();
    this.closeTimeout = setTimeout(() => {
      this.activeMenu.set(null);
      this.closeTimeout = null;
    }, 150);
  }

  onItemClick(item: NavItem): void {
    if (item.sections) {
      this.onMenuEnter(item.id);
    } else {
      this.closeMenu();
    }
  }

  closeMenu(): void {
    this.cancelClose();
    this.activeMenu.set(null);
  }

  toggleMobile(): void {
    this.mobileOpen.update((open) => !open);
  }

  closeMobile(): void {
    this.mobileOpen.set(false);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement | null;
    if (!target) return;
    if (target.closest('app-nav-menu')) return;
    this.closeMenu();
  }

  private cancelClose(): void {
    if (this.closeTimeout) {
      clearTimeout(this.closeTimeout);
      this.closeTimeout = null;
    }
  }
}
