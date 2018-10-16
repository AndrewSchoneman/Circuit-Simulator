/*
 * A logic simulator for gates and wires. The code borrows from roadNetwork.java
 * @version 2017-09-14 written by @author Douglas Jones. It has been modified
 * to run as a logic simulator with gates and wires instead of roads
 * and interesections. There have also beensignificant changes to the error
 * handling components of the program.
 * CS2820:0A02
 * @author Andrew Schoneman
 * @version 2017-9-17 MP2
 * Logic simulator to build a network consisting of gates and wires
 */
import java.util.LinkedList;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

// error reporing class
// see @main of logic class
class Errors{
    private static boolean canPrint = true;
    // An instance of this class is never to be created
    private Errors(){};

    public static void fatal( String command ){
	// reports a fatal error message
	System.err.println("Fatal error detected: " + command);
	System.exit(-1);
    }
    public static void warn( String command ){
	canPrint = false;
	// reports a nonfatal error with warning message
	System.err.println("Warning error detected: " + command );
    }
    public static boolean okToPrint(){
	// determines whether the network is mistake free and therefore
	// it is okay to print its textual representation
	return canPrint;
    }
}


class Wire{
/* Wires link gates
 * @see Gate
 */

    private final float delay;			// Wire delay in seconds
    private final Gate destination;		// where the wire will attach
    private final Gate source;			// source of the wire
    private final String destinationPinName;	// destination pin number
    private final String sourcePinName;		// source pin number


    public Wire( Scanner sc){
	String sourceName;		// see @findgate
	String destinationName;		// see @findgate


	if( sc.hasNext() ){
	    // get the source name for the wire
	    sourceName = sc.next();
	} else {
	    sourceName = "no source name provided";
	}
	if ( sc.hasNext() ){
	    // get the source pin name
	    sourcePinName = sc.next();
	    // wire source needs to come from output
	    if( !"out".equals(sourcePinName) && !"Out".equals(sourcePinName)){
		Errors.warn("Source pin name must be from output");
	    }
	} else {
	    sourcePinName = "no pin name provided";
	}
	if( sc.hasNext() ){
	    // the name of the gate where the wire is going to
	    destinationName = sc.next();
	} else {
	    Errors.warn("wire " +  sourceName +
			" destination pin name not provied");
	    destinationName = "no destination name provided";
	}
	if( sc.hasNext() ){
	    // the destination pin on the gate where the wire is going
	    destinationPinName = sc.next();
	    if( "out".equals(destinationPinName) ||
		"Out".equals(destinationPinName)){
		Errors.warn( "Wire " + " " +  sourceName + " " +
			     destinationName + " has output pin as input");
	    }
	} else {
	    destinationPinName = "no destination pin name";
	}
	// find the gate, if it exists, where the wire comes from
	source = Logic.findGate(sourceName);
	if(source == null){
	    Errors.warn("Source gate not found or doesn't exist");
	}
	// find the gate, if it exists, that the wire is heading to
	destination = Logic.findGate (destinationName );
	if(destination == null){
	    Errors.warn( "destination not found or doesn't exist" );
	}
	if( sc.hasNextFloat () ){
	   delay = sc.nextFloat();
	   // need to ensure that the gate delay makes sense
	   if( delay <= 0 ){
		Errors.warn( "Wire " + sourceName + " " + destinationName + " "
			     + delay +  " has a negative or zero  delay" );
	   }
	} else {
	    Errors.warn("Wire " + sourceName + " " + destinationName
			+   " has no delay or is missing a paramater" );
	    // if the delay doesn't exists it gets a nonsensical number
	    delay = 99999f;
	}
	// check to make sure an input pin isn't being used
	// more than once
	if(destination != null){
	    Logic.checkInputPin(destinationPinName, destination.name);
	}
	// done with this line so we move on
	sc.nextLine();
    }

    // a getter for the destination pin name
    public String destinationPin(){
	return this.destinationPinName;
    }

    // this is what allows the program to check for duplicate
    // input pins being used on a gate
    public String destinationGate(){
	if(this.destination != null){
	    return this.destination.name;
	} else {
	    return "foobar";
	}
    }

