/*******************************************************************************
 * Copyright (c) 2011 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.arapiki.disco.scanner.legacy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.scanner.FatalBuildScannerError;
import com.arapiki.utils.os.ShellResult;
import com.arapiki.utils.os.SystemUtils;

/**
 * This class provides the main entry point for scanning a legacy shell-command-based
 * build process into a BuildStore. This is simply a wrapper for the "cfs" command-line
 * tool, which also parses the cfs.trace file (that cfs generates) and constructs
 * an equivalent BuildStore.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class LegacyBuildScanner {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/
	
	/** the default trace file name - used if no other name is provided */
	private static final String DEFAULT_TRACE_FILE_NAME = "cfs.trace";
	
	/** Trace file name */
	private String traceFilePathName;
	
	/** 
	 * The BuildStore we should generate - or null if we should parse-only, but not
	 * create a BuildStore.
	 */
	private BuildStore buildStore = null;
	
	/**
	 * The PrintStream we should send debug output to. If not set (or set to null), the
	 * debug output will be disabled.
	 */
	private PrintStream debugStream = null;
	
	/**
	 * The debug verbosity level for showing the progress of a trace. 0 = none, 1 = some,
	 * 2 = extended debug. Note that unless debugStream is also set, there will be no
	 * debug output at all.
	 */
	private int debugLevel = 0;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new LegacyBuildScanner object, using the default trace file name.
	 */
	public LegacyBuildScanner() {
		
		/* set the default trace file name */
		setTraceFile(null);
		setBuildStore(null);
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Set this scanner's trace file name. This will be generated by a call to 
	 * traceShellCommand(), or read as input by parseToBuildStore().
	 * @param traceFilePathName The name of the file to scan to/from. If null, set
	 * the path name back to the default.
	 */
	public void setTraceFile(String traceFilePathName) {

		/* if a file name is provided... */
		if (traceFilePathName != null) {
			this.traceFilePathName = traceFilePathName;
		} 
		
		/* else, null means revert to default name */
		else {
			this.traceFilePathName = DEFAULT_TRACE_FILE_NAME;
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * @return The current trace file path name
	 */
	public String getTraceFile() {
		return this.traceFilePathName;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set the BuildStore object that the trace file's information will be added to.
	 * @param buildStore The BuildStore to collect information in, or null to not collect
	 * information.
	 */
	public void setBuildStore(BuildStore buildStore) {
		this.buildStore = buildStore;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The BuildStore we'll write the trace file's data into.
	 */
	public BuildStore getBuildStore() {
		return buildStore;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Set the PrintStream object that debugging information will be displayed on.
	 * @param debugStream The debugStream, or null to not display debug information.
	 */
	public void setDebugStream(PrintStream debugStream) {
		this.debugStream = debugStream;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The current PrintStream used for displaying debug information.
	 */
	public PrintStream getDebugStream() {
		return debugStream;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set the debug level of the trace to control how much debug output is displayed
	 * as the trace proceeds.
	 * @param level 0 (none), 1 (basic debug), 2 (extended debug). Any value > 2
	 * is consider to be the same as 2.
	 */
	public void setDebugLevel(int level) {
		
		/* validate the range, and restrict to meaningful values (without giving an error) */
		if (level < 0) {
			level = 0;
		} else if (level > 2) {
			level = 2;
		}
		
		debugLevel = level;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The current debug level (0, 1 or 2).
	 */
	public int getDebugLevel() {
		return debugLevel;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Invoke a shell command and load the file/task data into the BuildStore.
	 * @param args The shell command line arguments (as is normally passed into a main()
	 * function).
	 * @throws InterruptedException The scan operation was interrupted before it completed fully
	 * @throws IOException The build command was not found, or failed to execute for some reason
	 */
	public void traceShellCommand(String args[]) throws IOException, InterruptedException {
		
		/* locate the "cfs" executable program (in $DISCO_HOME/bin) */
		String discoHome = System.getenv("DISCO_HOME");
		if (discoHome == null) {
			throw new IOException("Unable to locate cfs tool. DISCO_HOME environment variable not set.");
		}
		
		/* 
		 * Create a single command line string, by joining all the arguments. If
		 * the user specified --trace-file, we also pass that to the cfs command.
		 */
		StringBuffer sb = new StringBuffer();
		sb.append(discoHome);
		sb.append("/bin/cfs ");
		
		/* pass the trace file name (which will default to "cfs.trace" otherwise) */
		sb.append("-o ");
		sb.append(traceFilePathName);
		sb.append(" ");
		
		/* pass debug flags */
		sb.append("-d ");
		sb.append(getDebugLevel());
		sb.append(" ");
		
		/* now the command's arguments */
		for (int i = 0; i < args.length; i++) {
			sb.append(args[i]);
			sb.append(' ');
		}
		
		/* 
		 * Execute the command, echoing the output/error to our console (but don't capture it
		 * in a buffer since we won't be looking at it.
		 */
		String commandLine = sb.toString();
		ShellResult result = SystemUtils.executeShellCmd(commandLine, "", true, false);
		if (result.getReturnCode() != 0) {
			throw new IOException("Failed to execute shell command: " + commandLine);
		}
		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Take a trace file and parse the content. If the caller has invoked setBuildStore()
	 * with a BuildStore object, we'll add the parsed information to that BuildStore. If
	 * the user has invoked setDebugStream(), debug information will be dumped to that
	 * stream. In normal operation, users would therefore call setBuildStore(), but probably
	 * won't call setDebugStream().
	 */
	public void parseTraceFile() {
		/*
		 * We now have a cfs.trace file in the current directory. We should parse this file
		 * and read the content into our BuildStore.
		 */
		TraceFileScanner scanner = null;
		
		try {
			scanner = new TraceFileScanner(traceFilePathName, 
					getBuildStore(), getDebugStream(), getDebugLevel());
			scanner.parse();
			scanner.close();
			
		} catch (FileNotFoundException e) {
			throw new FatalBuildScannerError("Trace file not found: " + traceFilePathName);
			
		} catch (IOException e) {
			throw new FatalBuildScannerError("Can't parse trace file: " + traceFilePathName);
		}		
	}
	
	/*-------------------------------------------------------------------------------------*/

}
