/* PRNG.java
 * support class for pseudo-random number generation
 * author Douglas W. Jones
 * version 2017-11-15 (MP5 solution)
 * Adapted from Logic.java Version 2017-10-30 (the MP4 solution).
 * 
 * Bug notices in the code indicate unsolved problems
 */

import java.util.Random;

/** Pseudo Random Number Generator
 *  needed to make a single global stream of numbers, hiding Java's failures
 */
public class PRNG {
    private static Random stream = new Random( 5 );
    // Bug:  For debugging, use a known seed so errors are reproducable

    /** get a number n where 0 <= n < bound
     *  @param bound
     *  @return n
     */
    public static int fromZeroTo( int bound ) {
	return stream.nextInt( bound );
    }

    /** get a floating point number x such that 0 <= n < bound
     *  @param f
     *  @return x
     */
    public static float randomFloat( float f ) {
	return stream.nextFloat() * f;
    }
}
