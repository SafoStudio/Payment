export type Maybe<T> = T | null;
export type InputMaybe<T> = Maybe<T>;
/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
  ID: { input: string; output: string; }
  String: { input: string; output: string; }
  Boolean: { input: boolean; output: boolean; }
  Int: { input: number; output: number; }
  Float: { input: number; output: number; }
};

export type CreateFeeRuleInput = {
  currency?: InputMaybe<Scalars['String']['input']>;
  feeType: FeeType;
  fixedAmount?: InputMaybe<Scalars['String']['input']>;
  fromWalletId?: InputMaybe<Scalars['ID']['input']>;
  isActive?: InputMaybe<Scalars['Boolean']['input']>;
  maxAmount?: InputMaybe<Scalars['String']['input']>;
  minAmount?: InputMaybe<Scalars['String']['input']>;
  name: Scalars['String']['input'];
  percentage?: InputMaybe<Scalars['String']['input']>;
  toWalletId?: InputMaybe<Scalars['ID']['input']>;
  transactionType: Scalars['String']['input'];
};

export type CreatePaymentInput = {
  amount: Scalars['String']['input'];
  currency: Scalars['String']['input'];
  description?: InputMaybe<Scalars['String']['input']>;
  idempotencyKey: Scalars['String']['input'];
  targetWalletId: Scalars['ID']['input'];
  walletId: Scalars['ID']['input'];
};

export type CreateRefundInput = {
  amount: Scalars['String']['input'];
  currency: Scalars['String']['input'];
  idempotencyKey: Scalars['String']['input'];
  paymentId: Scalars['ID']['input'];
  reason?: InputMaybe<Scalars['String']['input']>;
};

export type CreateUserInput = {
  email: Scalars['String']['input'];
  firstName: Scalars['String']['input'];
  lastName: Scalars['String']['input'];
  phone?: InputMaybe<Scalars['String']['input']>;
  roleCodes: Array<Scalars['String']['input']>;
};

export type CreateWalletInput = {
  currency: Scalars['String']['input'];
  initialBalance?: InputMaybe<Scalars['String']['input']>;
  userId: Scalars['ID']['input'];
};

export type Fee = {
  __typename?: 'Fee';
  amount: Scalars['String']['output'];
  calculationRule?: Maybe<Scalars['String']['output']>;
  createdAt: Scalars['String']['output'];
  currency: Scalars['String']['output'];
  fromWalletId: Scalars['ID']['output'];
  id: Scalars['ID']['output'];
  percentage?: Maybe<Scalars['String']['output']>;
  toWalletId: Scalars['ID']['output'];
  transactionId?: Maybe<Scalars['ID']['output']>;
  type: FeeType;
};

export type FeeInput = {
  amount: Scalars['String']['input'];
  calculationRule?: InputMaybe<Scalars['String']['input']>;
  currency: Scalars['String']['input'];
  fromWalletId: Scalars['ID']['input'];
  percentage?: InputMaybe<Scalars['String']['input']>;
  toWalletId: Scalars['ID']['input'];
  transactionId?: InputMaybe<Scalars['ID']['input']>;
  type: Scalars['String']['input'];
};

export type FeeRule = {
  __typename?: 'FeeRule';
  createdAt: Scalars['String']['output'];
  currency?: Maybe<Scalars['String']['output']>;
  feeType: FeeType;
  fixedAmount?: Maybe<Scalars['String']['output']>;
  fromWalletId?: Maybe<Scalars['ID']['output']>;
  id: Scalars['ID']['output'];
  isActive: Scalars['Boolean']['output'];
  maxAmount?: Maybe<Scalars['String']['output']>;
  minAmount?: Maybe<Scalars['String']['output']>;
  name: Scalars['String']['output'];
  percentage?: Maybe<Scalars['String']['output']>;
  toWalletId?: Maybe<Scalars['ID']['output']>;
  transactionType: Scalars['String']['output'];
  updatedAt: Scalars['String']['output'];
};

