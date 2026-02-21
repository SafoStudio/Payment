## Wallets Table (Core Table)

### Structure:
- **id** - unique wallet identifier
- **owner_id** - owner ID (e.g., user ID in your system)
- **balance** - balance with 4 decimal places
- **currency** - currency (USD, EUR, etc.)
- **status** - wallet status
- **version** - for optimistic locking
- **timestamps** - creation and update time

### Constraints:
- `balance >= 0` - balance cannot be negative
- `currency` - only 3 uppercase letters
- `status` - only allowed values

### Indexes for fast search:
- by owner
- by status
- by creation date

---

## Transactions Table (Operations Journal)
> **Immutable table** - records are only added, never deleted or modified.

### Structure:
- **debit_wallet_id** - where money is debited from
- **credit_wallet_id** - where money is credited to
- **amount** - amount (must be positive)
- **type** - operation type
- **reference_id** - for transaction linking (e.g., purchase refund)
- **timestamps** - transaction time

### Important constraints:
- `debit_wallet_id != credit_wallet_id` - cannot transfer to self
- **Transaction types:** TRANSFER, TOP_UP, WITHDRAWAL, REFUND

### Indexes for fast search:
- by debit wallet (with time sorting)
- by credit wallet (with time sorting)
- by creation time
- by reference ID (for linked transactions)

---

## Idempotency Keys Table (Duplicate Protection)
> Prevents double debiting when client sends request twice (e.g., network issues)

### Structure:
- **idempotency_key** - unique client-provided key
- **response** - cached operation result
- **expires_at** - when the key expires
- **created_at** - when the key was created

### How it works:
- Client sends request with unique key
- System checks if key exists in table
- If exists - returns saved response
- If not - performs operation and saves result

### Indexes for fast search:
- by expiration time (for cleanup)

---

## System Wallets (Service Accounts)
Two system wallets are created by default:
- **SYSTEM_REVENUE** - for user deposits
- **SYSTEM_FEES** - for fee collection

---

## Audit Log Table (Complete Change Journal)
> Tracks all changes for security and debugging

### Structure:
- **table_name** - which table changed
- **record_id** - which record
- **action** - INSERT/UPDATE/DELETE
- **old_data** - previous state
- **new_data** - new state
- **created_by** - who made the change
- **created_at** - when it happened

### What it records:
- Which table changed
- Which record
- Which action
- Old and new data
- Who made the change

### Indexes for fast search:
- by table and record (with time sorting)

---

## Overall System Architecture

### Operating Principles:
- **ACID transactions** - all operations are atomic
- **Immutability** - transaction history is unchangeable
- **Idempotency** - duplicate protection
- **Audit** - complete traceability
- **Optimistic locking** - via version field

### Typical Money Transfer Flow:
1. Check idempotency key
2. Begin transaction
3. Lock wallets with version check
4. Check balance
5. Update balances
6. Create record in transactions
7. Write to audit_log
8. Save result in idempotency_keys
9. Commit transaction

### Security Features:
- All changes are logged
- Money cannot be created "from thin air" (every operation has source and recipient)
- Balances always reconcile with transaction journal
- Protection against duplicate operations
