/* Logic.java
 * Main class for a program to process description of a logic circuit
 * author Douglas W. Jones
 * version 2017-11-15 (MP5 solution)
 * Adapted from Logic.java Version 2017-10-20 (the MP4 solution),
 * 
 * Bug notices in the code indicate unsolved problems
 */

import java.util.LinkedList;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/** The main class, orchestrates the building and simulation of a logic circuit.
 *  Logic circuits consist of a collection of gates connected by wires.
 *  Logic circuits are built using tools in class ScanSupport, with
 *  error reporting using class Errors.
 *  The actual simulation is done by Simulator.run()
 *  @see Simulator
 *  @see Wire
 *  @see Gate
 *  @see ScanSupport
 *  @see Errors
 */
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
	    if (Errors.count() == 0) Simulator.run();
	    // note that writeCircuit is no longer called anywhere
	} catch (FileNotFoundException e) {
	    Errors.fatal( "Can't open the file" );
	}
    }
}
