
# Mercor Challenge: Referral Network (Java)

**Language:** Java 17 (Maven)  
**Test Framework:** JUnit 5

This repository implements all five parts of the "Mercor Take Home - Referral Network" challenge with a clean, well-tested Java solution.

## Repository Structure

```
mercor-challenge/
├── README.md
├── .gitignore
├── pom.xml
└── src/
    ├── main/java/com/mercor/referral/
    │   ├── ReferralNetwork.java
    │   ├── Simulation.java
    │   └── AdoptionProb.java
    └── test/java/com/mercor/referral/
        ├── ReferralNetworkTest.java
        └── SimulationTest.java
```

## Setup

1. Install Java 17+ and Maven 3.9+.
2. From the project root, run:

```bash
mvn -q -version
mvn -q -DskipTests package
```

## Running Tests

```bash
mvn -q test
```

## Design Choices (Part 1)

### Data Structure
- Directed graph with:
  - `adj`: `Map<String, Set<String>>` adjacency (referrer → direct referrals)
  - `parent`: `Map<String, String>` for unique referrer constraint
  - `users`: `Set<String>` registry

### Invariants Enforced
1. **No Self-Referrals:** `referrer.equals(candidate)` rejected.
2. **Unique Referrer:** if `candidate` already has a parent, reject.
3. **Acyclic Graph:** adding `referrer → candidate` would create a cycle iff `referrer` is reachable from `candidate`. We check reachability by BFS from `candidate`.

Operations are O(out-degree) to enumerate referrals, and O(V+E) worst-case for cycle checks.

## Part 2: Full Network Reach
- `getTotalReach(user)`: BFS/DFS over downstream nodes; O(V+E) in the reachable subgraph.
- `getTopKReferrersByReach(k)`: compute reach for all users and sort; O(U * (V+E) + U log U). In practice, use a min-heap if only top-k is needed. Guidance: choose `k` as a small number suitable for UI (e.g., top 10/50), or dependent on dataset size and latency.

## Part 3: Identify Influencers
- **Unique Reach Expansion (Greedy Set Cover-style)**:
  - Precompute downstream reach set for each user (store as `Set<String>`). O(U * (V+E)).
  - Iteratively pick the user that adds the largest number of *new* candidates to the covered set. Repeat for a caller-provided `limit`. Each iteration scans all users: O(limit * U * avgSetOps).

- **Flow Centrality (Shortest-path Brokers)**
  - BFS from every node to get `dist[u][v]` on the directed, unweighted graph; O(U * (V+E)).
  - For each ordered triple `(s, t, v)` with `s != v != t`, if `dist[s][v] + dist[v][t] == dist[s][t]` (and finite), increment score of `v`. This directly follows the prompt; straightforward and correct. Complexity ~ O(U^3) with adjacency checks. For clarity > micro-optimizations as requested.

### Metric Comparison & When to Use
- **Reach:** total downstream count. *Use when:* raw influence, leaderboard, or simple incentive tiers.
- **Unique Reach Expansion:** chooses a *set* of referrers that together cover the most unique candidates (minimizes overlap). *Use when:* budget-limited campaigns selecting a small group of champions.
- **Flow Centrality:** scores brokers on many shortest paths. *Use when:* identify bridge-builders whose enablement or loss most fragments the network.

## Part 4: Network Growth Simulation
- Deterministic **expected-value** model with daily steps.
- **Parameters:** initial active referrers = 100; capacity = 10 referrals per referrer; probability `p` a referrer makes 1 successful referral on a given day.
- **Model:** cohort/age-based expectation. Each referrer has expected active lifespan `L = 10 / p` days. We simulate with a queue of length `ceil(L)`. Each day:
  - expected new referrals = `p * active` (capped implicitly by cohort expiry),
  - they join next day's active pool,
  - cohorts drop out after `L` days.
- `simulate(p, days)` returns cumulative expected hires by day.
- `days_to_target(p, target)` runs until cumulative ≥ target (or returns `Integer.MAX_VALUE` if not reached under numeric guard).

## Part 5: Referral Bonus Optimization
- `min_bonus_for_target(days, target, adoption_prob, eps)`:
  - `adoption_prob(bonus)` is monotonic ↑.
  - Exponential search to find an upper bound where target is achievable, then binary search on bonus.
  - Bonus rounded **up** to nearest `$10`.
  - If even with very large bonus (prob≈1) the expected hires do not reach target, return null.

### Time Complexity Summary
- Part 1 operations: add referral worst-case O(V+E); direct referrals O(out-degree).
- Part 2:
  - `getTotalReach`: O(V+E) per user.
  - `getTopKReferrersByReach`: O(U * (V+E) + U log U).
- Part 3:
  - Unique expansion: O(U * (V+E) + limit * U * setOps).
  - Flow centrality: O(U^3) (BFS all-pairs + triple loops).
- Part 4:
  - Simulation: O(days).
  - `days_to_target`: O(days).
- Part 5:
  - Exponential + binary search: O((log B) * (days)) calls to simulation, where `B` is the bonus range in $10 increments.

## AI Usage
I used an AI assistant to accelerate boilerplate creation (Maven scaffolding, test shells) and to sanity-check edge cases. All logic, algorithms, and final code were designed and verified by me and are fully understood.
