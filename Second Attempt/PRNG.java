import java.util.Random; 

public class PRNG {
    private static Random stream = new Random(5);
    
    // Bug:  For debugging, use a known seed so errors are reproducable

    /** get a number n where 0 <= n < bound
     *  @param bound
     *  @return n
     */
//    public static int fromZeroToInt( int bound ) {
//	return stream.nextInt( bound );
//    }
    public static float fromZeroToFloat(float delay ) {
	
	return stream.nextFloat( )*(delay); 
    }
}
