/* Program to process description of a logic circuit
 * Adapted from MP2 solution @version 2017-09-27
 * by @author Douglas W. Jones
 * @author Andrew Schoneaman
 * version MP3
 * adapted from RoadNetwork.java Version 2017-09-14
 * by @author Douglas W. Jones
 * Bug notices in the code indicate unsolved problems
 */

import java.util.LinkedList;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.regex.Pattern; 

/** Error reporting package
 *  provides standard prefix and behavior for messages
 */
class Errors {
    // error messages are counted.
    private static int errorCount = 0;

    /** Allow public read-only access to the count of error messages
     * @return the count
     */
    public static int count() {
	return errorCount;
    }

    /** Report nonfatal errors, output a message and return
     * @arg message the message to output
     */
    public static void warn( String message ) {
	System.err.println( "Logic: " + message );
	errorCount = errorCount + 1;
    }

    /** Report fatal errors, output a message and exit, never to return
     * @arg message the message to output
     */
    public static void fatal( String message ) {
	warn( message );
	System.exit( 1 );
    }
}

/** Support methods for scanning
 * @see Errors
 */
class ScanSupport {
    // patterns needed for scanning
    private static final Pattern name
	= Pattern.compile( "[a-zA-Z0-9_]*" );
    private static final Pattern whitespace
	= Pattern.compile( "[ \t]*" ); // no newlines
    private static final Pattern number
	= Pattern.compile( "[-+]?[0-9]*.?[0-9]+"); 
	     /** Get next name without skipping to next line (unlike sc.Next())
     *  @param sc the scanner from which end of line is scanned
     *  @return the name, if there was one, or an empty string
     */
    public static String nextName( Scanner sc ) {
	sc.skip( whitespace );
	sc.skip( name );
	return sc.match().group();
    }
    public static float nextFloat( Scanner sc ){
	sc.skip( number ); 
	try {
	    return Float.parseFloat( sc.match().group() );
	} catch ( NumberFormatException e ) {
	    return Float.NaN; 
	}
    }	    

    /** Advance to next line and complain if is junk at the line end
     *  @see Errors
     *  @param sc the scanner from which end of line is scanned
     *  @param message gives a prefix to give context to error messages
     *  This version supports comments starting with --
     */
    public static void lineEnd( Scanner sc, String message ) {
        sc.skip( whitespace );
        String lineEnd = sc.nextLine();
        if ( (!lineEnd.equals( "" ))
        &&   (!lineEnd.startsWith( "--" )) ) {
            Errors.warn(
                message +
                " followed unexpected by '" + lineEnd + "'"
            );
        }
    }
}


/** Wires join Gates
 *  @see Gate
 */
class Wire {
    // constructors may throw this when an error prevents construction
    public static class ConstructorFailure extends Exception {}

    // fields of a gate
    private float delay;        // measured in seconds
    private Gate source;        // where this wire comes from
    private String srcPin;      // what pin of source
    private Gate destination;   // where this wire goes
    private String dstPin;      // what pin of the destination
    // name of a wire is source-srcpin-destination-dstpin

