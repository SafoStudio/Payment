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
    id: 'notifications',
    label: 'Notifications',
    icon: '🔔',
    permissions: ['notification:read:own'],
    link: '/notifications',
  },
  {
    id: 'payments',
    label: 'Payments',
    icon: '⇄',
    permissions: ['payment:manage:own', 'payment:read:all'],
    sections: [
      {
        title: 'Payments',
        hideTitle: true,
        links: [
          { label: 'All payments', link: '/payments' },
          { label: 'Refunds', link: '/refunds' },
        ],
      },
    ],
  },
  {
    id: 'transactions',
    label: 'Transactions',
    icon: '⇆',
    permissions: ['transaction:read:own', 'transaction:read:all'],
    link: '/transactions',
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
          { label: 'Payouts', link: '/dashboard' },
        ],
      },
    ],
  },
  {
    id: 'users',
    label: 'Users',
    icon: '☷',
    permissions: ['user:read:all'],
    link: '/users',
  },
  {
    id: 'providers',
    label: 'Providers',
    icon: '⇋',
    permissions: ['provider:read:own', 'provider:manage:all'],
    link: '/providers',
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
    permissions: ['fee:manage:all', 'audit:read:all'],
    sections: [
      {
        title: 'Configuration',
        hideTitle: true,
        links: [
          { label: 'Fees & tariffs', link: '/admin/fees' },
          { label: 'Routing rules', link: '/admin/routing' },
          { label: 'Audit log', link: '/admin/audit' },
        ],
      },
    ],
  },
  {
    id: 'settings',
    label: 'Settings',
    icon: '⚙',
    permissions: ['user:read:own'],
    link: '/settings',
  },
];
