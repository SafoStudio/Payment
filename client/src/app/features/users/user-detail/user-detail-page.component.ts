import {
  ChangeDetectionStrategy,
  Component,
  type OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { type DocumentNode } from 'graphql';
import { gqlClient } from '@core/graphql/client';
import {
  ActivateUserDocument,
  AssignRolesDocument,
  BlockUserDocument,
  DeleteUserDocument,
  UpdateUserDocument,
  UserDocument,
  type UserQuery,
} from '@core/graphql/graphql';
import { toAppError, type AppError } from '@core/graphql/error-handler';
import { AuthService } from '@core/auth/auth.service';

type UserDetail = UserQuery['user'];

const ALL_ROLES = ['ADMIN', 'MERCHANT', 'SUPPORT'] as const;

@Component({
  selector: 'app-user-detail-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './user-detail-page.component.html',
  styleUrl: './user-detail-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserDetailPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly userId = this.route.snapshot.paramMap.get('id') ?? '';
  readonly allRoles = ALL_ROLES;

  private readonly _user = signal<UserDetail | null>(null);
  private readonly _loading = signal(true);
  private readonly _error = signal<AppError | null>(null);

  private readonly _busy = signal(false);
  private readonly _actionError = signal<string | null>(null);

  private readonly _emailDraft = signal('');
  private readonly _phoneDraft = signal('');
  private readonly _firstNameDraft = signal('');
  private readonly _lastNameDraft = signal('');
  private readonly _editDirty = signal(false);

  readonly user = this._user.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly busy = this._busy.asReadonly();
  readonly actionError = this._actionError.asReadonly();
  readonly emailDraft = this._emailDraft.asReadonly();
  readonly phoneDraft = this._phoneDraft.asReadonly();
  readonly firstNameDraft = this._firstNameDraft.asReadonly();
  readonly lastNameDraft = this._lastNameDraft.asReadonly();
  readonly editDirty = this._editDirty.asReadonly();

  readonly canUpdate = computed(() => this.auth.hasPermission('user:update'));
  readonly canBlock = computed(() => this.auth.hasPermission('user:block'));
  readonly canAssignRole = computed(() => this.auth.hasPermission('user:assign_role'));
  readonly isActive = computed(() => this._user()?.status === 'ACTIVE');
  readonly isBlocked = computed(() => this._user()?.status === 'BLOCKED');
  readonly isDeleted = computed(() => this._user()?.status === 'DELETED');

  ngOnInit(): void {
    void this.reload();
  }

  async reload(): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const result = await gqlClient
        .query(UserDocument, { id: this.userId }, { requestPolicy: 'network-only' })
        .toPromise();
      if (result.error) {
        this._error.set(toAppError(result.error));
      } else if (result.data?.user) {
        this.applyUser(result.data.user);
      }
    } catch (e) {
      this._error.set(toAppError(e));
    } finally {
      this._loading.set(false);
    }
  }

  onFieldInput(field: 'email' | 'phone' | 'firstName' | 'lastName', value: string): void {
    switch (field) {
      case 'email':
        this._emailDraft.set(value);
        break;
      case 'phone':
        this._phoneDraft.set(value);
        break;
      case 'firstName':
        this._firstNameDraft.set(value);
        break;
      case 'lastName':
        this._lastNameDraft.set(value);
        break;
    }
    this._editDirty.set(true);
  }

  async saveProfile(): Promise<void> {
    if (this._busy() || !this._editDirty()) return;
    this._busy.set(true);
    this._actionError.set(null);
    try {
      const u = this._user();
      if (!u) return;

      const input: Record<string, string> = {};
      if (this._emailDraft() !== u.email) input['email'] = this._emailDraft();
      if (this._phoneDraft() !== (u.phone ?? '')) input['phone'] = this._phoneDraft();
      if (this._firstNameDraft() !== u.firstName) input['firstName'] = this._firstNameDraft();
      if (this._lastNameDraft() !== u.lastName) input['lastName'] = this._lastNameDraft();

      if (Object.keys(input).length === 0) {
        this._editDirty.set(false);
        return;
      }

      const result = await gqlClient
        .mutation(UpdateUserDocument, { id: this.userId, input })
        .toPromise();

      if (result.error) {
        this._actionError.set(toAppError(result.error).message);
      } else {
        await this.reload();
      }
    } catch (e) {
      this._actionError.set(toAppError(e).message);
    } finally {
      this._busy.set(false);
    }
  }

  async changeStatus(kind: 'block' | 'activate' | 'delete'): Promise<void> {
    if (this._busy()) return;
    this._busy.set(true);
    this._actionError.set(null);
    try {
      let doc: DocumentNode;
      switch (kind) {
        case 'block':
          doc = BlockUserDocument;
          break;
        case 'activate':
          doc = ActivateUserDocument;
          break;
        case 'delete':
          doc = DeleteUserDocument;
          break;
      }

      const result = await gqlClient
        .mutation(doc, { id: this.userId })
        .toPromise();

      if (result.error) {
        this._actionError.set(toAppError(result.error).message);
      } else {
        await this.reload();
      }
    } catch (e) {
      this._actionError.set(toAppError(e).message);
    } finally {
      this._busy.set(false);
    }
  }

  hasRole(code: string): boolean {
    return this._user()?.roles.includes(code) ?? false;
  }

  async toggleRole(code: string): Promise<void> {
    if (this._busy()) return;
    const u = this._user();
    if (!u) return;

    const current = new Set(u.roles);
    if (current.has(code)) {
      current.delete(code);
    } else {
      current.add(code);
    }

    if (current.size === 0) {
      this._actionError.set('A user must have at least one role.');
      return;
    }

    this._busy.set(true);
    this._actionError.set(null);
    try {
      const result = await gqlClient
        .mutation(AssignRolesDocument, {
          userId: this.userId,
          roleCodes: [...current],
        })
        .toPromise();

      if (result.error) {
        this._actionError.set(toAppError(result.error).message);
      } else {
        await this.reload();
      }
    } catch (e) {
      this._actionError.set(toAppError(e).message);
    } finally {
      this._busy.set(false);
    }
  }

  formatAmount(amount: string, currency: string): string {
    const value = Number(amount);
    if (!Number.isFinite(value)) return amount;
    return new Intl.NumberFormat(undefined, {
      style: 'currency',
      currency,
    }).format(value);
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleString(undefined, {
      year: 'numeric',
      month: 'short',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  statusClass(status: string): string {
    switch (status) {
      case 'ACTIVE':
        return 'user-detail__status--active';
      case 'BLOCKED':
        return 'user-detail__status--blocked';
      case 'DELETED':
        return 'user-detail__status--deleted';
      default:
        return '';
    }
  }

  goBack(): void {
    void this.router.navigate(['/users']);
  }

  private applyUser(u: UserDetail): void {
    this._user.set(u);
    this._emailDraft.set(u.email);
    this._phoneDraft.set(u.phone ?? '');
    this._firstNameDraft.set(u.firstName);
    this._lastNameDraft.set(u.lastName);
    this._editDirty.set(false);
  }
}
