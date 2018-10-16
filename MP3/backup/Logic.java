/* Description of a logic circuit adapted by
 * @author Andrew Schoneman from the posted
 * solution to MP2 by @author Douglas W. Jones
 * It also contains a scan support class pulled
 * from RoadNetwork.java by @author Douglas W.
 * Jones
 * version 2017-09-27
 * adapted from RoadNetwork.java Version 2017-09-14
 * @version 2017-10-09
 * @author Andrew Schoneman A2
 * CS2820: 0A02
 * MP3 description
 * This is a logic simulator with classes consisting
 * of gates and wires as well as various other helper
 * classes and it is  adapted from code written by
 * Douglas W. Jones. It contains significant changes
 * to the structure of the code, class heierarchy,
 * and error handling
 * Bug notices in the code indicate unsolved problems
 */

import java.util.LinkedList;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.PriorityQueue; 
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
     *  @arg message the message to output
     */
    public static void warn( String message ) {
	System.err.println( "Logic: " + message );
	errorCount = errorCount + 1;
    }

    /** Report fatal errors, output a message and exit, never to return
     *  @arg message the message to output
     */
    public static void fatal( String message ) {
	warn( message );
	System.exit( 1 );
    }
}





class ScanSupport {
    // patterns needed for scanning
    private static final Pattern name
	= Pattern.compile( "[a-zA-Z0-9_]*" );
    private static final Pattern whitespace
	= Pattern.compile( "[ \t]*" ); // no newlines

    /** Get next name without skipping to next line (unlike sc.Next())
     *  @param sc the scanner from which end of line is scanned
     *  @return the name, if there was one, or an empty string
     */
    public static String nextName( Scanner sc ) {
	sc.skip( whitespace );
	sc.skip( name );
	return sc.match().group();
    }
    public static interface errorMessage{
	public String myString(); 
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
 /*
 *  @see Gate
 */


class Simulator {

    public interface Action {
        // actions contain the specific code of each event
        void trigger( float time );
    }

    private static class Event {
        public float time; // the time of this event
        public Action act; // what to do at that time
    }

    private static PriorityQueue <Event> eventSet
        = new PriorityQueue <Event> (
            (Event e1, Event e2) -> Float.compare( e1.time, e2.time )
        );

    /** Call schedule to make act happen at time.
     *  Users typically pass the action as a lambda expression:
     *  <PRE>
     *  Simulator.schedule( t, ( float time ) -> method( ... time ... ) )
     *  </PRE>
     */
    static void schedule( float time, Action act ) {
        Event e = new Event();
        e.time = time;
        e.act = act;
        eventSet.add( e );
    }

    /** run the simulation.
     *  Call run() after scheduling some initial events to run the simulation.
     */
    static void run() {
        while (!eventSet.isEmpty()) {
            Event e = eventSet.remove();
            e.act.trigger( e.time );
        }
    }
}


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
	// the below code will grab info about a wire
	// if that information is missing then the
	// scan support method will return an empty
	// string which is what a nonspecified variable
	// will be set to
	String sourceName  = ScanSupport.nextName( sc );
	srcPin = ScanSupport.nextName( sc );
	String dstName = ScanSupport.nextName( sc );
	dstPin = ScanSupport.nextName( sc );
	// find the gate where the wire comes from
	source = Logic.findGate( sourceName );
	// find the gate the wire is headed to
	destination = Logic.findGate( dstName );
	if (source == null) {
	    Errors.warn( "No such source gate: Wire "
			+ sourceName + " " + srcPin + " "
			+ dstName + " " + dstPin
	    );
	    sc.nextLine();
	    throw new ConstructorFailure();
	}
	if (destination == null) {
	    Errors.warn( "No such destination gate: Wire "
			+ sourceName + " " + srcPin + " "
			+ dstName + " " + dstPin
	    );
	    sc.nextLine();
	    throw new ConstructorFailure();
	}
	// register the input pin that this wire
	// is linked to is in use by the gate
	// see @gate
	destination.registerInput( dstPin, this );
	// this checks that the output pin is correct
	// and if so connects it to the gate
	// see @gate
	source.registerOutput( srcPin, this );
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
        ScanSupport.lineEnd( sc, "wire " + sourceName +" "+ dstName );
    }

