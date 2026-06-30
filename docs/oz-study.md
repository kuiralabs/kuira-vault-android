# OpenZeppelin Compact — Study & Vault Design

A no-gaps study of the vendored OpenZeppelin Compact library (`contract/oz/`,
pinned `v0.3.0-alpha`, commit `c096e317`) as the basis for the Midnight Vault.
Every non-test module was read in full. This doc is the authority the
`Vault.compact` design derives from.

**Toolchain (verified):** compactc `0.31.1`, language `0.23.0`, runtime `0.16.0`,
generated code targets **ledger-8.0.2** (the SDK's bundled 8.0.3 handles it — we
stay on 8.0.3). All 32 real modules compile clean under 0.31.1.

---

## 1. Audit status — read this first

The May 2026 OpenZeppelin audit covers **access, security, token, utils** at
`v0.1.0` only. The **`multisig/` suite is newer and NOT in audit scope.** So
"OZ is audited" is true for tokens/access/security, **not** for the multisig.
Treat multisig modules as good-but-unaudited reference we own and verify.

---

## 2. The auth landscape (the crux)

There is **no single working external-signature multisig** in Compact — Compact
has no ECDSA primitive. There are three real identity schemes and two M-of-N
models, only one of which is non-stubbed and fully working today.

### Identity primitives

| Scheme | Identity | Binding strength | Privacy |
|---|---|---|---|
| `Ownable` / `AccessControl` | `accountId = persistentHash([sk])` (`Utils_computeAccountId`) | **Weak** — witness-only, unsalted, cross-contract linkable | Public membership |
| `ZOwnablePK` | `commitment = H(H(ownPublicKey(), nonce), salt, counter, domain)` | **Strong** — bound to chain-context `ownPublicKey()`, rotates per transfer | Hides PK; single-admin only |
| `ShieldedAccessControl` | `accountId = H(sk, salt, domain)`; role = Merkle leaf; revoke = nullifier | Witness-`sk` based; salt-scoped | Hides role holders; per-call nullifier links one holder's actions over time |

**Key fact (from the access study):** `ownPublicKey()` is the **only unforgeable
caller identity** — it comes from tx/chain context. Every `wit_*SK` / `wit_secretKey`
witness is DApp-supplied and constrained *only* by the ZK proof; never trust the
witness body, only the in-circuit hash/assert.

### M-of-N models in the presets

| Preset | Auth model | Stubbed? | M-of-N today? |
|---|---|---|---|
| **`ShieldedMultiSig` (V1)** | `getCaller()=ownPublicKey()` + **on-chain approval accounting** (async, 1 signer/tx) | **No — fully real** | **YES** |
| `ShieldedMultiSigV2` | ECDSA sig-bundle vs key commitments, `fold` in one tx | **Yes — `stubVerifySignature → true`** | No (faked) |
| `ShieldedMultiSigV3` (mint/burn) | same sig-bundle, fixed 2-of-3 | **Yes — stubbed** | No (faked) |
| `ForwarderPrivate` | in-circuit `persistentHash` **preimage** gate | **No — real** | 1-of-1 only |

**The decisive finding:** **V1 already implements real, enforced, async M-of-N**
(`createProposal → approve per-signer → execute at threshold`) with no stub. The
stub the CHANGELOG warns about is the *synchronous ECDSA* path in V2/V3 — which we
do not use. `ForwarderPrivate` proves the in-circuit preimage pattern works if we
want a hidden-signer binding later.

---

## 3. Module reference (every non-test module)

### multisig/ (the Vault's spine — unaudited)

- **`SignerManager<T>` / `Signer<T>`** — M-of-N registry over `Set<T>`. `initialize`,
  `assertSigner(caller)`, `assertThresholdMet(count)`, `_addSigner`/`_removeSigner`/
  `_changeThreshold` (all unguarded — consumer gates). `T` is the identity type:
  V1 uses `Either<ZswapCoinPublicKey, ContractAddress>`; V2/V3 use `Bytes<32>`
  commitments. `Signer<T>` adds an inline `_isInitialized`; `SignerManager<T>` omits it.
- **`ProposalManager`** — proposal lifecycle only (**no approval counting**). Types:
  `ProposalStatus{Inactive,Active,Executed,Cancelled}`, `RecipientKind{ShieldedUser,
  UnshieldedUser,Contract}`, `Recipient{kind,address:Bytes<32>}`, `Proposal{to,color,
  amount,status}`. Helpers `shielded/unshielded/contractRecipient`, `toShielded/
  toUnshieldedRecipient`. `_createProposal`/`_cancelProposal`/`_markExecuted` (unguarded)
  + getters. Approval tally is the consumer's job (V1 adds it).
- **`UnshieldedTreasury`** — `_balances: Map<color,Uint<128>>`. `_deposit(color,amount)`
  = `receiveUnshielded` + balance update (overflow-checked). `_send(recipient,color,
  amount)` = balance check + `sendUnshielded`. `getTokenBalance`. Unguarded.
