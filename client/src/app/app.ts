import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { GraphqlService } from './graphql';

interface Wallet {
  id: string;
  currency: string;
  balance: string;
}

@Component({
  imports: [RouterOutlet],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App implements OnInit {
  protected readonly title = signal('client');

  private readonly graphql = inject(GraphqlService);

  protected readonly wallets = signal<Wallet[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.loading.set(true);
    this.graphql
      .query<{ wallets: Wallet[] }>('{ wallets { id currency balance } }')
      .subscribe({
        next: (response) => {
          console.log('✅ GraphQL response:', response);
          this.wallets.set(response.data?.wallets ?? []);
          this.loading.set(false);
        },
        error: (err) => {
          console.error('❌ GraphQL error:', err);
          this.error.set(err.message ?? 'Unknown error');
          this.loading.set(false);
        },
      });
  }
}
