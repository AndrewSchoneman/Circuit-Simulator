/* Gate.java
 * Representations of logic gates in class Gate and its subsidiaries
 * author Douglas W. Jones
 * version 2017-11-12 (MP5 solution)
 * Adapted from Logic.java Version 2017-10-30 (the MP4 solution),
 * 
 * Bug notices in the code indicate unsolved problems
 */

import java.util.LinkedList;
import java.util.Scanner;

/** Gates process inputs from Wires and deliver outputs to Wires
 *  @see Wire
 *  @see AndGate
 *  @see OrGate
 *  @see NotGate
 *  @see ConstGate
 */
public abstract class Gate {
    // constructors may throw this when an error prevents construction
    public static class ConstructorFailure extends Exception {}

    // fields of a gate
    public final String name;            // textual name of gate, never null!
    protected final float delay;         // the delay of this gate, in seconds

    // information about gate connections and logic values is all in subclasses

    /** Constructor used only from within subclasses of class Gate
     *  @param name used to initialize the final field
     *  @param delay used to initialize the final field
     */
    protected Gate( String name, float delay ) {
	this.name = name;
	this.delay = delay;
    }

    /** The public use this factory to construct gates
     *  @param sc the scanner from which the textual gate description is read
     *  @return the newly constructed gate
     */
    public final static Gate factory( Scanner sc ) throws ConstructorFailure {
	// tempraries used while constructing a gate
	final String name;
	final String kind;
	final float delay;
	final Gate newGate;

	// scan basic fields of input line
	try {
	    name = ScanSupport.nextName(
		sc, ()->"gate ???"
	    );
	    kind = ScanSupport.nextName(
		sc, ()->"gate " + name + " ???"
	    );
	    delay = ScanSupport.nextFloat(
		sc, ()->"gate " + name + " " + kind + " ???"
	    );
	} catch (ScanSupport.NotFound e) {
	    throw new ConstructorFailure();
	}

	// check the fields
	if (Logic.findGate( name ) != null) {
	    Errors.warn( "Redefinition: gate " + name + " " + kind );
	    sc.nextLine();
	    throw new ConstructorFailure();
	}

	if (delay < 0.0F) Errors.warn(
	    "Negative delay: " + "gate " + name + " " + kind + " " + delay
	    // don't throw a failure here, we can build a gate with this error
	);

	// now construct the right kind of gate
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

    /** tell the gate that one of its input pins is in use
     *  @param w the wire that is connected
     *  @param pinName the text of a pin name
     *  @return a pin number usable as a parameter to inPinName
     */
    public abstract int registerInput( Wire w, String pinName );

    /** tell the gate that one of its output pins is in use
     *  @param w the wire that is connected
     *  @param pinName the text of a pin name
     *  @return a pin number usable as a parameter to outPinName
     */
    public abstract int registerOutput( Wire w, String pinName );

    /** get the name of the input pin, given its number
     *  @param pinNumber a pin number previously returned by registerInput
     *  @return pinName the textual name of an input pin
     */
    public abstract String inPinName( int pinNumber );

    /** get the name of the output pin, given its number
     *  @param pinNumber a pin number previously returned by registerOutput
     *  @return pinName the textual name of an output pin
     */
    public abstract String outPinName( int pinNumber );

    /** check the sanity of this gate's connections
     */
    public abstract void checkSanity();

    // Simulation methods

    /** simulate the change of one of this gate's inputs
     *  @param time the time when the input changes
     *  @param dstPin the pin that changes
     *  @param v the new logic value
     */
    public abstract void inputChangeEvent( float time, int dstPin, boolean v );

} // abstract class Gate

/** Gathers all of the properties common to single-output gates
 *  Specifically, all LogicGates drive a single list of output wires
 *  with a single output value when an OutputChangeEvent occurs.
 *  @see AndGate
 *  @see OrGate
 *  @see NotGate
 */
abstract class LogicGate extends Gate {
    // set of all wires out of this gate
    private LinkedList <Wire> outgoing = new LinkedList <Wire> ();

    // this gate's value, computed by input change events
    protected boolean value = false;

    // this gate's most recent actual output value
    private boolean outValue = false;

    /** The constructor used only from subclasses of LogicGate
     *  @param name used to initialize the final field
     *  @param delay used to initialize the final field
     */
    public LogicGate( String name, float delay ) {
	super( name, delay );
    }

    /** tell the gate that one of its output pins is in use
     *  @param w the wire that is connected
     *  @param pinName
     *  @return corresponding pin number
     */
    public final int registerOutput( Wire w, String pinName ) {
	if ("out".equals( pinName )) {
	    outgoing.add( w );
	    return 0;
	} else {
	    Errors.warn( "Illegal output pin: " + name + " " + pinName );
	    return -1;
	}
    }

    /** get the name of the output pin, given its number
     *  @param pinNumber
     *  @return pinName
     */
    public final String outPinName( int pinNumber ) {
	if (pinNumber == 0) return "out";
	return "???";
    }