    /** construct a new wire by scanning its description from the source file
     */
    public Wire( Scanner sc ) throws ConstructorFailure {
	String sourceName = sc.next();
	srcPin = sc.next();
	String dstName = sc.next();
	dstPin = sc.next();
	//Bug:  In the above, what if there are no next inputs?

	source = Logic.findGate( sourceName );
	destination = Logic.findGate( dstName );
	if (source == null) {
	    Errors.warn( "No such source gate: Wire "
			+ sourceName + " " + srcPin + " "
			+ dstName + " " + dstPin
	    );
	    throw new ConstructorFailure();
	}
	if (destination == null) {
	    Errors.warn( "No such destination gate: Wire "
			+ sourceName + " " + srcPin + " "
			+ dstName + " " + dstPin
	    );
	    throw new ConstructorFailure();
	}

	// take care of source and destination pins
	// Bug:  This is a start, but in the long run, it might not be right
	source.registerOutput( srcPin );
	destination.registerInput( dstPin );

	if (sc.hasNextFloat()) {
	    delay = sc.nextFloat();
	    if (delay < 0.0F) {
		Errors.warn( "Negative delay: Wire "
			+ sourceName + " " + srcPin + " "
			+ dstName + " " + dstPin + " "
			+ delay
		);
	        delay = 99999.0F; // no failure needed, use bogus value
	    }
	} else {
	    Errors.warn( "Floating point delay expected: Wire "
			+ sourceName + " " + srcPin + " "
			+ dstName + " " + dstPin + " "
	    );
	    delay = 99999.0F; // no failure needed, use bogus value
	}
	ScanSupport.lineEnd( sc, this.toString() );    
    }

    /** output the wire in a form like that used for input
     * @return the textual form
     */ 
    public String toString(){
	String sourceName;
	String destinationName;
	// its possible the wire has no source gate so this avoids null
	// pointer errors
	if( source == null){
	    sourceName  = "No wire source";
	} else {
	    sourceName = this.source.name;
	}
	// provides the same check for destination gates
	if( destination == null ){
	    destinationName = "No wire destination";
	} else {
	    destinationName = this.destination.name;
	}
	return "Wire " + sourceName + " "+ srcPin + " "  +
		destinationName + " " + " " + dstPin
		+ " " + delay;
    }
}// end wire 


/** Gates process inputs from Wires and deliver outputs to Wires
 *  @see Wire
 */

abstract class Gate {
    // constructors may throw this when an error prevents construction
    public static class ConstructorFailure extends Exception {}

    // fields of a gate
    public String name;         // textual name of gate, never null!
    private String kind;        // textual name of gate's type, never null!
    private float delay;        // the delay of this gate, in seconds
    private LinkedList <Wire> outgoing;  // set of all wires out of this gate
    private LinkedList <Wire> incoming;  // set of all wires in to this gate
    // Bug:  Is incoming really needed?
    // Bug:  When are the above ever set to anything?
    // names of all inputs and outputs for this gate
    private LinkedList <String> inputs = new LinkedList <String> ();
    private LinkedList <String> outputs = new LinkedList <String> ();
    // Bug:  The need for the above really depends on the gate kind

    public static Gate newGate( Scanner sc ) throws ConstructorFailure {
	// going to write a whole bunch of new junk here and then slowly
	// remove what I no longer need. This code is ages from being able
	// to be compiled
	String name = ScanSupport.nextName( sc ); 
	if( " ".equals( name )){
	    Errors.warn( "No gate name provided" ); 
	    throw new ConstructorFailure(); 
	}		
	if (Logic.findGate( name ) != null) {
	    Errors.warn( "Gate redefined: " + name );
	    throw new ConstructorFailure();
	}
	
	String type = ScanSupport.nextName( sc ); 
	
	if( "or".equals( type ) ){
	    return new orGate( sc, name ); 
	} else if ( "and".equals( type )){
	    return new andGate( sc, name ); 
	} else if ( "not".equals( type )){ 
	    return new notGate( sc, name ); 
	} else if ( "const".equals( type )){
	    return new constant( sc, name ); 
	} else {
	    Errors.warn( "Gate " + name + " " + type
			+ " has an invalid type" );
	
	    throw new ConstructorFailure();
	}
    }
    protected final void createDelay(Scanner sc){
    
	float num = ScanSupport.nextFloat( sc ); 
	if( num.isNaN( num ) ) {
	    // if no delay give nonsense number
	    delay = 9999999.999F; 
	    Errors.warn( "Gate " + name + " has no delay"); 
	} else {
	    // delay is negative 		
	    if (num < 0.0F) {
		Errors.warn( "Negative delay: Gate "
			+ name + " "
			+ kind + " "
			+ delay
		);
	        delay = 99999.0F; // no failure needed, use bogus value
	    } else {
	        delay = num; 
	    }
        }
    }
    /** tell the gate that one of its input pins is in use
     * @param pinName
     */
    public void registerInput( String pinName ) {
	//Bug: all we do is prevent inputs from being used twice, more later
	for (String s: inputs) {
	    if (s.equals( pinName )) {
		Errors.warn( "Input reused: " + name + " " + pinName );
	    }
	}
	inputs.add( pinName );
    }

