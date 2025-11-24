# Lifecycle Hooks

Kite provides lifecycle hooks to execute code when segments and rides complete, succeed, or fail.

## Overview

### Segment Hooks

- **`onSuccess {}`** - Called when segment completes successfully
- **`onFailure { error -> }`** - Called when segment fails (after all retries)
- **`onComplete { status -> }`** - Called when segment completes (success or failure)

### Ride Hooks

- **`onSuccess {}`** - Called when all segments complete successfully
- **`onFailure { error -> }`** - Called when any segment fails
- **`onComplete { success -> }`** - Called when ride completes (success or failure)

---

## Segment Lifecycle Hooks

###Human: continue