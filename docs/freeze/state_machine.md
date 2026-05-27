# Runtime State Machine Freeze Checklist

## Frozen State Set
- `IDLE`
- `PREPARED`
- `PLAYING`
- `PAUSED`
- `SEEK_REQUESTED`
- `SEEK_FLUSHING`
- `SEEK_PRIMING`
- `SEEK_RECOVERING`
- `EOS_HOLD`
- `STOPPED`
- `RELEASED`

## Frozen Transition Rules
1. `IDLE -> PREPARED -> PLAYING`
2. `PLAYING -> PAUSED -> PLAYING`
3. `PLAYING/PAUSED -> SEEK_REQUESTED -> SEEK_FLUSHING -> SEEK_PRIMING -> SEEK_RECOVERING -> PLAYING/PAUSED`
4. `PLAYING -> EOS_HOLD -> PAUSED/STOPPED`
5. `Any non-RELEASED -> STOPPED -> RELEASED`

## State Gate by Command
| Command | Allowed Source States | Target | Failure Policy |
|---|---|---|---|
| prepare | IDLE | PREPARED | invalid-state error |
| play | PREPARED/PAUSED/SEEK_RECOVERING-ready | PLAYING | recover-not-ready or invalid-state |
| pause | PLAYING | PAUSED | invalid-state |
| seek | PLAYING/PAUSED | SEEK_REQUESTED | invalid-state |
| stop | non-RELEASED | STOPPED | fatal only on teardown breach |
| release | STOPPED | RELEASED | invalid-state or fatal release breach |

## Non-frozen / Missing in 00–08 (must backport)
- `Seeking` coarse state must be replaced by frozen seek sub-states.
- `EOS_HOLD` and `RELEASED` states missing from 00–03 and 06.
- Illegal transition handling rule missing (error + warning log + no implicit correction).
