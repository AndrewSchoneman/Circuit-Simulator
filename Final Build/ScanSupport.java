/* ScanSupport.java
 * A support class for scanning input files
 * author Douglas W. Jones
 * version 2017-11-15 (MP5 solution)
 * Adapted from Logic.java Version 2017-10-30 (the MP4 solution).
 * 
 * Class ScanSupport taken from RoadNetwork.java Version 2017-10-25
 *
 * Bug notices in the code indicate unsolved problems
 */

import java.util.regex.Pattern;
import java.util.Scanner;

/** Support methods for scanning
 * @see Errors
 */
public class ScanSupport {
    // exception thrown to indicate failure
    public static class NotFound extends Exception {}

    // patterns needed for scanning
    private static final Pattern name
	= Pattern.compile( "[a-zA-Z0-9_]*" );
    private static final Pattern intPattern
	= Pattern.compile( "-?[0-9][0-9]*|");
    private static final Pattern floatPattern
	= Pattern.compile( "-?[0-9][0-9]*\\.?[0-9]*|\\.[0-9][0-9]*|");
    private static final Pattern whitespace
	= Pattern.compile( "[ \t]*" ); // no newlines

    // interface for passing error messages
    public static interface Message {
	public String myString();
    }

    /** Get next name without skipping to next line (unlike sc.Next())
     *  @param sc the scanner from which end of line is scanned
     *  @param message the context part of the missing name error message
     *  @return the name if there was one.
     *  @throws NotFound if there wasn't one
     */
    public static String nextName( Scanner sc, Message m ) throws NotFound {
	sc.skip( whitespace );
	sc.skip( name );
	String s = sc.match().group();
	if ("".equals( s )) {
	    Errors.warn( "name expected: " + m.myString() );
	    sc.nextLine();
	    throw new NotFound();
	}
	return s;
    }

    /** Get next int without skipping to next line (unlike sc.nextInt())
     *  @param sc the scanner from which end of line is scanned
     *  @param message the message to output if there was no int
     *  @return the value if there was one
     *  @throws NotFound if there wasn't one
     */
    public static int nextInt( Scanner sc, Message m ) throws NotFound {
	sc.skip( whitespace );
	sc.skip( intPattern );
	String s = sc.match().group();
	if ("".equals( s )) {
	    Errors.warn( "Float expected: " + m.myString() );
	    sc.nextLine();
	    throw new NotFound();
	}
	// now, s is guaranteed to hold a legal int
	return Integer.parseInt( s );
    }

    /** Get next float without skipping to next line (unlike sc.nextFloat())
     *  @param sc the scanner from which end of line is scanned
     *  @param message the message to output if there was no float
     *  @return the value if there was one
     *  @throws NotFound if there wasn't one
     */
    public static float nextFloat( Scanner sc, Message m ) throws NotFound {
	sc.skip( whitespace );
	sc.skip( floatPattern );
	String s = sc.match().group();
	if ("".equals( s )) {
	    Errors.warn( "Float expected: " + m.myString() );
	    sc.nextLine();
	    throw new NotFound();
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
    public static void lineEnd( Scanner sc, Message message ) {
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
} // class ScanSupport
