/* Gate.java
 * Representations of logic gates in class Gate and its subsidiaries
 * adapted from code by author Douglas W. Jones
 *
 * MP6 solution
 * author Andrew Schoneman
 *
 * The MP6 solution makes use of a simulation framework used
 * in RoadNetwork.java also by author Douglas W. Jones
 * version 2017-11-20
 * Adapted from Logic.java Version 2017-11-12 (the MP5 solution),
 * by author Douglas W. Jones
 *
 * Bug notices in the code indicate unsolved problems
 */

import java.util.LinkedList;
import java.util.Scanner;

/** Gates process inputs from Wires and deliver outputs to Wires,
 *  the top level Gate is a framework to create two input or Logic
 *  Gates
 *  @see Wire
 *  @see LogicGate
 *  @see TwoInputGate
 */
public abstract class Gate {
    public static class ConstructorFailure extends Exception {}

    // fields of a gate

    // textual name of gate, never null!
    public final String name;
    protected final float delay;	// the delay of this gate, in seconds

    // information about gate connections and logic values is all in subclasses

    /** Constructor used only from within subclasses of class Gate
     *  @param name used to initialize the final field, represents gate type
     *  @param delay used to initialize the final field, the delay of the gate
     *  in seconds
     */
    protected Gate( String name, float delay ) {
	this.name = name;
	this.delay = delay;
    }

    /** The public use this factory to construct gates
     *  @param sc the scanner from which the textual gate description is read
     *  @throws ConstructorFailure to suppress the construction of ill
     *  formed gates
     *  @return the newly constructed gate
     *  @see LogicGate
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

    /** tell the gate that one of its input pins is in use,
     *  in order to be created all of a Gate's input pins
     *  must be in use by a wire
     *  @param w the wire that is connected
     *  @param pinName the text of a pin name
     *  @see checkSanity
     *  @see inPinName
     *  @return a pin number usable as a parameter to inPinName
     */
    public abstract int registerInput( Wire w, String pinName );

    /** tell the gate that one of its output pins is in use
     *  an output pin can have any number of outgoing wires
     *  @param w the wire that is connected
     *  @param pinName the text of a pin name
     *  @return a pin number usable as a parameter to outPinName
     *  @see outPinName
     *  @see LogicGate
     */
    public abstract int registerOutput( Wire w, String pinName );

    /** get the name of the input pin, given its number, if a String
     *  n is a legal name of an input to a gate then that String can
     *  be found with
     *
     *  n = g.inPinName( g.registerInput(w, n) );
     *
     *  This means that inPinName gets that name of the pin registered
     *  by registerInput
     *  @param pinNumber a pin number previously returned by registerInput
     *  @return pinName the textual name of an input pin
     */
    public abstract String inPinName( int pinNumber );

    /** get the name of the output pin, given its number,that number
     *  must always be zero.
     *  @see registerOutput
     *  @param pinNumber a pin number previously returned by registerOutput
     *  @return pinName the textual name of an output pin
     */
    public abstract String outPinName( int pinNumber );

    /** check the sanity of this gate's connections by
     *  making sure that all of a Gate's input pins are
     *  in use or else the simulation can't run
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
 *  @see Wire
 *  @see Gate
 */
abstract class LogicGate extends Gate {
    // set of all wires out of this gate
    private LinkedList <Wire> outgoing = new LinkedList <Wire> ();

    // this gate's value, computed by input change events

    protected boolean value = false;

    /** a gate's most recent actual output value */
    private boolean outValue = false;

    /** The constructor used only from subclasses of LogicGate
     *  @param name used to initialize the final field
     *  @param delay used to initialize the final field
     *  @see NotGate
     */
    LogicGate( String name, float delay ) {
	super( name, delay );
    }

    /** tell the gate that one of its output pins is in use
     *  @param w the wire that is connected
     *  @param pinName the name of the pin to be set
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

    /** get the name of the output pin, given its number,
     *  if the pin number is correct
     *  @param pinNumber the output pin number
     *  @return pinName the name of the output pin
     */
    public final String outPinName( int pinNumber ) {
	if (pinNumber == 0) return "out";
	return "???";
    }

    // Simulation methods

    /** Simulate an output change on this wire
     *  Passes the new value to the input of the gate to which this wire goes.
     *  Uses the this.value field to determine the new output value.
     *  Output change events are scheduled (directly or indirectly) by the
     *  input change event of the actual gate object. So called micro events,
     *  changes where a gate's output would change from true to true, are
     *  suppressed by a low pass filter
     *  @param time tells when this wire's input changes
     *  @see inputChangeEvent
     *  @see Wire
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
 *  @see Gate
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
     *  @see AndGate
     *  @see OrGate
     */
    public TwoInputGate( String name, float delay ) {
	super( name, delay );
    }

    /** tell the gate that one of its input pins is in use
     *  @param w the wire that is connected
     *  @param pinName the name of the pin
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

    /** get the name of the input pin, given its number, if
     *  the number is valid
     * @param pinNumber number of the pin (1 or 0)
     * @return pinName the name of the pin
     */
    public final String inPinName( int pinNumber ) {
	if (pinNumber == 1) return "in1";
	if (pinNumber == 2) return "in2";
	return "???";
    }

