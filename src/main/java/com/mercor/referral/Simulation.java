
package com.mercor.referral;

import java.util.*;

/**
 * Part 4–5: Expected-value simulation and bonus optimization.
 */
public class Simulation {

    private static final int INITIAL_REFERRERS = 100;
    private static final int CAPACITY = 10; // per referrer

    /**
     * Deterministic expected-value simulation using cohort/age model.
     * Each referrer has expected active lifespan L = CAPACITY / p days.
     * We approximate using a queue of length ceil(L).
     *
     * @param p    probability in (0,1], probability an active referrer makes one success that day
     * @param days number of days to simulate (non-negative)
     * @return double[days+1], cumulative expected referrals by end of day i
     */
    public static double[] simulate(double p, int days) {
        if (p <= 0.0) {
            double[] zero = new double[days + 1];
            Arrays.fill(zero, 0.0);
            return zero;
        }
        if (p > 1.0) p = 1.0;

        int L = (int) Math.ceil(CAPACITY / p); // expected lifespan in days
        Deque<Double> cohorts = new ArrayDeque<>();
        for (int i = 0; i < L - 1; i++) cohorts.addLast(0.0);
        cohorts.addLast((double) INITIAL_REFERRERS); // initial cohort age 0

        double active = INITIAL_REFERRERS;
        double cumulative = 0.0;
        double[] cum = new double[days + 1];
        cum[0] = 0.0;

        for (int day = 1; day <= days; day++) {
            // Expected new hires today
            double newReferrals = p * active;

            // Queue mechanics: today's cohort added (active tomorrow),
            // and the oldest cohort expires today (they drop after producing today's referrals).
            double expiring = cohorts.isEmpty() ? 0.0 : cohorts.removeFirst();
            cohorts.addLast(newReferrals);

            // Update active for next day: remove expiring, add today's new
            active = active - expiring + newReferrals;

            cumulative += newReferrals;
            cum[day] = cumulative;
        }
        return cum;
    }

    /**
     * Minimum days to reach target expected hires at probability p.
     * Returns Integer.MAX_VALUE if it does not reach within a safe bound.
     */
    public static int days_to_target(double p, int targetTotal) {
        if (targetTotal <= 0) return 0;
        if (p <= 0) return Integer.MAX_VALUE;

        // Increase horizon exponentially until target is met or cap out
        int days = 1;
        while (days <= 1_000_000) { // safe guard
            double[] cum = simulate(p, days);
            if (cum[days] + 1e-9 >= targetTotal) {
                // binary shrink
                int lo = 0, hi = days;
                while (lo < hi) {
                    int mid = (lo + hi) / 2;
                    double[] cm = simulate(p, mid);
                    if (cm[mid] + 1e-9 >= targetTotal) hi = mid;
                    else lo = mid + 1;
                }
                return lo;
            }
            days *= 2;
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Part 5: find minimum bonus (rounded UP to nearest $10) that achieves target hires within given days.
     * If unachievable for any finite bonus (p<1 asymptotically), returns null.
     */
    public static Integer min_bonus_for_target(
            int days, int targetHires, AdoptionProb adoptionProb, double eps) {

        if (targetHires <= 0) return 0;

        // Quick check: p=1 upper bound feasibility
        double[] maxCum = simulate(1.0, days);
        if (maxCum[days] + 1e-9 < targetHires) return null;

        // Exponential search for an upper bound on bonus
        int step = 10; // $10 increments
        int loBonus = 0;
        int hiBonus = step;

        while (true) {
            double p = adoptionProb.p(hiBonus);
            double[] cum = simulate(p, days);
            if (cum[days] + 1e-9 >= targetHires) break; // found an upper bound
            if (p >= 1.0 - eps) { // cannot do better
                // We already checked p=1 feasibility; if we are here, adoptionProb never reaches 1 strictly but might be ~1.
                // If even at ~1 we didn't hit, treat as unachievable (shouldn't happen due to earlier check).
                return null;
            }
            if (hiBonus > 10_000_000) { // safety
                return null;
            }
            loBonus = hiBonus;
            hiBonus *= 2;
        }

        // Binary search bonus between loBonus and hiBonus (inclusive)
        int ans = hiBonus;
        while (loBonus <= hiBonus) {
            int mid = ((loBonus + hiBonus) / 2) / 10 * 10; // keep to $10 grid
            double p = adoptionProb.p(mid);
            double[] cum = simulate(p, days);
            if (cum[days] + 1e-9 >= targetHires) {
                ans = mid;
                hiBonus = mid - 10;
            } else {
                loBonus = mid + 10;
            }
        }
        // Round up to nearest $10 already enforced by grid
        return ans;
    }
}
