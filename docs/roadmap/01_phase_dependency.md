# Stage Dependency Definition

## Dependency Chain
- Stage 0 -> Stage 1 -> Stage 2 -> Stage 3

## Stage 0 -> Stage 1
- Required outputs:
  - freeze checklist
  - unresolved conflicts ledger
  - runtime contracts
- Blocker:
  - unresolved C0 conflicts

## Stage 1 -> Stage 2
- Required outputs:
  - validation evidence against runtime contracts
  - ownership/lifecycle/state/failure checks all pass
- Blocker:
  - any contract lacks measurable validation signal

## Stage 2 -> Stage 3
- Required outputs:
  - execution readiness decision
  - milestone gating map
- Blocker:
  - missing exit criteria or ambiguous rollback strategy
