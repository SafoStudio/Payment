import {
  ChangeDetectionStrategy,
  Component,
  inject,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-user-detail-page',
  standalone: true,
  templateUrl: './user-detail-page.component.html',
  styleUrl: './user-detail-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly userId = this.route.snapshot.paramMap.get('id') ?? '';

  back(): void {
    void this.router.navigate(['/users']);
  }
}
