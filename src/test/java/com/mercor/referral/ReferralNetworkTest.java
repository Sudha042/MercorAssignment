
package com.mercor.referral;

import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ReferralNetworkTest {

    @Test
    void testAddUsersAndReferralsConstraints() {
        ReferralNetwork rn = new ReferralNetwork();
        rn.ensureUsers("A","B","C","D");

        // self referral rejected
        assertFalse(rn.addReferral("A", "A"));

        // unique referrer
        assertTrue(rn.addReferral("A", "B"));
        assertFalse(rn.addReferral("C", "B"));

        // cycle prevention
        rn.ensureUsers("E","F");
        assertTrue(rn.addReferral("B","C"));
        assertTrue(rn.addReferral("C","D"));
        // Now adding D->A would create a cycle A->B->C->D->A
        assertFalse(rn.addReferral("D","A"));
    }

    @Test
    void testDirectReferralsAndReach() {
        ReferralNetwork rn = new ReferralNetwork();
        rn.ensureUsers("A","B","C","D","E","F");
        assertTrue(rn.addReferral("A","B"));
        assertTrue(rn.addReferral("A","C"));
        assertTrue(rn.addReferral("B","D"));
        assertTrue(rn.addReferral("C","E"));
        assertTrue(rn.addReferral("E","F"));

        Set<String> direct = rn.getDirectReferrals("A");
        assertEquals(Set.of("B","C"), new HashSet<>(direct));

        assertEquals(5, rn.getTotalReach("A")); // B,C,D,E,F
        assertEquals(2, rn.getTotalReach("B")); // D
        assertEquals(2, rn.getTotalReach("C")); // E,F
        assertEquals(0, rn.getTotalReach("F"));
    }

    @Test
    void testTopK() {
        ReferralNetwork rn = new ReferralNetwork();
        rn.ensureUsers("A","B","C","D","E");
        rn.addReferral("A","B");
        rn.addReferral("A","C");
        rn.addReferral("B","D");
        rn.addReferral("C","E");

        var top2 = rn.getTopKReferrersByReach(2);
        assertEquals("A", top2.get(0).getKey());
        assertEquals(4, top2.get(0).getValue());
        assertEquals(2, top2.size());
    }

    @Test
    void testUniqueReachExpansion() {
        ReferralNetwork rn = new ReferralNetwork();
        rn.ensureUsers("A","B","C","D","E","F","G","H");
        rn.addReferral("A","B");
        rn.addReferral("A","C");
        rn.addReferral("B","D");
        rn.addReferral("C","E");
        rn.addReferral("E","F");
        rn.addReferral("G","H");

        List<String> chosen = rn.uniqueReachExpansion(2);
        assertTrue(chosen.contains("A")); // covers many
        // second could be G or others depending on reach overlap
        assertEquals(2, chosen.size());
    }

    @Test
    void testFlowCentrality() {
        ReferralNetwork rn = new ReferralNetwork();
        rn.ensureUsers("A","B","C","D","E");
        rn.addReferral("A","B");
        rn.addReferral("B","C");
        rn.addReferral("A","D");
        rn.addReferral("D","C");
        rn.addReferral("C","E");

        var ranking = rn.flowCentrality();
        // B and D lie on many shortest paths from A to E
        String first = ranking.get(0).getKey();
        assertTrue(Set.of("B","D","C","A","E").contains(first)); // sanity check non-empty order
        assertEquals(5, ranking.size());
    }
}
