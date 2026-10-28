export interface NavLink {
  label: string;
  link: string;
  external?: boolean;
}

export interface NavSection {
  title: string;
  hideTitle?: boolean;
  links: NavLink[];
}

export interface NavItem {
  id: string;
  label: string;
  icon: string;
  permissions: readonly string[];
  link?: string;
  sections?: NavSection[];
}

export const NAV_ITEMS: readonly NavItem[] = [
  {
    id: 'dashboard',
    label: 'Dashboard',
    icon: '⊞',
    permissions: ['dashboard:read'],
    link: '/dashboard',
  },
  {
    id: 'payments',
    label: 'Payments',
    icon: '⇄',
    permissions: ['payment:manage:own', 'payment:read:all'],
    sections: [
      {
        title: 'Overview',
        hideTitle: true,
        links: [
          { label: 'All payments', link: '/payments' },
          { label: 'Transactions', link: '/transactions' },
          { label: 'Refunds', link: '/refunds' },
          { label: 'Disputes', link: '/dashboard' },
        ],
      },
    ],
  },
  {
    id: 'wallets',
    label: 'Wallets',
    icon: '◈',
    permissions: ['wallet:manage:own', 'wallet:read:all'],
    sections: [
      {
        title: 'Balances',
        hideTitle: true,
        links: [
          { label: 'Balances', link: '/wallets' },
          { label: 'Balance history', link: '/dashboard' },
          { label: 'Payouts', link: '/dashboard' },
          { label: 'Top up', link: '/dashboard' },
        ],
      },
    ],
  },
  {
    id: 'providers',
    label: 'Providers',
    icon: '⇋',
    permissions: ['provider:read:own', 'provider:manage:all'],
    sections: [
      {
        title: 'Connected',
        hideTitle: true,
        links: [
          { label: 'Connected providers', link: '/dashboard' },
          { label: 'Catalog', link: '/dashboard' },
        ],
      },
      {
        title: 'Orchestration',
        links: [
          { label: 'Routing rules', link: '/dashboard' },
          { label: 'Health', link: '/dashboard' },
        ],
      },
    ],
  },
  {
    id: 'developers',
    label: 'Developers',
    icon: '⌘',
    permissions: ['developer:read:own'],
    sections: [
      {
        title: 'Integration',
        hideTitle: true,
        links: [
          { label: 'API keys', link: '/dashboard' },
          { label: 'Webhooks', link: '/dashboard' },
          { label: 'Sandbox', link: '/dashboard' },
          { label: 'API logs', link: '/dashboard' },
        ],
      },
      {
        title: 'Resources',
        links: [
          { label: 'Documentation ↗', link: '/dashboard' },
        ],
      },
    ],
  },
  {
    id: 'admin',
    label: 'Admin',
    icon: '☰',
    permissions: ['user:manage:all', 'role:manage:all', 'merchant:manage:all'],
    sections: [
      {
        title: 'Platform',
        hideTitle: true,
        links: [
          { label: 'Merchants', link: '/dashboard' },
          { label: 'Users', link: '/dashboard' },
          { label: 'Roles & permissions', link: '/dashboard' },
        ],
      },
      {
        title: 'Configuration',
        links: [
          { label: 'Fees & tariffs', link: '/dashboard' },
          { label: 'Audit log', link: '/dashboard' },
        ],
      },
    ],
  },
  {
    id: 'settings',
    label: 'Settings',
    icon: '⚙',
    permissions: ['user:read:own'],
    link: '/dashboard',
  },
];
