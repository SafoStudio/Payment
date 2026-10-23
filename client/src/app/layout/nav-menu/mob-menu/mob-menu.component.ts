import {
  ChangeDetectionStrategy,
  Component,
  effect,
  input,
  output,
  signal,
} from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import type { NavItem } from '../config/nav.config';

@Component({
  selector: 'app-mob-menu',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './mob-menu.component.html',
  styleUrl: './mob-menu.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MobMenuComponent {
  open = input.required<boolean>();
  items = input.required<readonly NavItem[]>();
  closed = output<void>();

  readonly expanded = signal<string | null>(null);

  constructor() {
    effect(() => {
      if (typeof document === 'undefined') return;
      document.body.style.overflow = this.open() ? 'hidden' : '';
    });
  }

  toggleExpand(id: string): void {
    this.expanded.update((current) => (current === id ? null : id));
  }

  onClose(): void {
    this.expanded.set(null);
    this.closed.emit();
  }
}