    // Simulation methods

    /** Simulate an output change on this wire
     *  @param time tells when this wire's input changes
     *  Passes the new value to the input of the gate to which this wire goes.
     *  Uses the this.value field to determine the new output value.
     *  Output change events are scheduled (directly or indirectly) by the
     *  input change event of the actual gate object.
     *  @see Gate.inputChangeEvent
     */
    protected final void outputChangeEvent( float time ) {
	if (value != outValue) { // only if the output actually changes
	    outValue = value;
	    System.out.println(
		"At " + time + " " + toString() +
		" out " + " changes to " + value
	    );
	    for (Wire w: outgoing) {
		w.inputChangeEvent( time, value );
	    }
	}
    }

} // abstract class LogicGate

/** Handles the properties common to logic gates with two inputs
 *  Specifically, all two-input gates have two input wires, in1 an in2.
 *  @see AndGate
 *  @see OrGate
 *  @see LogicGate
 */
abstract class TwoInputGate extends LogicGate {
    // usage records for inputs
    protected boolean in1used = false;
    protected boolean in2used = false;

    // Boolean values of inputs
    protected boolean in1 = false;
    protected boolean in2 = false;

    /** The constructor used only from subclasses of TwoInputGate
     *  @param name used to initialize the final field
     *  @param delay used to initialize the final field
     */
    protected TwoInputGate( String name, float delay ) {
	super( name, delay );
    }

    /** tell the gate that one of its input pins is in use
     *  @param w the wire that is connected
     *  @param pinName
     *  @return corresponding pin number
     */
    public final int registerInput( Wire w, String pinName ) {
	if ("in1".equals( pinName )) {
	    if (in1used) Errors.warn(
		"Multiple uses of input pin: " + name + " in1"
	    );
	    in1used = true;
	    return 1;
	} else if ("in2".equals( pinName )) {
	    if (in2used) Errors.warn(
		"Multiple uses of input pin: " + name + " in2"
	    );
	    in2used = true;
	    return 2;
	} else {
	    Errors.warn( "Illegal input pin: " + name + " " + pinName );
	    return -1;
	}
    }

    /** get the name of the input pin, given its number
     * @param pinNumber
     * @return pinName
     */
    public final String inPinName( int pinNumber ) {
	if (pinNumber == 1) return "in1";
	if (pinNumber == 2) return "in2";
	return "???";
    }

    /** check the sanity of this gate's connections
     */
    public final void checkSanity() {
	if (!in1used) Errors.warn( "Unused input pin: " + name + " in1" );
	if (!in2used) Errors.warn( "Unused input pin: " + name + " in2" );
    }

    // Simulation methods

    /** update the output value of a gate based on its input values
     *  This is called from inputChangeEvent to delegate the response of the
     *  logic gate to the input change to the actual gate instead of this
     *  abstract class.
     *  @param time the value is updated
     */
    abstract void updateValue( float time );
    
    /** simulate the change of one of this gate's inputs
     *  @param time the time when the input changes
     *  @param dstPin the pin that changes
     *  @param v the new logic value
     */
    public void inputChangeEvent( float time, int dstPin, boolean v ) {
	if (dstPin == 1) {
	    in1 = v;
	} else if (dstPin == 2) {
	    in2 = v;
	}
	updateValue( time );
    }

} // abstract class TwoInputGate

/** Handles the properties specific to and gates.
 *  @see TwoInputGate
 *  @see LogicGate
 */
final class AndGate extends TwoInputGate {

    /** The constructor used only from within class Gate
     *  @param name used to initialize the final field
     *  @param delay used to initialize the final field
     */
    public AndGate( String name, float delay ) {
	super( name, delay );
    }

    /** reconstruct the textual description of this gate
     *  @return the textual description
     */
    public String toString() {
	return "gate " + name + " and " + delay;
    }

    // Simulation methods

    /** update the output value of a gate based on its input values
     *  @param time the value is updated
     */
    void updateValue( float time ) {
	boolean newVal = in1 & in2;
	if (newVal != value) {
	    value = newVal;
	    Simulator.schedule(
		time + (delay * 0.95f) + PRNG.randomFloat( delay * 0.1f ),
		(float t) -> outputChangeEvent( t )
	    );
	}
    }

} // class AndGate

/** Handles the properties specific to or gates.
 *  @see TwoInputGate
 *  @see LogicGate
 */
final class OrGate extends TwoInputGate {

    /** The constructor used only from within class Gate
     *  @param name used to initialize the final field
     *  @param delay used to initialize the final field
     */
    public OrGate( String name, float delay ) {
	super( name, delay );
    }

    /** reconstruct the textual description of this gate
     *  @return the textual description
     */
    public String toString() {
	return "gate " + name + " or " + delay;
    }

    // Simulation methods

