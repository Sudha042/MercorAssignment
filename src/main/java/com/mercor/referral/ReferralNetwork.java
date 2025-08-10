
package com.mercor.referral;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Part 1–3: Directed acyclic referral network with analytics.
 *
 * Invariants:
 *  - No self referrals
 *  - Unique referrer (each candidate has at most one parent)
 *  - Acyclic (reject edges that would create a cycle)
 */
public class ReferralNetwork {

    private final Map<String, Set<String>> adj = new HashMap<>();
    private final Map<String, String> parent = new HashMap<>();
    private final Set<String> users = new HashSet<>();

    public boolean addUser(String user) {
        if (user == null || user.isBlank()) return false;
        if (users.add(user)) {
            adj.computeIfAbsent(user, k -> new HashSet<>());
            return true;
        }
        return false;
    }

    /**
     * Add a directed referral link referrer -> candidate.
     * Returns true if added, false if rejected by constraints.
     */
    public boolean addReferral(String referrer, String candidate) {
        if (referrer == null || candidate == null) return false;
        if (referrer.equals(candidate)) return false; // no self-referral
        if (!users.contains(referrer) || !users.contains(candidate)) return false;
        if (parent.containsKey(candidate)) return false; // unique referrer

        // Cycle check: would candidate reach referrer already?
        if (isReachable(candidate, referrer)) return false;

        // Add edge
        adj.get(referrer).add(candidate);
        parent.put(candidate, referrer);
        return true;
    }

    public Set<String> getDirectReferrals(String user) {
        if (!users.contains(user)) return Collections.emptySet();
        return Collections.unmodifiableSet(adj.getOrDefault(user, Collections.emptySet()));
    }

    private boolean isReachable(String start, String target) {
        if (start.equals(target)) return true;
        Deque<String> dq = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();
        dq.add(start);
        seen.add(start);
        while (!dq.isEmpty()) {
            String u = dq.poll();
            for (String v : adj.getOrDefault(u, Collections.emptySet())) {
                if (v.equals(target)) return true;
                if (seen.add(v)) dq.add(v);
            }
        }
        return false;
    }

    // ===== Part 2: Reach =====

    public int getTotalReach(String user) {
        return getReachSet(user).size();
    }

    private Set<String> getReachSet(String user) {
        if (!users.contains(user)) return Collections.emptySet();
        Set<String> reached = new HashSet<>();
        Deque<String> dq = new ArrayDeque<>();
        dq.add(user);
        while (!dq.isEmpty()) {
            String u = dq.poll();
            for (String v : adj.getOrDefault(u, Collections.emptySet())) {
                if (reached.add(v)) dq.add(v);
            }
        }
        return reached;
    }

    public List<Map.Entry<String, Integer>> getTopKReferrersByReach(int k) {
        List<Map.Entry<String, Integer>> all = users.stream()
                .map(u -> Map.entry(u, getTotalReach(u)))
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());
        if (k <= 0 || k >= all.size()) return all;
        return all.subList(0, k);
    }

    // ===== Part 3: Influencers =====

    /** Precompute full downstream reach sets for all users. */
    public Map<String, Set<String>> precomputeReachSets() {
        Map<String, Set<String>> reach = new HashMap<>();
        for (String u : users) {
            reach.put(u, getReachSet(u));
        }
        return reach;
    }

    /**
     * Greedy unique reach expansion:
     * @param limit how many referrers to select
     * @return ordered list of chosen users maximizing unique covered candidates
     */
    public List<String> uniqueReachExpansion(int limit) {
        Map<String, Set<String>> reach = precomputeReachSets();
        Set<String> covered = new HashSet<>();
        List<String> chosen = new ArrayList<>();
        Set<String> available = new HashSet<>(users);

        while (chosen.size() < limit && !available.isEmpty()) {
            String best = null;
            int bestGain = -1;
            for (String u : available) {
                Set<String> set = reach.getOrDefault(u, Collections.emptySet());
                int gain = 0;
                for (String c : set) if (!covered.contains(c)) gain++;
                if (gain > bestGain) {
                    bestGain = gain;
                    best = u;
                }
            }
            if (best == null || bestGain <= 0) break;
            chosen.add(best);
            for (String c : reach.get(best)) covered.add(c);
            available.remove(best);
        }
        return chosen;
    }

    /**
     * Flow centrality per prompt: for all s,t,v, increment v if it lies on a shortest s→t path.
     * Returns descending list of (user, score).
     */
    public List<Map.Entry<String, Integer>> flowCentrality() {
        List<String> nodes = new ArrayList<>(users);
        int n = nodes.size();
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(nodes.get(i), i);

        final int INF = 1_000_000_000;
        int[][] dist = new int[n][n];
        for (int i = 0; i < n; i++) Arrays.fill(dist[i], INF);

        // BFS from every node
        for (int i = 0; i < n; i++) {
            String s = nodes.get(i);
            Deque<String> dq = new ArrayDeque<>();
            Map<String, Integer> d = new HashMap<>();
            dq.add(s);
            d.put(s, 0);
            while (!dq.isEmpty()) {
                String u = dq.poll();
                int du = d.get(u);
                for (String v : adj.getOrDefault(u, Collections.emptySet())) {
                    if (!d.containsKey(v)) {
                        d.put(v, du + 1);
                        dq.add(v);
                    }
                }
            }
            for (Map.Entry<String, Integer> e : d.entrySet()) {
                dist[i][idx.get(e.getKey())] = e.getValue();
            }
        }

        Map<String, Integer> score = new HashMap<>();
        for (String u : users) score.put(u, 0);
        for (int si = 0; si < n; si++) {
            for (int ti = 0; ti < n; ti++) {
                if (si == ti) continue;
                int dst = dist[si][ti];
                if (dst >= INF) continue;
                for (int vi = 0; vi < n; vi++) {
                    if (vi == si || vi == ti) continue;
                    if (dist[si][vi] + dist[vi][ti] == dst) {
                        String v = nodes.get(vi);
                        score.put(v, score.get(v) + 1);
                    }
                }
            }
        }

        return score.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());
    }

    // ===== Utilities =====

    public Set<String> getUsers() {
        return Collections.unmodifiableSet(users);
    }

    public void ensureUsers(String... ids) {
        for (String id : ids) addUser(id);
    }
}
