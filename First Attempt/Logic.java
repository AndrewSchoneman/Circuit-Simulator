/* A description of a logic circuit
 * adapted from the MP3 solution
 * by @author Douglas W. Jones it also
 * contains a simulation framework from roadNetwork.java
 * also by @author Douglas W. Jones
 *
 * @author Andrew Schoneman
 *
 * version 2017-10-20 (MP4 solution)
 * Adapted from Logic.java Version 2017-09-27 (the MP2 solution),
 * which was adapted from RoadNetwork.java Version 2017-09-14
 *
 * Class ScanSupport added from lecture notes for Oct. 3 and 5, augmented
 * with ScanSupport.nextFloat from the homework 6 solution
 *
 * This solution involves lots of duplicate code between subclasses of Gate
 * because it makes no effort to combine, for example, and and or gates into
 * a common subclass containing all gates that have 2 inputs named in1 and in2.
 * A more elaborate class hierarchy could have reduced this duplication.
 *
 * Bug notices in the code indicate unsolved problems
 */

import java.util.regex.Pattern;
import java.util.LinkedList;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
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

/** class for running a simulation taken from roadNetwork.java
 *  uses a priority queue to determine the order of events an
 *  an interface for triggering the action at the time of the
 *  event
 *  @author Douglas W. Jones
 *
 */

class Simulator {

    /** interface used to allow lambda expressions passed to schedule method
     */
    public interface Action {
	// actions contain the specific code of an event
	void trigger( float time );
    }

    private static class Event {
	public float time;	// time of the event
	public Action act;	// the action to perform
    }

    private static PriorityQueue <Event> eventSet
	= new PriorityQueue <Event> (
	    (Event e1, Event e2) -> Float.compare( e1.time, e2.time )
	);

    /** schedule one new event
     *  @param time when an event will occur
     *  @param act the action that will be triggered at that time
     *  Typically, this is called as follows:
     *  <pre>
     *  </pre>
     */
    public static void schedule( float time, Action act ) {
	Event e = new Event();
	e.time = time;
	e.act = act;
	eventSet.add( e );
    }

    /** main loop that runs the simulation
     *  This must be called after all initial events are scheduled.
     */
    public static void run() {
	while (!eventSet.isEmpty()) {
	    Event e = eventSet.remove();
	    e.act.trigger( e.time );
	}
    }
}

/** Support methods for scanning
 * @see Errors
 */
class ScanSupport {
    // patterns needed for scanning
    private static final Pattern name = Pattern.compile(
	"[a-zA-Z0-9_]*"
    );
    private static final Pattern number = Pattern.compile(
	"[0-9][0-9]*\\.?[0-9]*|\\.[0-9][0-9]*|"
    );
    private static final Pattern whitespace = Pattern.compile(
	"[ \t]*" // no newlines in this pattern
    );

    /** Interface used for error messages
     *  Messages are typically formulated as string-valued lambda expressions
     *  so that any concatenations they contain are only computed if the
     *  message is needed.
     */
    public static interface ErrorMessage {
        public String myString();
    }

    /** Get next name without skipping to next line (unlike sc.next())
     *  @param sc the scanner from which end of line is scanned
     *  @param message the message to output if there was no name
     *  @return the name, if there was one, or an empty string
     */
    public static String nextName( Scanner sc, ErrorMessage message ) {
        sc.skip( whitespace );
        sc.skip( name );
        String s = sc.match().group();
        if ("".equals( s )) {
            Errors.warn( "Name expected: " + message.myString() );
            sc.nextLine();
        }
        return s;
    }

    /** Get next float without skipping to next line (unlike sc.nextFloat())
     *  @param sc the scanner from which end of line is scanned
     *  @param message the message to output if there was no float
     *  @return the value, if there was one, or NaN if not
     */
    public static float nextFloat( Scanner sc, ErrorMessage message ) {
        sc.skip( whitespace );
        sc.skip( number );
        String s = sc.match().group();
        if ("".equals( s )) {
            Errors.warn( "Float expected: " + message.myString() );
            sc.nextLine();
	    return Float.NaN;
        }
	// now, s is guaranteed to hold a legal float
	return Float.parseFloat( s );
    }

