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

/** Framework for discrete event simulation
 */
class Simulator {

    /** interface used to allow lambda expressions passed to schedule method
     *  This is a functional interface.
     *  Typically, it will be used from lambda expressions passed to the
     *  schedule method.
     */
    public interface Action {
        /** trigger the action
	 *  @param time gives the time at which the action is triggered
	 */
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
     *  Simulator.schedule( someTime, (float time)->aMethodCall( time ... ) );
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
