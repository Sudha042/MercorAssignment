
package com.mercor.referral;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SimulationTest {

    @Test
    void testSimulateBasic() {
        double p = 0.1;
        int days = 30;
        double[] cum = Simulation.simulate(p, days);
        assertEquals(days + 1, cum.length);
        assertTrue(cum[days] > cum[days/2]);
        assertTrue(cum[days] > 0.0);
    }

    @Test
    void testDaysToTarget() {
        int d = Simulation.days_to_target(0.2, 500);
        assertTrue(d > 0 && d < 10000);
    }

    @Test
    void testMinBonusForTarget() {
        AdoptionProb ap = bonus -> Math.max(0.0, Math.min(1.0, (bonus / 1000.0))); // linear clip: $1000 -> p=1
        Integer bonus = Simulation.min_bonus_for_target(60, 2000, ap, 1e-3);
        assertNotNull(bonus);
        assertTrue(bonus % 10 == 0);
        // If we make target huge, it may be unattainable within days at p<=1
        Integer impossible = Simulation.min_bonus_for_target(5, 1000000, ap, 1e-3);
        assertNull(impossible);
    }
}
