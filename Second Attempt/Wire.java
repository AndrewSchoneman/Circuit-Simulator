import java.util.Scanner;


public class Wire {
    // constructors may throw this when an error prevents construction
    public static class ConstructorFailure extends Exception {}

    // fields of a gate
    private final float delay;        // measured in seconds
    private final Gate source;        // where this wire comes from, never null
    private final int srcPin;         // what pin number of source
    private final Gate destination;   // where this wire goes, never null
    private final int dstPin;         // what pin number of destination
    // note, wires don't understand pin numbers, only gates do.
    // note, by convention -1 is an illegal pin number.

    /** construct a new wire by scanning its description from the source file
     *  @param sc the scanner from which the wire description is scanned
     *  @see ScanSupport for the tools used to access the scanner
     *  @throws ConstructorFailure when a new wire cannot be constructed
     */
    public Wire( Scanner sc ) throws ConstructorFailure {
	// temporaries used during construction
	final String sourceName;
	final String srcPinName;
	final String dstName;
	final String dstPinName;

	// pick off the text fields of the source line
	try {
	    sourceName = ScanSupport.nextName(
		sc, ()-> "wire ???"
	    );
	    srcPinName = ScanSupport.nextName(
		sc, ()->"wire " + sourceName + " ???"
	    );
	    dstName = ScanSupport.nextName(
		sc, ()->"wire " + " " + srcPinName + " ???"
	    );
	    dstPinName = ScanSupport.nextName(
		sc, ()->"wire " + " " + srcPinName + " " + dstName + " ???"
	    );
	} catch (ScanSupport.NotFound e) {
	    throw new ConstructorFailure();
	}

	source = Logic.findGate( sourceName );
	destination = Logic.findGate( dstName );
	if (source == null) {
	    Errors.warn( "No such source gate: wire "
			+ sourceName + " " + srcPinName + " "
			+ dstName + " " + dstPinName
	    );
	    sc.nextLine();
	    throw new ConstructorFailure();
	}
	if (destination == null) {
	    Errors.warn( "No such destination gate: wire "
			+ sourceName + " " + srcPinName + " "
			+ dstName + " " + dstPinName
	    );
	    sc.nextLine();
	    throw new ConstructorFailure();
	}

	// take care of source and destination pins
	// Bug:  This is a start, but in the long run, it might not be right
	srcPin = source.registerOutput( this, srcPinName );
	dstPin = destination.registerInput( this, dstPinName );

	// pick off the numeric field of the source line
	try {
	    delay = ScanSupport.nextFloat(
		sc, ()->"wire "
		    + sourceName + " " + srcPinName + " "
		    + dstName + " " + dstPinName + " ???"
	    );
	} catch (ScanSupport.NotFound e) {
	    throw new ConstructorFailure();
	}
	if (delay < 0.0F) Errors.warn( "Negative delay: " + this.toString() );
	
	ScanSupport.lineEnd( sc, ()->this.toString() );
    }

    /** get textual description of a wire in a form like that used for input
     * @return the textual form
     */
    public String toString() {
	return  "wire "
		+ source.name + " "
		+ source.outPinName( srcPin ) + " "
		+ destination.name + " "
		+ destination.inPinName( dstPin ) + " "
		+ delay;
    }

    // Simulation methods

    /** Simulate an input change on this wire
     *  @param time tells when this wire's input changes
     *  @param v gives the new value on this wire
     *  schedules an output change event after the wire's delay.
     *  @see outputChangeEvent
     */
    public void inputChangeEvent( float time, boolean v ) {
  	Simulator.schedule(
	    time + delay, (float t) -> outputChangeEvent( t, v )
        );
    }

    /** Simulate an output change on this wire
     *  @param time tells when this wire's input changes
     *  @param v gives the new value on this wire
     *  Passes the new value to the input of the gate to which this wire goes.
     *  @see Gate.inputChangeEvent
     */
    private void outputChangeEvent( float time, boolean v ) {
	destination.inputChangeEvent( time, dstPin, v );
    }

} // class Wire

