/* Simulator.java
 * Support package for discfrete-event simulation
 * author Douglas W. Jones
 * version 2017-11-15 (MP5 solution)
 * Adapted from Logic.java Version 2017-10-30 (the MP4 solution),
 * 
 * Class Simulator taken from RoadNetwork.java Version 2017-10-25
 *
 * Bug notices in the code indicate unsolved problems
 */

import java.util.LinkedList;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Random;
import java.util.PriorityQueue;
public class Simulator {

    public static abstract class Event {
	// time of event, always set by subclass constructor, effectively final
	protected final float time;

	// constructor
	Event( float t ) {
	    time = t;
	}

	// each subclass must define how to trigger it
	abstract void trigger();
    }

    private static PriorityQueue <Event> eventSet
	= new PriorityQueue <Event> (
	    (Event e1, Event e2) -> Float.compare( e1.time, e2.time )
	);

    /** schedule one new event
     *  @param e the event to schedule
     */
    public static void schedule( Event e ) {
	eventSet.add( e );
    }

    /** main loop that runs the simulation
     *  This must be called after all initial events are scheduled.
     */
    public static void run() {
	while (!eventSet.isEmpty()) {
	    Event e = eventSet.remove();
	    e.trigger();
	}
    }
} // class Simulator