- **`ShieldedTreasury`** (stateful) — `_coins: Map<color,QualifiedShieldedCoinInfo>`
  (one merged UTXO/color) + `_received`/`_sent` accounting. `_deposit(coin)` =
  `receiveShielded` + merge. `_send(recipient,color,amount)` = lookup + `sendShielded`,
  change re-homed via `sendImmediateShielded`. `getTokenBalance` reads real value.
- **`ShieldedTreasuryStateless`** — minimal; `_send` takes a caller-provided coin, no
  on-ledger balance (operator tracks coins off-chain via indexer). Used by V2.
- **`ForwarderUnshielded` / `ForwarderShielded`** — atomic receive→forward to a fixed
  parent (`UserAddress` / `ZswapCoinPublicKey`). Permissionless; `_isInitialized` gate.
  Documents the **UTXO→contract limit** (see §4).
- **`ForwarderPrivate`** — deposits accumulate; `_drain(coin,parent,opSecret,value)`
  gated by `_calculateParentCommitment(parent,opSecret) == _parentCommitment` (real
  `persistentHash` preimage proof, no `witness` keyword). 1-of-1.
- **Presets** (`ShieldedMultiSig`, `V2`, `V3`, `forwarder/*`) — deployable contracts
  composing the above. V1 = the real async M-of-N (see §2).

### access/ (audited)

- **`Ownable`** — single admin, `_owner: Either<Bytes<32>,ContractAddress>`,
  `assertOnlyOwner` checks `persistentHash([wit_OwnableSK()]) == _owner.left`.
  Rejects contract-address owners (no C2C). Inline `_isInitialized`.
- **`AccessControl`** — RBAC, `_operatorRoles`/`_adminRoles` maps, `assertOnlyRole`,
  `grantRole`/`revokeRole` (admin-gated), `renounceRole` (self). Same witness-`sk`
  identity. Public membership. No `initialize` (constructor bootstraps).
- **`ZOwnablePK`** — shielded single-admin, commitment-based, rotates per transfer,
  binds to `ownPublicKey()` + `wit_secretNonce()`. Strong binding, single-admin only.
  High circuit cost (SHA256: `assertOnlyOwner` k=15 rows≈24437).
- **`ShieldedAccessControl`** — shielded RBAC. `_operatorRoles: MerkleTree<20,
  RoleCommitment>`, `_roleCommitmentNullifiers: Set`. Grant = insert leaf; revoke =
  insert nullifier. `assertOnlyRole` proves Merkle membership of *your* commitment +
  not-revoked, via `wit_getRoleCommitmentPath` + `wit_secretKey`. **2^20 slots,
  irrecoverable; admin trust is fundamental (can burn never-granted pairings).**

### security/

- **`Pausable`** — `_isPaused`, `assertPaused`/`assertNotPaused`, `_pause`/`_unpause`
  (unguarded — gate behind auth).
- **`Allowlist` / `Blocklist`** — `Set<Bytes<32>>` membership; `assertAllowed` /
  `assertNotBlocked` seams; `_allow`/`_disallow`, `_block`/`_unblock` (unguarded).
  **Public/enumerable** — leaks the candidate set; use a shielded variant for privacy.
- **`Initializable`** — one-shot guard. **DO NOT use across >1 same-directory module:**
  compiler bug #270 (0.29.0–0.31.0, treat as unfixed at 0.31.1) collapses shared
  `_isInitialized` into one slot. Every OZ module inlines its own flag instead — **so
  must the Vault.**

### crypto/ & utils/

- **`ElGamal`** — stateless Jubjub ElGamal, additively homomorphic, lifted
  `encrypt(pk,Uint<128>,r)`, `add`/`sub`/`scalarMul`/`rerandomize`, `assertDecryptsTo`.
  Witness-free (caller supplies `ek`/plaintext via own witnesses). **Caveats:** no
  bundled range proof (caller bounds the value); **unaudited subgroup-membership trust
  assumption** (re-verify on any ledger/circuits bump); randomness must be fresh/secret/
  `<ℓ` (reuse leaks plaintext difference). The tool for hiding a treasury *amount*.
- **`Utils`** (prefix `Utils_`) — `computeAccountId(sk)=persistentHash([sk])`,
  `canonicalize` (zero the inactive `Either` arm — use on any `Either` map key),
  `selfAsRecipient()=right(kernel.self())`, `zeroAccount`, `UINT128_MAX`, zero checks.

### token/ (audited) — NOT on the Vault's custody path

`FungibleToken`/`NonFungibleToken`/`MultiToken` are **ledger-balance abstractions for
issuing your own token** (the contract is the bookkeeper) and **reject contract
recipients**. `NativeShieldedToken*` mint/burn native shielded UTXOs (fire-and-forget;
no `balanceOf`/`transfer`; the returned `ShieldedCoinInfo` is the only copy → deliver
out-of-band). A Vault holding *existing* NIGHT/shielded funds uses the **treasuries**,
not these. (Relevant only if the Vault later issues a share token.)