export type FeeType =
  | 'FIXED'
  | 'MIXED'
  | 'PERCENTAGE'
  | 'TIERED';

export type Mutation = {
  __typename?: 'Mutation';
  _empty?: Maybe<Scalars['Boolean']['output']>;
  activateUser: User;
  addRole: User;
  assignRoles: User;
  blockUser: User;
  blockWallet: Wallet;
  cancelPayment: Payment;
  closeWallet: Wallet;
  createFee: Fee;
  createFeeRule: FeeRule;
  createPayment: Payment;
  createRefund: Refund;
  createUser: User;
  createWallet: Wallet;
  deleteFeeRule: Scalars['Boolean']['output'];
  deleteUser: User;
  processPayment: Payment;
  processRefund: Refund;
  removeRole: User;
  topUp: Transaction;
  transfer: Transaction;
  unblockWallet: Wallet;
  updateFeeRule: FeeRule;
  updateUser: User;
};


export type MutationActivateUserArgs = {
  id: Scalars['ID']['input'];
};


export type MutationAddRoleArgs = {
  roleCode: Scalars['String']['input'];
  userId: Scalars['ID']['input'];
};


export type MutationAssignRolesArgs = {
  roleCodes: Array<Scalars['String']['input']>;
  userId: Scalars['ID']['input'];
};


export type MutationBlockUserArgs = {
  id: Scalars['ID']['input'];
};


export type MutationBlockWalletArgs = {
  id: Scalars['ID']['input'];
};


export type MutationCancelPaymentArgs = {
  id: Scalars['ID']['input'];
};


export type MutationCloseWalletArgs = {
  id: Scalars['ID']['input'];
};


export type MutationCreateFeeArgs = {
  input: FeeInput;
};


export type MutationCreateFeeRuleArgs = {
  input: CreateFeeRuleInput;
};


export type MutationCreatePaymentArgs = {
  input: CreatePaymentInput;
};


export type MutationCreateRefundArgs = {
  input: CreateRefundInput;
};


export type MutationCreateUserArgs = {
  input: CreateUserInput;
};


export type MutationCreateWalletArgs = {
  input: CreateWalletInput;
};


export type MutationDeleteFeeRuleArgs = {
  id: Scalars['ID']['input'];
};


export type MutationDeleteUserArgs = {
  id: Scalars['ID']['input'];
};


export type MutationProcessPaymentArgs = {
  id: Scalars['ID']['input'];
};


export type MutationProcessRefundArgs = {
  id: Scalars['ID']['input'];
};


export type MutationRemoveRoleArgs = {
  roleCode: Scalars['String']['input'];
  userId: Scalars['ID']['input'];
};


export type MutationTopUpArgs = {
  input: TopUpInput;
};


export type MutationTransferArgs = {
  input: TransferInput;
};


export type MutationUnblockWalletArgs = {
  id: Scalars['ID']['input'];
};


export type MutationUpdateFeeRuleArgs = {
  input: UpdateFeeRuleInput;
};


export type MutationUpdateUserArgs = {
  id: Scalars['ID']['input'];
  input: UpdateUserInput;
};

export type Payment = {
  __typename?: 'Payment';
  amount: Scalars['String']['output'];
  createdAt: Scalars['String']['output'];
  currency: Scalars['String']['output'];
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  idempotencyKey?: Maybe<Scalars['String']['output']>;
  status: PaymentStatus;
  targetWalletId: Scalars['ID']['output'];
  transactionId?: Maybe<Scalars['ID']['output']>;
  type: PaymentType;
  updatedAt: Scalars['String']['output'];
  walletId: Scalars['ID']['output'];
};

export type PaymentFilterInput = {
  currency?: InputMaybe<Scalars['String']['input']>;
  status?: InputMaybe<PaymentStatus>;
  type?: InputMaybe<PaymentType>;
};