    /** output the wire in a form like that used for input
     * @return the textual form
     */
    public String toString() {
	return  "wire "
		+ source.name + " "
		+ srcPin + " "
		+ destination.name + " "
		+ dstPin + " "
		+ delay;
    }
}

/** Gate is abtract and at the high level it process inputs
 *  from Wires and deliver outputs to Wires
 *  the abstract class gate is used to return a type of gate
 *  not, and, or, constant - are all subclasses of class gate
 *  @see Wire
 *  @see notGate
 *  @see andGate
 *  @see orGate
 *  @see constant
 */
abstract class Gate {
    // constructors may throw this when an error prevents construction
    public static class ConstructorFailure extends Exception {}

    // fields of a gate
    public String name;         // textual name of gate, never null!
    public float delay;
    public static Gate newGate( Scanner sc ) throws ConstructorFailure {

	// Bug: in the above, what if there was no next
	String nme = ScanSupport.nextName( sc );

	if (Logic.findGate( nme ) != null) {
	    Errors.warn( "Gate redefined: " + nme );
	    sc.nextLine();
	    throw new ConstructorFailure();
	}
	if( " ".equals( nme )){
	    Errors.warn( " Gate has no name " );
	    sc.nextLine();
	    throw new ConstructorFailure();
	}
	// here we get the type of gate to build
	// and if it is valid we return a new
	// gate of that type otherwise we throw
	// a constructor failure and suppress
	// creation of the gate
	String type = ScanSupport.nextName( sc );
	if( "and".equals( type )){
	    return new andGate( sc, nme);
	} else if("or".equals( type )){
	    return new orGate( sc, nme );
	} else if("not".equals( type )){
	    return new notGate(sc, nme);
	} else if("const".equals( type )){
	    return new constant(sc, nme);
	} else {
	    Errors.warn( "Gate " + nme + "  has an unknown type");
	    sc.nextLine();
	    throw new ConstructorFailure();
	}
    }

    /** tell the gate that one of its input pins is in use
     *  this is an abstract void and its only purpose is to
     *  be overridden by its subclasses
     * @param pinName - the name of the pin to attach
     * @param Wire - the wire attached to the pin
     */

    abstract void registerInput( String pinName, Wire w );

    /** tell the gate that one of its output pins is in use
     *  this is an abstact void that is overridden by its subclasses
     * @param pinName - name of outgoing pin
     * @param Wire - actual wire out of pin
     */

    abstract void registerOutput( String pinName, Wire w );

    /** Another abstract method which is overridden by its
     *  subclasses and its job is to return true if all of
     *  a gate's input pins are in use
     *  @return true if all input pins used else return false
     */

    abstract boolean inputsUsed();

    /** a method used by all subclasses to finish their
     *  construction and get a float corresponding to
     *  the gate delay in seconds
     * @return the gate delay
     */

    protected float getDelay(Scanner sc, String name){
	if (sc.hasNextFloat()) {
	    delay = sc.nextFloat();
	    if (delay < 0.0F) {
		Errors.warn( "Negative delay: Gate "
			+ name + " "
			+ delay
		);
		// no failure needed, use bogus value
	        delay = 99999.0F;
	    }
	} else {
	    Errors.warn( "Floating point delay expected: Gate "
			+ name + " "
	    );
	   delay = 99999.0F; // no failure needed, use bogus value
	}
	ScanSupport.lineEnd( sc, name );
	return delay;
    }

    /** output the gate in a form like that used for input
     * @return the textual form
     */
    public String toString() {
	// Bug:  Can name be null because of a bad definition?
	return  "gate "
		+ name;

    }
}
/*
 * Start of subclasses for class Gate
 */