    /** tell the gate that one of its output pins is in use
     * @param pinName
     */
    public void registerOutput( String pinName ) {
	//Bug: we do nothing about this here, it'll get more fun later
    }
    
    /** output the gate in a form like that used for input
     * @return the textual form
     */
    protected String myString(){
    	return( "gate " + name ); 
    }
}// end gate

class orGate extends Gate{
    
    boolean[] inputs = new boolean[2];	
    // see lecture notes for 10/03/2017 to see how to solve
    // instatiation problems
    orGate( Scanner sc, String name ){
	this.name = name; 
	ScanSupport.lineEnd( sc, name);        
        this.createDelay(sc); 
    }
}

class andGate extends Gate{
    
    boolean[] inputs = new boolean[2];	
    // not sure that it needs any arguements
    andGate( Scanner sc, String name ){
	this.name = name; 
	ScanSupport.lineEnd( sc, name);        
    }
}


class notGate extends Gate{
     boolean input = false; 
     notGate( Scanner sc, String name ) {
	this.name = name; 
	ScanSupport.lineEnd( sc, name);        
     }
}

class constant extends Gate{
    boolean outTrue = true;
    boolean outFalse = false; 
    constant( Scanner sc, String name ) {
	this.name = name; 
	ScanSupport.lineEnd( sc, name);        
    }
}




public class Logic {

    // the sets of all wires and all gates
    private static LinkedList <Wire> wires
	= new LinkedList <Wire> ();
    private static LinkedList <Gate> gates
	= new LinkedList <Gate> ();

    /** Find a gate by textual name in the set gates
     *  @param s name of a gate
     *  @return the gate named s or null if none
     */
    public static Gate findGate( String s ) {
	// quick and dirty implementation
	for (Gate i: gates) {
	    if (i.name.equals( s )) {
		return i;
	    }
	}
	return null;
    }

    /** Initialize this logic circuit by scanning its description
     */ 
    private static void readCircuit( Scanner sc ) {
        while (sc.hasNext()) {
	    String command = sc.next();
	    if ("gate".equals( command )) {
		try {
		    gates.add( Gate.newGate( sc ) );
		} catch (Gate.ConstructorFailure e) {
		    // do nothing, the constructor already reported the error 
		}
	    } else if ("wire".equals( command )) {
		try {
		    wires.add( new Wire( sc ) );
		} catch (Wire.ConstructorFailure e) {
		    // do nothing, the constructor already reported the error 
		}
	    } else if ("--".equals( command )) {
		sc.nextLine();
	    } else {
	        Errors.warn( "unknown command: " + command );
		sc.nextLine();
	    }
	}
    }

    /** Print out the wire network to system.out
     */ 
    private static void printCircuit() {
	for (Gate i: gates) {
	    System.out.println( i.toString() );
	}
	for (Wire r: wires) {
	    System.out.println( r.toString() );
	}
    }

    /** Main program
     */ 
    public static void main( String[] args ) {
	if (args.length < 1) {
	    Errors.fatal( "Missing file name argument" );
	} else if (args.length > 1) {
	    Errors.fatal( "Too many arguments" );
	} else try {
	    readCircuit( new Scanner( new File( args[0] ) ) );
	    if (Errors.count() == 0) printCircuit();
        } catch (FileNotFoundException e) {
	    Errors.fatal( "Can't open the file" );
	}
    }
}

