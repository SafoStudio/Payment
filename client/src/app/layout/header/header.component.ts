import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { NavMenuComponent } from '../nav-menu/nav-menu.component';
import { UserMenuComponent } from '../user-menu/user-menu.component';
import { ThemeService } from '@core/theme/theme.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [NavMenuComponent, UserMenuComponent],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HeaderComponent {
  private readonly theme = inject(ThemeService);

  readonly isDark = this.theme.isDark;

  toggleTheme(): void {
    this.theme.toggle();
  }
}