/** Class and gate is a type of gate with two input
 *  pins and one output pin
 */

class andGate extends Gate {
    private final float delay;		// gate delay in seconds
    private boolean pin1 = false;
    private boolean pin2 = false;

    // set of all wires out of this gate
    private LinkedList <Wire> outgoing
	    = new LinkedList<Wire> ();

    // wires attached to input pins
    private Wire[] incoming = new Wire[2];

    protected andGate( Scanner sc, String name ){
	this.name = name;
	this.delay = getDelay(sc, this.name);
    }
    /** Registers an input pin as in use, if the name is
     *  invalid or the pin is already in use complain
     *  @param String input
     */
    public void registerInput(String input, Wire w){
        if ("in1".equals(input) && !pin1) {
	    pin1 = true;
	    incoming[0] = w;
	} else if ("in2".equals(input) && !pin2) {
            pin2 = true;
	    incoming[1] = w;
	} else {
	    if( (pin1 || pin2) &&
		("in1".equals(input) || "in2".equals(input)) ){
		Errors.warn("Gate: " + this.name
				+ " " + input +
				" is illegaly redefined");
	    } else {
	        Errors.warn("Gate " + this.name +
				" unknown pin name found "
				+ input);
	    }
	}
    }

    /** Checks to ensure output name is valid and stores the
     *  wires going out of the gate
     *  @param String name
     *  @param Wire w
     */
    protected void registerOutput(String name, Wire w){
	if ("out".equals( name )){
	   outgoing.add(w);
	} else {
	   Errors.warn("Unknown out pin found " + name);
	}
    }
    protected boolean inputsUsed(){
	return pin1 && pin2;
    }
    public String toString(){
	return super.toString() + " and " + delay;
    }
}
/* or gate is a type of class gate which has two input pins
 * and one output pin
 * see @Gate
 * see @wire
 *
 */
class orGate extends Gate {
    private final float delay;		// gate delay in seconds

    // is pin in use? initially false
    private boolean pin1 = false;

    // is pin in use? initially false
    private boolean pin2 = false;

    // set of all wires out of this gate
    private LinkedList <Wire> outgoing
	    = new LinkedList<Wire> ();

    // wires attached to input pins
    private Wire[] incoming = new Wire[2];

    protected orGate( Scanner sc, String name ){
	// set gate name
	this.name = name;
	// set gate delay
	this.delay = getDelay(sc, this.name);
    }
    /** Registers an input pin as in use, if the name is
     *  invalid or the pin is already in use complain
     *  the only valid pin names are in1 and in2
     *  @param String input
     */
    protected void registerInput(String input, Wire w){
        if ("in1".equals(input) && !pin1) {
	    pin1 = true;
	    incoming[0] = w;
	} else if ("in2".equals(input) && !pin2) {
            pin2 = true;
	    incoming[1] = w;
	} else {
	   if( (pin1 || pin2) && ("in1".equals(input) || "in2".equals(input)) ){
		Errors.warn("Gate " + this.name + " " + input +
				" is illegaly redefined");
	    } else {
	        Errors.warn("Gate: " + this.name +
				" unknown pin name found "
				+ input);
	    }
	}
    }

    /** Checks to ensure output name is valid and stores the
     *  wires going out of the gate the only valid name is
     *  of an output pin is out
     *  @param String name
     *  @param Wire w
     */
    protected void registerOutput(String name, Wire w){
	if ("out".equals( name )){
	   outgoing.add(w);
	} else {
	   Errors.warn("Unknown out pin found " + name);
	}
    }
    // returns true if both gates are used else returns false
    protected boolean inputsUsed(){
	return pin1 && pin2;
    }
    // subclass to string method uses parent class Gate's
    // to string method and and inlcudes gate type and delay
    public String toString(){
	return super.toString() + " or " + delay;
    }
}

/* not gate is a type of class gate which has one input pin
 * and one output pin
 * see @Gate
 * see @wire
 *
 */
