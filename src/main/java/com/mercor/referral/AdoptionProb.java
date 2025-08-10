
package com.mercor.referral;

@FunctionalInterface
public interface AdoptionProb {
    /**
     * @param bonus bonus amount in dollars
     * @return probability p in [0,1] that an active user makes one successful referral on a given day
     */
    double p(double bonus);
}