    /** check the sanity of this gate's connections, by ensuring
     *  that all of its input pins are in use by a wire
     *  @see Wire
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

    /** simulate the change of one of this gate's inputs, this will
     *  call update value for one of its subclasses which will handle
     *  simulating the result of an input change to one of its pins
     *  @param time the time when the input changes
     *  @param dstPin the pin that changes
     *  @param v the new logic value
     *  @see AndGate
     *  @see OrGate
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
     *  and if a change in a pin's input value causes an ouput change
     *  then an output change event is scheduled which could be
     *  suppressed by the low pass filter
     *  @param time the value is updated
     *  @see outputChangeEvent
     *  @see inputChangeEvent
     */
    void updateValue( float time ) {
	boolean newVal = in1 & in2;
	if (newVal != value) {
	    value = newVal;
	    Simulator.schedule(
		new Simulator.Event(
		    time + (delay * 0.95f) + PRNG.randomFloat( delay * 0.1f )) {
		    void trigger(){
			outputChangeEvent( time );
		    }
		}
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
     *  and if the input value changes then schedule an output change
     *  event which could be suppressed due to the low pass filter
     *  @param time the value is updated
     *  @see inputChangeEvent
     *  @see outputChangeEvent
     */
    void updateValue( float time ) {
	boolean newVal = in1 | in2;
	if (newVal != value) {
	    value = newVal;
	    Simulator.schedule(
		new Simulator.Event(
		    time + (delay * 0.95f) + PRNG.randomFloat( delay * 0.1f )) {
		    void trigger(){
			outputChangeEvent( time );
		    }
		}
	    );
	}
    }

} // class OrGate

/** Handles the properties specific to not gates.
 *  @see LogicGate
 *  @see Gate
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

    /** tell the gate that its input pin is in use, an
     *  input pin can only be used by at most one wire
     *  so if another wire tries to connect to its input
     *  pin there is an error
     *  @param w the wire that is connected
     *  @param pinName the name of the pin
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
     * @param pinNumber the pin number, should be 0.
     * @return pinName the name of the pin.
     */
    public String inPinName( int pinNumber ) {
	if (pinNumber == 0) return "in";
	return "???";
    }

    /** check the sanity of this gate's connections, by
     *  ensuring that its input pin is in use by a wire
     *  also begins the simulation by scheduling changes
     *  for not gate outputs
     *  @see outputChangeEvent
     *  @see Wire
     */
    public void checkSanity() {
	if (!inUsed) Errors.warn( "Unused input pin: " + name + " in" );

	// this is a good time to launch the simulation
	value = true;
	Simulator.schedule(
	    new Simulator.Event( this.delay ){
		void trigger(){
		    outputChangeEvent( time );
		}
	    }
	);
    }

    /** reconstruct the textual description of this gate
     *  @return the textual description
     */
    public String toString() {
	return "gate " + name + " not " + delay;
    }

    // Simulation methods

    /** simulate the change of one of this gate's inputs which
     *  causes a change to its output if the event isn't filtered
     *  out by the low  pass filter
     *  @param time the time when the input changes
     *  @param dstPin the pin that changes
     *  @param v the new logic value
     *  @see outputChangeEvent
     */
    public void inputChangeEvent( float time, int dstPin, boolean v ) {
	value = !v;
	Simulator.schedule(
	    new Simulator.Event(
		time + (delay * 0.95f) + PRNG.randomFloat( delay * 0.1f )){
		    void trigger(){
			outputChangeEvent( time );
		    }
		}
	    );
    }

} // class NotGate

/** Handles the properties specific to const gates.
 *  After a const gate's delay has passed then all
 *  of the wires connected to the true output pin
 *  will turn from false  to true.
 *  @see LogicGate
 */
final class ConstGate extends Gate {
    /** set of all true wires out of the gate*/
    private LinkedList <Wire> outgoingTrue = new LinkedList <Wire> ();
    /** Set of all false wires out of the gate*/
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
     *  @param pinName the name of the pin
     *  @return corresponding pin number
     */
    public int registerInput( Wire w, String pinName ) {
	Errors.warn( "Illegal input pin: " + name + " " + pinName );
	return -1;
    }

    /** tell the gate that one of its output pins is in use
     *  @param w the wire that is connected
     *  @param pinName the name of the pin
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
     * @param pinNumber the pin number
     * @return pinName the pin name, never anything because
     * const gates have no input pins
     */
    public String inPinName( int pinNumber ) {
	return "???";
    }

    /** get the name of the output pin, given its number
     * @param pinNumber the output pin number
     * @return pinName the output pin name, 0 is false
     * 1 is true
     */
    public String outPinName( int pinNumber ) {
	if (pinNumber == 0) return "false";
	if (pinNumber == 1) return "true";
	return "???";
    }

    /** check the sanity of this gate's connections, since
     *  a const gate has no inputs there is nothing to
     *  check, however a call to this will also
     *  begin the simulation by scheduling output changes
     *  from false to true for wires connected to the true
     *  output
     *  @see outputChangeEvent
     */
    public void checkSanity() {
	// no sanity check; there are no input pins to check

	// this is a good time to launch the simulation
	Simulator.schedule(
	    new Simulator.Event( this.delay ){
		    void trigger(){
			outputChangeEvent( time );
		    }
	    }
	);
    }

    /** reconstruct the textual description of this gate
     *  @return the textual description
     */
    public String toString() {
	return "gate " + name + " const " + delay;
    }

    // Simulation methods

    /** simulate the change of one of this gate's inputs,
     *  a call to this will cause a fatal
     *  error because const gates have no inputs
     *  @param time the time when the input changes
     *  @param dstPin the pin that changes
     *  @param v the new logic value
     */
    public void inputChangeEvent( float time, int dstPin, boolean v ) {
	Errors.fatal( "Input should never change: " + toString() );
    }
    /** simulate output changes for wires connected to the
     *  true output
     *  @param time the time of the event
     */
    private void outputChangeEvent( float time ) {
	System.out.println(
	    "At " + time + " " + toString() + " true " + " changes to true"
	);
	for (Wire w: outgoingTrue) {
	    w.inputChangeEvent( time, true );
	}
    }

} // class ConstGate
