/*
 *
 *    Copyright 2016 jshook
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * /
 */

package io.engineblock.activityapi.ratelimits;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <h2>Synopsis</h2>
 *
 * This TokenPool represents a finite quantity which can be
 * replenished with regular refills. Extra tokens that do not fit
 * within the active token pool are saved in a waiting token pool and
 * used to backfill when allowed according to the backfill rate.
 *
 * A detailed explanation for how this works is included at @link "http://docs.engineblock.io/dev-notes/rate_limiter_design/"
 *
 *
 * <p>This is the basis for the token-based rate limiters in
 * EngineBlock. This mechanism is easily adaptable to bursting
 * capability as well as a degree of stricter timing at speed.
 * Various methods for doing this in a lock free way were
 * investigated, but the intrinsic locks provided by synchronized
 * method won out for now. This may be revisited when EB is
 * retrofitted for J11.
 * </p>
 */
public class TokenPool {
    private final static Logger logger = LoggerFactory.getLogger(TokenPool.class);
    private long maxActivePool;
    private long maxOverActivePool;
    private double burstRatio;
    private volatile long activePool;
    private volatile long waitingPool;


    /**
     * This constructor tries to pick reasonable defaults for the token pool for
     * a given rate spec. The active pool must be large enough to contain one
     * op worth of time, and the burst ratio
     *
     * @param rateSpec a {@link RateSpec}
     */
    public TokenPool(RateSpec rateSpec) {
        apply(rateSpec);
        logger.debug("initialized token pool: " + this.toString() + " for rate:" + rateSpec.toString());
    }

//    public TokenPool(TokenPool tokenPool, RateSpec rateSpec) {
//        this(rateSpec);
//        this.waitingPool=tokenPool.getWaitPool();
//        this.activePool=tokenPool.getActivePool();
//        logger.debug("continued token pool: " + this.toString() + " for rate:" + rateSpec.toString());
//    }

    public void apply(RateSpec rateSpec) {
        this.maxActivePool = Math.max((long) 1E6, (long) ((double) rateSpec.getNanosPerOp()));
        this.maxOverActivePool = (long) (maxActivePool * rateSpec.getBurstRatio());
        this.burstRatio = rateSpec.getBurstRatio();
    }

    //    public TokenPool(long maxActivePool, double burstRatio) {
//        this.maxActivePool=maxActivePool;
//        this.burstRatio = burstRatio;
//        this.maxOverActivePool = (long)(maxActivePool*burstRatio);
//    }
//

    public TokenPool(long poolsize, double burstRatio) {
        this.maxActivePool = poolsize;
        this.burstRatio = burstRatio;
        this.maxOverActivePool = (long) (maxActivePool * burstRatio);
    }
    public long getMaxActivePool() {
        return maxActivePool;
    }

    public double getBurstRatio() {
        return burstRatio;
    }


    /**
     * Take tokens up to amt tokens form the pool and report
     * the amount of token removed.
     *
     * @param amt tokens requested
     * @return actual number of tokens removed, greater to or equal to zero
     */
    public synchronized long takeUpTo(long amt) {
        long take = Math.min(amt, activePool);
        activePool -= take;
        return take;
    }

    /**
     * wait for the given number of tokens to be available, and then remove
     * them from the pool.
     *
     * @param amt The number of tokens to take
     * @return the total number of tokens untaken, including wait tokens
     */
    public synchronized long blockAndtake(long amt) {
        while (activePool < amt) {
            //System.out.println(ANSI_BrightRed +  "waiting for " + amt + "/" + activePool + " of max " + maxActivePool + ANSI_Reset);
            try {
                wait(maxActivePool / 1000000, (int) maxActivePool % 1000000);
            } catch (InterruptedException ignored) {
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            //System.out.println("waited for " + amt + "/" + activePool + " tokens");
        }
        //System.out.println(ANSI_BrightYellow + "taking " + amt + "/" + activePool + ANSI_Reset);

        activePool -= amt;
        return waitingPool + activePool;
    }

    public synchronized boolean takeIfAvailable(long amt) {
        if (activePool < amt) {
            return false;
        }
        activePool -= amt;
        return true;
    }

    public long getTokenCount() {
        return activePool + waitingPool;
    }

    public long getWaitPool() {
        return waitingPool;
    }

    public long getActivePool() {
        return activePool;
    }

    /**
     * Add the given number of new tokens to the pool, forcing any amount
     * that would spill over the current pool size into the wait token pool, but
     * moving up to the configured burst tokens back from the wait token pool
     * otherwise.
     *
     * The amount of backfilling that occurs is controlled by the backfill ratio,
     * based on the number of tokens submitted. This causes normalizes the
     * backfilling rate to the fill rate, so that it is not sensitive to refill
     * scheduling.
     *
     * @param newTokens The number of new tokens to add to the token pools
     * @return the number of tokens in the active pool
     */
    public synchronized long refill(long newTokens) {

        long needed = Math.max(maxActivePool - activePool, 0L);
        long adding = Math.min(newTokens, needed);
        long unused = newTokens - adding;
        waitingPool += unused;
        activePool += adding;

        double backfillFactor = (double) newTokens / maxActivePool;

        long backfill = Math.min(maxOverActivePool - activePool, waitingPool);

        waitingPool -= backfill;
        activePool += backfill;

        //System.out.println(this);
        notifyAll();

        return activePool;
    }

    /**
     * This method of calling refill allows for correction of the backfill rate
     * with respect to refill frequency. For example, if you call this method
     * for times as often, set the backfill ratio to 0.25D, and so on.
     *
     * @param newTokens     The number of new tokens to add to the token pools
     * @param backfillRatio The proportion of available backfill to use, maximum
     * @return the number of tokens in the active pool
     */
    public synchronized long refill(long newTokens, double backfillRatio) {
        long needed = Math.max(maxActivePool - activePool, 0L);
        long adding = Math.min(newTokens, needed);
        long unused = newTokens - adding;
        waitingPool += unused;
        activePool += adding;

        long backfill = Math.min(maxOverActivePool - activePool, waitingPool);
        backfill = (long) (backfillRatio * (double) backfill);

        waitingPool -= backfill;
        activePool += backfill;

        //System.out.println(this);
        notifyAll();

        return activePool;

    }

    @Override
    public String toString() {
        return "Tokens: active=" + activePool +"/" + maxActivePool
                + String.format(
                        " (%3.1f%%)A (%3.1f%%)B ",
                (((double)activePool/(double)maxActivePool)*100.0),
                (((double)activePool/(double)maxOverActivePool)*100.0)) + " waiting=" + waitingPool;
    }

}