class notGate extends Gate {
    private final float delay;		// delay in seconds
    public boolean input = false;	// input pin in use initially false

    // set of all wire out of this gate
    private LinkedList <Wire> outgoing
	    = new LinkedList<Wire> ();

    // wire attached to input pin
    private Wire incoming;

    protected notGate( Scanner sc, String name ) {
	// set gate name
	this.name = name;
	// gate delay
	this.delay = getDelay(sc, this.name);
    }
    /** Registers input pin for a not gate as in use or
     *  complains if the pin is already used the only
     *  correct name for a pin is in
     *  @param String pin
     *
     */

    protected void registerInput(String pin, Wire w){
	if("in".equals(pin) && !input){
	    input = true;
	    incoming = w;
	} else if("in".equals(pin) && input){
	    Errors.warn("Gate " +this.name +
			" pin illegally redefined");
	} else {
	    Errors.warn("Gate " + this.name +
			" unknown pin found");
	}
    }
    /** Registers wires going out of a particular not gate
     *  the only valid name for output is out
     *  @param String name
     *  @param Wire w
     */
    protected void registerOutput(String name, Wire w){
	if ("out".equals( name )){
	   outgoing.add(w);
	} else {
	   Errors.warn("Unknown out pin found " + name);
	}
    }
    // returns true if input is in use otherwise false
    protected boolean inputsUsed(){
	return input;
    }
    // subclass to string method uses superclass to string
    // and includes type and delay of the gate
    public String toString(){
	return super.toString() + " not " + delay;
    }
}

/* constant  gate is a type of class gate which has no input pins
 * and two output pins one true and one false.
 * see @Gate
 * see @wire
 *
 */
class constant extends Gate {
    private final float delay;

    // boolean representing the output value of false; unused
    private final boolean outFalse = false;

    // boolean representing the output value of true; unused
    private final boolean outTrue = true;

    // set of all wires out of false
    private LinkedList <Wire> outgoingFalse
	    = new LinkedList<Wire> ();

    // set of all wires out of false
    private LinkedList <Wire> outgoingTrue
	    = new LinkedList<Wire> ();

    protected constant( Scanner sc, String name ){
	// set the gate name
	this.name = name;
	// set the gate delay
	this.delay = getDelay(sc, this.name);
    }
    // wires are never allowed into a constant so if someone
    // tries complain about it
    protected void registerInput(String pin, Wire w){
	Errors.warn( "Constant cannot register input " + pin );
    }

    // check to ensure that outputs are valid from a constant gate
    // and if so add them to the list of wires out of the constant
    // gate the only valid outputs are true and false
    protected void registerOutput(String name, Wire w){
	if("true".equals(name)) {
	    outgoingTrue.add(w);
	} else if ("false".equals(name)){
	    outgoingFalse.add(w);
	} else{
	  Errors.warn( "Incorrect output found for constant " + name );
	}
    }
    // there are no inputs for const so true is always returned
    protected boolean inputsUsed(){
	return true;
    }
    // to string subclass method uses superclass method
    // as well as adding its type and delay
    public String toString(){
	return super.toString() + " const " + delay;
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

    /**  check to see that all input pins are used
     */
    private static void checkInputs(){
	for (Gate g : gates){
	    if(!g.inputsUsed()){
		Errors.warn("Not all input pins used "
				+ g.toString() );
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
     *  attempts to read a single file name
     *  and then create the circuit if the
     *  file can be opened and only a single
     *  argument corresponding to the correct
     *  filename is provided
     */
    public static void main( String[] args ) {
	if (args.length < 1) {
	    Errors.fatal( "Missing file name argument" );
	} else if (args.length > 1) {
	    Errors.fatal( "Too many arguments" );
	} else try {
	    readCircuit( new Scanner( new File( args[0] ) ) );
	    checkInputs();
	    if (Errors.count() == 0) printCircuit();
        } catch (FileNotFoundException e) {
	    Errors.fatal( "Can't open the file" );
	}
    }
}