    /** Advance to next line and complain if is junk at the line end
     *  @see Errors
     *  @param sc the scanner from which end of line is scanned
     *  @param message gives a prefix to give context to error messages
     *  This version supports comments starting with --
     */
    public static void lineEnd( Scanner sc, ErrorMessage message ) {
        sc.skip( whitespace );
        String lineEnd = sc.nextLine();
        if ( (!lineEnd.equals( "" ))
        &&   (!lineEnd.startsWith( "--" )) ) {
            Errors.warn(
                message.myString() +
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
    private int pinNum;		      // pin num of gate for wire
    private final float delay;        // measured in seconds
    private final Gate source;        // where this wire comes from, never null
    private final String srcPin;      // what pin of source, never null
    private final Gate destination;   // where this wire goes, never null
    private final String dstPin;      // what pin of the destination, never null
    // name of a wire is source-srcpin-destination-dstpin

    /** construct a new wire by scanning its description from the source file
     */
    public Wire( Scanner sc ) throws ConstructorFailure {
	String sourceName = ScanSupport.nextName(
	    sc, ()-> "wire ???"
	);
	if ("".equals( sourceName )) throw new ConstructorFailure();

	srcPin = ScanSupport.nextName(
	    sc, ()->"wire " + sourceName + " ???"
	);
	if ("".equals( srcPin )) throw new ConstructorFailure();

	String dstName = ScanSupport.nextName(
	    sc, ()->"wire " + " " + srcPin + " ???"
	);
	if ("".equals( dstName )) throw new ConstructorFailure();

	dstPin = ScanSupport.nextName(
	    sc, ()->"wire " + " " + srcPin + " " + dstName + " ???"
	);
	if ("".equals( dstPin )) throw new ConstructorFailure();

	source = Logic.findGate( sourceName );
	destination = Logic.findGate( dstName );
	if (source == null) {
	    Errors.warn( "No such source gate: wire "
			+ sourceName + " " + srcPin + " "
			+ dstName + " " + dstPin
	    );
	    sc.nextLine();
	    throw new ConstructorFailure();
	}
	if (destination == null) {
	    Errors.warn( "No such destination gate: wire "
			+ sourceName + " " + srcPin + " "
			+ dstName + " " + dstPin
	    );
	    sc.nextLine();
	    throw new ConstructorFailure();
	}

	// take care of source and destination pins
	// the wires also pass themselves to the gates
	// so the gates are aware of which wires are
	// coming in or going out of them
	source.registerOutput( srcPin, this );
	destination.registerInput( dstPin, this );
	delay = ScanSupport.nextFloat(
	    sc, ()->"wire "
		+ sourceName + " " + srcPin + " "
		+ dstName + " " + dstPin + " ???"
	);
	if (Float.isNaN( delay )) throw new ConstructorFailure();
	if (delay < 0.0F) Errors.warn( "Negative delay: " + this.toString() );

	ScanSupport.lineEnd( sc, ()->this.toString() );
    }
    /** a void for setting the pin number that the wire is connected to
     *  0 for pin1 and 1 for pin2 of two ouput gates and just zero for
     *  one input pin gates
     */
    public void setPinNum(int i){
	this.pinNum = i;
    }
    /** schedules an input change event for the wire at the
     *  time of the sim plus the delay of the wire
     */
    public void inputChangeEvent(float time, boolean newValue){
	Simulator.schedule(time+this.delay, (float t)->
			this.outputChangeEvent(t, newValue));
    }
    /** schedule an input change event for the pin of the gate that is
     *  connected to the wire
     */
    public void outputChangeEvent(float time, boolean newValue){
	destination.inputChangeEvent(time,this.pinNum,  newValue);
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

/** Gates process inputs from Wires and deliver outputs to Wires
 *  @see Wire
 */
abstract class Gate {
    // constructors may throw this when an error prevents construction
    public static class ConstructorFailure extends Exception {}

    // fields of a gate
    public final String name;            // textual name of gate, never null!
    protected final float delay;         // the delay of this gate, in seconds
    protected LinkedList<Wire> outputs = new LinkedList<Wire>();
    protected String type;
    /** The constructor used only from within subclasses
     *  @param name used to initialize the final field
     *  @param delay used to initialize the final field
     */
    protected Gate( String name, float delay ) {
	this.name = name;
	this.delay = delay;
    }
    /** The public use this factory to construct gates
     *  @param sc the scanner from which the gate description is read
     *  @return the newly constructed gate
     */
    public static Gate factory( Scanner sc ) throws ConstructorFailure {
	String name = ScanSupport.nextName( sc, ()->"gate ???" );
	if ("".equals( name )) throw new ConstructorFailure();
	String kind = ScanSupport.nextName( sc, ()->"gate " + name + " ???" );
	if ("".equals( kind )) throw new ConstructorFailure();

	if (Logic.findGate( name ) != null) {
	    Errors.warn( "Redefinition: gate " + name + " " + kind );
	    sc.nextLine();
	    throw new ConstructorFailure();
	}

	final float delay = ScanSupport.nextFloat(
	    sc, ()->"gate " + name + " " + kind + " ???"
	);
	if (Float.isNaN( delay )) throw new ConstructorFailure();
	if (delay < 0.0F) Errors.warn(
	    "Negative delay: " + "gate " + name + " " + kind + " " + delay
	);

	final Gate newGate; // initialized by one of the alternatives below
	if ("and".equals( kind )) {
	    newGate = new AndGate( name, delay );
	} else if ("or".equals( kind )) {
	    newGate = new OrGate( name, delay );
	} else if ("not".equals( kind )) {
	    newGate = new NotGate( name, delay );
	} else if ("const".equals( kind )) {
	    newGate = new ConstGate( name, delay );
	} else {
	    Errors.warn( "Unknown gate kind: gate " + name + " " + kind );
	    sc.nextLine();
	    throw new ConstructorFailure();
	}

	ScanSupport.lineEnd( sc, ()->newGate.toString() );
	return newGate;
    }
    /** a void which schedules output changes for all of the
     *  output wires connected to a gate at the time of the sim
     *  plus the delay of the gate
     */
    public void outputChangeEvent(float time, boolean newValue){
	System.out.println("At time " + time + " "  + this.type
			   + " Gate " + this.name + " output changes to "
			   + String.valueOf(newValue));
	for( Wire w: outputs){
	    w.inputChangeEvent(time, newValue);
	}
    }
    /** a void for scheduling input changes for gates
     *
     */
    abstract void inputChangeEvent(float time, int pin, boolean newValue);

    /** tell the gate that one of its input pins is in use
     * @param pinName
     */
    public abstract void registerInput( String pinName, Wire w );

    /** tell the gate that one of its output pins is in use
     * @param pinName
     */
    public abstract void registerOutput( String pinName, Wire w );

    /** check the sanity of this gate's connections
     */
    public abstract void checkSanity();

} // class Gate

class AndGate extends Gate {
    // usage records for inputs
    private boolean in1used = false;
    private boolean in2used = false;
    private boolean[] inVal = {false, false};	// input value of gate pins
    private boolean output = false;		// output of gate
    /** The constructor used only from within class Gate
     *  @param name used to initialize the final field
     *  @param delay used to initialize the final field
     */
    protected AndGate( String name, float delay ) {
	super( name, delay );
	type = "And";
    }

    /** tell the gate that one of its input pins is in use
     *  @param pinName
     */
    public void registerInput( String pinName, Wire w ) {
        if ("in1".equals( pinName )) {
	    if (in1used) Errors.warn(
		"Multiple uses of input pin: " + name + " in1"
	    );
	    in1used = true;
	    w.setPinNum(0);
	} else if ("in2".equals( pinName )) {
	    if (in2used) Errors.warn(
		"Multiple uses of input pin: " + name + " in2"
	    );
	    in2used = true;
	    w.setPinNum(1);
	} else {
	    Errors.warn( "Illegal input pin: " + name + " " + pinName );
	}
    }

    /** tell the gate that one of its output pins is in use
     *  @param pinName
     */
    public void registerOutput( String pinName, Wire w ) {
        if ("out".equals( pinName )) {
	    outputs.add(w);
	} else {
	    Errors.warn( "Illegal output pin: " + name + " " + pinName );
	}
    }

    /** check the sanity of this gate's connections
     */
    public void checkSanity() {
	if (!in1used) Errors.warn( "Unused input pin: " + name + " in1" );
	if (!in2used) Errors.warn( "Unused input pin: " + name + " in2" );
    }

    /** reconstruct the textual description of this gate
     *  @return the textual description
     */
    public String toString() {
	return "gate " + name + " and " + delay;
    }
    public void inputChangeEvent(float time, int pin, boolean newValue){
	boolean newOut;
	inVal[pin] = newValue;
	if(inVal[0] && inVal[1]){
	    newOut = true;
	} else {
	    newOut = false;
	}
	if(newOut != output){
	    output = newOut;
	    // schedule the output change at
	    // the time plus the delay
	    Simulator.schedule( time + this.delay,
			        (float t)-> outputChangeEvent( t, output ) );
	}
    }

} // class AndGate

class OrGate extends Gate {
    // usage records for inputs
    private boolean in1used = false;
    private boolean in2used = false;
    private boolean[] inVal = {false, false};	// input value for pins
    private boolean output = false;		// output value of gate
    /** The constructor used only from within class Gate
     *  @param name used to initialize the final field
     *  @param delay used to initialize the final field
     */
    protected OrGate( String name, float delay ) {
	super( name, delay );
	type = "Or";
    }

    /** tell the gate that one of its input pins is in use
     *  @param pinName
     */
    public void registerInput( String pinName, Wire w) {
        if ("in1".equals( pinName )) {
            if (in1used) Errors.warn(
                "Multiple uses of input pin: " + name + " in1"
            );
            in1used = true;
	    // tell the wire what pin number its connected to
	    w.setPinNum(0);
        } else if ("in2".equals( pinName )) {
            if (in2used) Errors.warn(
                "Multiple uses of input pin: " + name + " in2"
            );
            in2used = true;
	    // tell the wire what pin number its connected to
	    w.setPinNum(1);
        } else {
            Errors.warn( "Illegal input pin: " + name + " " + pinName );
        }
    }
    // input change event for simulator. This checks the logic for an or gate
    public void inputChangeEvent(float time, int pin, boolean newValue){
	boolean newOut;
	// set the boolean for the specific input pin of the gate
	inVal[pin] = newValue;
	if(inVal[0] || inVal[1]){
	    newOut = true;
	} else {
	    newOut = false;
	}
	// check to see if the gate output will change and if so
	// schedule an output change event
	if(newOut != output){
	    output = newOut;
	    Simulator.schedule( time + this.delay,
			        (float t)-> outputChangeEvent( t, output ) );
	}
    }
    /** tell the gate that one of its output pins is in use
     *  @param pinName
     */
    public void registerOutput( String pinName, Wire w ) {
        if ("out".equals( pinName )) {
	    outputs.add(w);
	} else {
	    Errors.warn( "Illegal output pin: " + name + " " + pinName );
	}
    }
    /** check the sanity of this gate's connections
     */
    public void checkSanity() {
	if (!in1used) Errors.warn( "Unused input pin: " + name + " in1" );
	if (!in2used) Errors.warn( "Unused input pin: " + name + " in2" );
    }

    /** reconstruct the textual description of this gate
     *  @return the textual description
     */
    public String toString() {
	return "gate " + name + " or " + delay;
    }

} // class OrGate

class NotGate extends Gate {
    // usage records for inputs
    private boolean inUsed = false;
    private boolean outVal = false;
    /** The constructor used only from within class Gate
     *  @param name used to initialize the final field
     *  @param delay used to initialize the final field
     */
    protected NotGate( String name, float delay ) {
	super( name, delay );
	type = "Not";
	Simulator.schedule( this.delay, (float t)->
			    outputChangeEvent( t , !outVal  ) );
    }

    /** tell the gate that one of its input pins is in use
     *  @param pinName
     */
    public void registerInput( String pinName, Wire w ) {
        if ("in".equals( pinName )) {
            if (inUsed) Errors.warn(
                "Multiple uses of input pin: " + name + " in"
            );
            inUsed = true;
	    w.setPinNum(0);
	} else {
	    Errors.warn( "Illegal input pin: " + name + " " + pinName );
	}
    }

    /** tell the gate that one of its output pins is in use
     *  @param pinName
     */
    public void registerOutput( String pinName, Wire w ) {
        if ("out".equals( pinName )) {
	    outputs.add(w);
	} else {
	    Errors.warn( "Illegal output pin: " + name + " " + pinName );
	}
    }
    // input change event for simulator
    public void inputChangeEvent(float time, int pin, boolean newValue){
	// invert the input value and assign that value to the output
	// value of the gate then schedule the output change event
	outVal = !newValue;
	Simulator.schedule(time + this.delay, (float t)->
			outputChangeEvent(t, outVal));
    }
    /** check the sanity of this gate's connections
     */
    public void checkSanity() {
	if (!inUsed) Errors.warn( "Unused input pin: " + name + " in" );
    }

    /** reconstruct the textual description of this gate
     *  @return the textual description
     */
    public String toString() {
	return "gate " + name + " not " + delay;
    }
} // class NotGate

class ConstGate extends Gate {

    /** The constructor used only from within class Gate
     *  @param name used to initialize the final field
     *  @param delay used to initialize the final field
     */
    // a list of the true outputs from the const gate
    private LinkedList<Wire> trueOutputs = new LinkedList<Wire>();
    protected ConstGate( String name, float delay ) {
	super( name, delay );
	String type = "Const True";
	Simulator.schedule( this.delay, (float t)->
			    initialChangeEvent( t, true ) );
    }

    /** tell the gate that one of its input pins is in use
     *  @param pinName
     */
    public void registerInput( String pinName, Wire w ) {
	Errors.warn( "Illegal input pin: " + name + " " + pinName );
    }

    /** tell the gate that one of its output pins is in use
     *  @param pinName
     */
    public void registerOutput( String pinName, Wire w ) {
        if ("true".equals( pinName )) {
            outputs.add(w);
	    // the only outputs that ever change on a const gate are
	    // the true outputs so we need to collect them specifically
	    trueOutputs.add(w);
	} else if ("false".equals( pinName )) {
	    // there's really no reason to ever collect false outputs since
	    // they never change but I do anyway in case we need them later
	    outputs.add(w);
	} else {
	    Errors.warn( "Illegal output pin: " + name + " " + pinName );
	}
    }

    // this needs to be overriden since its an abstract void in gate
    // but it can't ever be called by the code since a correctly
    // constructed logic circuit will never have input wires into
    // const gates so the error message is mostly for the user looking
    // at the code itself it doesn't really serve a purpose
    public void inputChangeEvent(float time, int pin, boolean newValue){
	Errors.warn("input change event called on const gate " + this.name);
    }
    // an initial change event for const gate involves changing all of the
    // true outputs to true as they are initially false it should never be
    // called by outside code so its private
    private void initialChangeEvent(float time, boolean newValue){
	System.out.println("At time " + this.delay + " " + this.name +
			   " const Gate true output changes to true"
	    );
	    for( Wire w: trueOutputs){
		w.inputChangeEvent(time, newValue);
	}
    }

    /** check the sanity of this gate's connections
     */
    public void checkSanity() {
	// no sanity check; there are no input pins to check
    }

    /** reconstruct the textual description of this gate
     *  @return the textual description
     */
    public String toString() {
	return "gate " + name + " const " + delay;
    }
} // class ConstGate

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
		    gates.add( Gate.factory( sc ) );
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

    /** Check that a circuit is properly constructed
     */
    private static void sanityCheck() {
	for (Gate i: gates) i.checkSanity();
	// Bug: Are there any sensible sanity checks on wires?
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
	    sanityCheck();
	    if (Errors.count() == 0){
		// as instructed by the TA we no
		// longer print out the circuit anymore
		// but I'll leave print circuit here
		// commented out so if you want to view
		// the printed representation of the logic
		// circuit all you need to do is delete
		// the slashes
		// printCircuit();
		Simulator.run();
	    }
        } catch (FileNotFoundException e) {
	    Errors.fatal( "Can't open the file" );
	}
    }
}