export type PaymentPage = {
  __typename?: 'PaymentPage';
  hasMore: Scalars['Boolean']['output'];
  items: Array<Payment>;
  nextCursor?: Maybe<Scalars['String']['output']>;
};

export type PaymentStatus =
  | 'CANCELLED'
  | 'FAILED'
  | 'PENDING'
  | 'PROCESSING'
  | 'REFUNDED'
  | 'SUCCEEDED';

export type PaymentType =
  | 'PAYMENT'
  | 'REFUND'
  | 'TOP_UP'
  | 'TRANSFER'
  | 'WITHDRAWAL';

export type Query = {
  __typename?: 'Query';
  _empty?: Maybe<Scalars['Boolean']['output']>;
  activeUsers: Array<User>;
  feeRule?: Maybe<FeeRule>;
  feeRules: Array<FeeRule>;
  feeRulesByType: Array<FeeRule>;
  feesByTransaction: Array<Fee>;
  feesByWallet: Array<Fee>;
  me: User;
  payment: Payment;
  paymentByIdempotencyKey?: Maybe<Payment>;
  payments: PaymentPage;
  paymentsByWallet: Array<Payment>;
  refund?: Maybe<Refund>;
  refundsByPayment: Array<Refund>;
  transaction: Transaction;
  user: User;
  userByEmail: User;
  users: Array<User>;
  usersByRole: Array<User>;
  wallet: Wallet;
  walletTransactions: Array<Transaction>;
  wallets: Array<Wallet>;
  walletsByUserId: Array<Wallet>;
};


export type QueryFeeRuleArgs = {
  id: Scalars['ID']['input'];
};


export type QueryFeeRulesByTypeArgs = {
  transactionType: Scalars['String']['input'];
};


export type QueryFeesByTransactionArgs = {
  transactionId: Scalars['ID']['input'];
};


export type QueryFeesByWalletArgs = {
  walletId: Scalars['ID']['input'];
};


export type QueryPaymentArgs = {
  id: Scalars['ID']['input'];
};


export type QueryPaymentByIdempotencyKeyArgs = {
  key: Scalars['String']['input'];
};


export type QueryPaymentsArgs = {
  after?: InputMaybe<Scalars['String']['input']>;
  filter?: InputMaybe<PaymentFilterInput>;
  limit?: InputMaybe<Scalars['Int']['input']>;
};


export type QueryPaymentsByWalletArgs = {
  walletId: Scalars['ID']['input'];
};


export type QueryRefundArgs = {
  id: Scalars['ID']['input'];
};


export type QueryRefundsByPaymentArgs = {
  paymentId: Scalars['ID']['input'];
};


export type QueryTransactionArgs = {
  id: Scalars['ID']['input'];
};


export type QueryUserArgs = {
  id: Scalars['ID']['input'];
};


export type QueryUserByEmailArgs = {
  email: Scalars['String']['input'];
};


export type QueryUsersByRoleArgs = {
  roleCode: Scalars['String']['input'];
};


export type QueryWalletArgs = {
  id: Scalars['ID']['input'];
};


export type QueryWalletTransactionsArgs = {
  limit: Scalars['Int']['input'];
  walletId: Scalars['ID']['input'];
};


export type QueryWalletsByUserIdArgs = {
  userId: Scalars['ID']['input'];
};

export type Refund = {
  __typename?: 'Refund';
  amount: Scalars['String']['output'];
  createdAt: Scalars['String']['output'];
  currency: Scalars['String']['output'];
  id: Scalars['ID']['output'];
  originalTransactionId: Scalars['ID']['output'];
  paymentId: Scalars['ID']['output'];
  reason?: Maybe<Scalars['String']['output']>;
  refundTransactionId?: Maybe<Scalars['ID']['output']>;
  status: RefundStatus;
  updatedAt: Scalars['String']['output'];
};