**Missing/referenced-but-absent modules** (flag for any plan that needs them):
`NativeTokenConverter`, `NativeShieldedTokenSupply`/`FamilySupply`,
`NativeShieldedTokenDerivedNonce` — referenced by token docs, not vendored.

---

## 4. Hard constraints to design around

1. **UTXO → contract limit.** A contract receives an unshielded/shielded coin only if
   it *claims it in the same transaction* (`receiveUnshielded`/`receiveShielded` in the
   deposit circuit). So: **deposits into the Vault work; withdraw-to-user works;
   withdraw-to-another-contract does not yet** (reserved for a future C2C upgrade; the
   `Either<ContractAddress,UserAddress>` storage shape keeps that a verifier-key-only
   change). This is task #30's answer.
2. **`Initializable` #270** — inline `_isInitialized` per module; never share it.
3. **No ECDSA** — synchronous external-signature multisig (V2/V3) cannot be made real
   today. Use V1's caller-identity model.
4. **Shielded fire-and-forget** — a shielded coin sent out has no ciphertext for the
   recipient to scan; the `ShieldedCoinInfo` must be delivered out-of-band, or use the
   stateful `ShieldedTreasury` which re-homes change to the contract.
5. **ElGamal subgroup assumption is unaudited** — re-verify on any ledger/circuits bump
   if we use confidential amounts.

---

## 5. The Vault design (derived from the study)

**The Vault = V1's proven async M-of-N, extended to dual (unshielded + shielded)
custody, with our identity bound to the Sigil wallet.**

### Composition

```
Vault.compact
├─ SignerManager<Either<ZswapCoinPublicKey, ContractAddress>>  prefix Signer_
├─ ProposalManager                                             prefix Proposal_
├─ UnshieldedTreasury                                          prefix UTreasury_   (NIGHT custody)
├─ ShieldedTreasury  (stateful)                                prefix STreasury_   (shielded custody)
└─ own ledger: _proposalApprovals, _approvalCount             (V1's accounting, extended)
```

### Identity / auth (real, non-stubbed)

- A signer is a **wallet/Sigil**, identified by `getCaller() = left(ownPublicKey())`
  — the unforgeable chain-bound identity (V1's model, stronger than a free `sk`
  witness). `SignerManager` holds the signer set; `Signer_assertSigner(getCaller())`
  gates signer-only circuits.
- M-of-N is **async on-chain accumulation**: each signer calls `approve(id)` in their
  own tx; we record `_proposalApprovals[id][caller]=true` and bump `_approvalCount[id]`,
  rejecting double-approval. `execute(id)` asserts `Signer_assertThresholdMet(count)`.
  The approval map is exactly the double-count guard (the role nullifier's job, done
  with a plain map since identity is public `ownPublicKey()`).

### Flows

1. **Deposit** (permissionless) — `depositUnshielded(color,amount)` →
   `UTreasury__deposit` (`receiveUnshielded`); `depositShielded(coin)` →
   `STreasury__deposit` (`receiveShielded`). Exercises the #15 `Key::Stack` ops.
2. **Propose** (signer-only) — `propose(to,color,amount,kind)` →
   `Proposal__createProposal`. `to` built via the `Proposal_*Recipient` helpers.
3. **Approve** (signer-only, async, one per signer) — accumulate approvals.
4. **Execute** (anyone, once threshold met) — route by `RecipientKind` to
   `UTreasury__send` or `STreasury__send`; `Proposal__markExecuted`.

### Optional hardening (post-core, not slice 1)

- **Confidential amounts** via `ElGamal` (hide proposal/treasury amounts), accepting
  the range-proof + subgroup caveats.
- **Hidden signers** via the `ForwarderPrivate`/`ZOwnablePK` commitment-preimage
  pattern instead of public `ownPublicKey()`, if signer privacy is required.
- `Pausable` emergency stop on deposits/execute; `Allowlist` on proposers.

### First slice (proves the stack on-device, task #28/#30)

`Vault.compact` with `depositUnshielded` + `getUnshieldedBalance` + the signer
registry, deployed through the SDK on the 8.0.3 runtime: proves an OZ-composed
contract runs on our engine (#28) and that contract funding via the receive path
works (#30) — before layering the full propose/approve/execute governance.

---

## 6. Open questions for implementation

- **N and threshold:** V1 fixes `Vector<3>` signers. The Vault should parameterize N
  (the constructor takes the signer vector); confirm `SignerManager.initialize<#n>`
  generic-N works for our chosen N at compile time.
- **Approval uniqueness at larger N:** V1's per-signer map handles any N (unlike V2's
  adjacent-commitment `!=` trick, which is 2-only). Good — we keep the map.
- **Dust/fees:** every state-changing circuit costs DUST; the deposit/approve/execute
  txs must be funded (the SDK's dust path). Validate on-device.