    /** update the output value of a gate based on its input values
     *  @param time the value is updated
     */
    void updateValue( float time ) {
	boolean newVal = in1 | in2;
	if (newVal != value) {
	    value = newVal;
	    Simulator.schedule(
		time + (delay * 0.95f) + PRNG.randomFloat( delay * 0.1f ),
		(float t) -> outputChangeEvent( t )
	    );
	}
    }

} // class OrGate

/** Handles the properties specific to not gates.
 *  @see LogicGate
 */
final class NotGate extends LogicGate {
    // usage records for inputs
    private boolean inUsed = false;

    /** The constructor used only from within class Gate
     *  @param name used to initialize the final field
     *  @param delay used to initialize the final field
     */
    public NotGate( String name, float delay ) {
	super( name, delay );
    }

    /** tell the gate that one of its input pins is in use
     *  @param w the wire that is connected
     *  @param pinName
     *  @return corresponding pin number
     */
    public int registerInput( Wire w, String pinName ) {
	if ("in".equals( pinName )) {
	    if (inUsed) Errors.warn(
		"Multiple uses of input pin: " + name + " in"
	    );
	    inUsed = true;
	    return 0;
	} else {
	    Errors.warn( "Illegal input pin: " + name + " " + pinName );
	    return -1;
	}
    }

    /** get the name of the input pin, given its number
     * @param pinNumber
     * @return pinName
     */
    public String inPinName( int pinNumber ) {
	if (pinNumber == 0) return "in";
	return "???";
    }

    /** check the sanity of this gate's connections
     */
    public void checkSanity() {
	if (!inUsed) Errors.warn( "Unused input pin: " + name + " in" );

	// this is a good time to launch the simulation
	value = true;
	Simulator.schedule( delay, (float t) -> outputChangeEvent( t ) );
    }

    /** reconstruct the textual description of this gate
     *  @return the textual description
     */
    public String toString() {
	return "gate " + name + " not " + delay;
    }

    // Simulation methods

    /** simulate the change of one of this gate's inputs
     *  @param time the time when the input changes
     *  @param dstPin the pin that changes
     *  @param v the new logic value
     */
    public void inputChangeEvent( float time, int dstPin, boolean v ) {
	value = !v;
	Simulator.schedule(
	    time + (delay * 0.95f) + PRNG.randomFloat( delay * 0.1f ),
	    (float t) -> outputChangeEvent( t )
	);
    }

} // class NotGate

/** Handles the properties specific to const gates.
 */
final class ConstGate extends Gate {
    // set of all wires out of this gate
    private LinkedList <Wire> outgoingTrue = new LinkedList <Wire> ();
    private LinkedList <Wire> outgoingFalse = new LinkedList <Wire> ();

    /** The constructor used only from within class Gate
     *  @param name used to initialize the final field
     *  @param delay used to initialize the final field
     */
    public ConstGate( String name, float delay ) {
	super( name, delay );
    }

    /** tell the gate that one of its input pins is in use
     *  @param w the wire that is connected
     *  @param pinName
     *  @return corresponding pin number
     */
    public int registerInput( Wire w, String pinName ) {
	Errors.warn( "Illegal input pin: " + name + " " + pinName );
	return -1;
    }

    /** tell the gate that one of its output pins is in use
     *  @param w the wire that is connected
     *  @param pinName
     *  @return corresponding pin number
     */
    public int registerOutput( Wire w, String pinName ) {
	if ("true".equals( pinName )) {
	    outgoingTrue.add( w );
	    return 1;
	} else if ("false".equals( pinName )) {
	    outgoingFalse.add( w );
	    return 0;
	} else {
	    Errors.warn( "Illegal output pin: " + name + " " + pinName );
	    return -1;
	}
    }

    /** get the name of the input pin, given its number
     * @param pinNumber
     * @return pinName
     */
    public String inPinName( int pinNumber ) {
	return "???";
    }

    /** get the name of the output pin, given its number
     * @param pinNumber
     * @return pinName
     */
    public String outPinName( int pinNumber ) {
	if (pinNumber == 0) return "false";
	if (pinNumber == 1) return "true";
	return "???";
    }

    /** check the sanity of this gate's connections
     */
    public void checkSanity() {
	// no sanity check; there are no input pins to check

	// this is a good time to launch the simulation
	Simulator.schedule( delay, (float t) -> outputChangeEvent( t ) );
    }

    /** reconstruct the textual description of this gate
     *  @return the textual description
     */
    public String toString() {
	return "gate " + name + " const " + delay;
    }

    // Simulation methods

    /** simulate the change of one of this gate's inputs
     *  @param time the time when the input changes
     *  @param dstPin the pin that changes
     *  @param v the new logic value
     */
    public void inputChangeEvent( float time, int dstPin, boolean v ) {
	Errors.fatal( "Input should never change: " + toString() );
    }

    private void outputChangeEvent( float time ) {
	System.out.println(
	    "At " + time + " " + toString() + " true " + " changes to true"
	);
	for (Wire w: outgoingTrue) {
	    w.inputChangeEvent( time, true );
	}
    }

} // class ConstGate