    // return the textual representation of a wire
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
	return "Wire " + sourceName + " "+ sourcePinName + " "  +
		destinationName + " " + " " + destinationPinName
		+ " " + delay;
    }
}
/* Gates are linked by wires
 * @see Wire
 */
class Gate{
    private final LinkedList<Wire> outgoing = new LinkedList<Wire>();
    public final String name;	// name of the gate
    public final String type;	// type of the gate (ie "or" "and" etc)
    public final float delay;	// gate delay in seconds

    public Gate(Scanner sc){
	// name the gate
	if ( sc.hasNext() ){
	    name = sc.next();
	    Gate checkName = Logic.findGate( name );
	    // checking to ensure no gate is defined twice
	    if(checkName != null){
		Errors.warn("Gate: " + name + " is illegally redefined");
	    }
	} else {
	    Errors.warn( "Gate doesn't have a name" );
	    name = "no name provided";
	}
	if ( sc.hasNext() ){
	    // get the type of the gate
	    type = sc.next();
	} else {
	    type = "no type specified";
	    Errors.warn( "Gate: " + name + " doesn't have a type");
	}
	// set the gate delay
	if( sc.hasNextFloat() ){
	    delay  = sc.nextFloat();
	    // check to make sure the delay makes sense
	    if( delay <= 0 ){
		Errors.warn( "Gate " + name + " has a negative delay" );
	    }
	} else {
	    delay = 99999f;
	    Errors.warn( "Gate " + name +
			 " has no delay or is missing a parameter" );
	}
	sc.nextLine();
    }
    // return the textual representation of the gate
    public String toString(){
	return name + " " + type + " " + delay;
    }
}


public class Logic{
    // list of all gates
    static LinkedList<Gate> networkGates = new LinkedList<Gate>();
    // list of all wires
    static LinkedList<Wire> networkWires = new LinkedList<Wire>();

    private static void readNetwork(Scanner sc){
	// keep going till we've reached the end of the file
	while( sc.hasNext() ){
	    String type = sc.next();
	    // if the word gate is present we call gate constructor
	    // then add the gate to the linked list. I also check all
	    // commands in lower case so "wire" and "Wire" are both
	    // acceptable commands from the file.
	    if("gate".equals( type.toLowerCase()  )){
		networkGates.add( new Gate( sc ));
	    // else if the word wire is present we call the wire constructor
	    // and then add the wire to the linked list
	    } else if ("wire".equals( type.toLowerCase() )){
		networkWires.add( new Wire( sc ));
	    // skip linkes with "--" as they denote comments in the file
	    } else if ("--".equals( type.toLowerCase() ) ){
		sc.nextLine();
	    // otherwise there is a command that isn't supported by the program
	    } else {
		Errors.warn( "Unknown command found: " + type );
	    }
	}
    }
    // prints out all of the gates and wires present in the network
    private static void printNetwork(){
	for(Gate gate : networkGates){
	    System.out.println( gate.toString() );
	}
	for(Wire wire : networkWires){
	    System.out.println( wire.toString() );
	}
    }
    // this is used by @wire to link wires to gates
    public static Gate findGate(String name){
	for(Gate toLink : networkGates){
	    if( name.equals( toLink.name )) {
		return toLink;
	    }
	}
	// if no gate is found null is returned by
	// the search
	return null;
    }

    // this is used b @wire to ensure no input pin is used more than
    // once
    public static void checkInputPin(String name, String inputPin){
	for(Wire  w : networkWires){
	    if(name.equals(w.destinationPin())
	       && inputPin.equals(w.destinationGate())){
		Errors.warn( "Input pin may not be used more than once ");
	    }
	}
    }
    // main to read from a file name provided by the command linke
    public static void main(String[] args){
	if(args.length < 1){
	    Errors.fatal("No file name provided");
	} else if  (args.length > 1){
	    Errors.fatal("Unexpected command Line args");
	} else try{
	    // if command linke args are the correct length we try to read
	    // from the file
	    readNetwork( new Scanner( new File( args[0] )));
	    // print the network if there were no errors in the file
	    if(Errors.okToPrint()){
		printNetwork();
	    }
	// this catches the  error where the file couldn't be opened
	} catch (FileNotFoundException e){
		Errors.fatal("File " + args[0] + " could not be opened");
	}
    }
}