export type RefundStatus =
  | 'COMPLETED'
  | 'FAILED'
  | 'PENDING'
  | 'PROCESSING'
  | 'REJECTED';

export type TopUpInput = {
  amount: Scalars['String']['input'];
  currency: Scalars['String']['input'];
  description?: InputMaybe<Scalars['String']['input']>;
  idempotencyKey: Scalars['String']['input'];
  walletId: Scalars['ID']['input'];
};

export type Transaction = {
  __typename?: 'Transaction';
  amount: Scalars['String']['output'];
  createdAt: Scalars['String']['output'];
  creditWalletId: Scalars['ID']['output'];
  currency: Scalars['String']['output'];
  debitWalletId: Scalars['ID']['output'];
  description?: Maybe<Scalars['String']['output']>;
  id: Scalars['ID']['output'];
  referenceId?: Maybe<Scalars['ID']['output']>;
  status: TransactionStatus;
  type: TransactionType;
};

export type TransactionStatus =
  | 'CANCELLED'
  | 'COMPLETED'
  | 'FAILED'
  | 'PENDING';

export type TransactionType =
  | 'REFUND'
  | 'TOP_UP'
  | 'TRANSFER'
  | 'WITHDRAWAL';

export type TransferInput = {
  amount: Scalars['String']['input'];
  currency: Scalars['String']['input'];
  description?: InputMaybe<Scalars['String']['input']>;
  fromWalletId: Scalars['ID']['input'];
  idempotencyKey: Scalars['String']['input'];
  toWalletId: Scalars['ID']['input'];
};

export type UpdateFeeRuleInput = {
  currency?: InputMaybe<Scalars['String']['input']>;
  feeType?: InputMaybe<FeeType>;
  fixedAmount?: InputMaybe<Scalars['String']['input']>;
  fromWalletId?: InputMaybe<Scalars['ID']['input']>;
  id: Scalars['ID']['input'];
  isActive?: InputMaybe<Scalars['Boolean']['input']>;
  maxAmount?: InputMaybe<Scalars['String']['input']>;
  minAmount?: InputMaybe<Scalars['String']['input']>;
  name?: InputMaybe<Scalars['String']['input']>;
  percentage?: InputMaybe<Scalars['String']['input']>;
  toWalletId?: InputMaybe<Scalars['ID']['input']>;
  transactionType?: InputMaybe<Scalars['String']['input']>;
};

export type UpdateUserInput = {
  email?: InputMaybe<Scalars['String']['input']>;
  firstName?: InputMaybe<Scalars['String']['input']>;
  lastName?: InputMaybe<Scalars['String']['input']>;
  phone?: InputMaybe<Scalars['String']['input']>;
};

export type User = {
  __typename?: 'User';
  createdAt: Scalars['String']['output'];
  email: Scalars['String']['output'];
  firstName: Scalars['String']['output'];
  fullName: Scalars['String']['output'];
  hasPasskey?: Maybe<Scalars['Boolean']['output']>;
  id: Scalars['ID']['output'];
  lastName: Scalars['String']['output'];
  permissions: Array<Scalars['String']['output']>;
  phone?: Maybe<Scalars['String']['output']>;
  roles: Array<Scalars['String']['output']>;
  status: UserStatus;
  updatedAt: Scalars['String']['output'];
  wallets: Array<Wallet>;
};

export type UserStatus =
  | 'ACTIVE'
  | 'BLOCKED'
  | 'DELETED';

export type Wallet = {
  __typename?: 'Wallet';
  balance: Scalars['String']['output'];
  createdAt: Scalars['String']['output'];
  currency: Scalars['String']['output'];
  id: Scalars['ID']['output'];
  status: WalletStatus;
  updatedAt: Scalars['String']['output'];
  userId: Scalars['ID']['output'];
};

export type WalletStatus =
  | 'ACTIVE'
  | 'BLOCKED'
  | 'CLOSED';
