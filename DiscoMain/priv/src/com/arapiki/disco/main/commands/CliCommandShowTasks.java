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

package com.arapiki.disco.main.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.arapiki.disco.main.CliUtils;
import com.arapiki.disco.main.ICliCommand;
import com.arapiki.disco.main.CliUtils.DisplayWidth;
import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.BuildTasks;
import com.arapiki.disco.model.Components;
import com.arapiki.disco.model.FileNameSpaces;
import com.arapiki.disco.model.TaskSet;

/**
 * Disco CLI Command class that implements the "show-tasks" command. See the 
 * getLongDescription() method for details of this command's features.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandShowTasks implements ICliCommand {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/* should we show component membership when displaying reports? */
	protected static boolean optionShowComps = false;

	/* do we want short output? */
	protected static boolean optionShort = false;
	
	/* do we want long output? */
	protected static boolean optionLong = false;

	/* the output format of the report (ONE_LINE, WRAPPED, NOT_WRAPPED) */
	protected DisplayWidth outputFormat = DisplayWidth.WRAPPED;
	
	/* the TaskSet used to filter our results (if -f/--filter is used) */
	protected TaskSet filterTaskSet = null;
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		// TODO Add a description
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "show-tasks";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getOptions()
	 */
	@Override
	public Options getOptions() {
		
		Options opts = new Options();

		/* add the --show-comps option */
		Option showCompsOpt = new Option("c", "show-comps", false, "Show component of each task in report output");
		opts.addOption(showCompsOpt);
		
		/* add the -s/--short option */
		Option shortOpt = new Option("s", "short", false, "Provide abbreviated output");
		opts.addOption(shortOpt);	

		/* add the -l/--long option */
		Option longOpt = new Option("l", "long", false, "Provide detailed/long output");
		opts.addOption(longOpt);
		
		/* add the -f/--filter option */
		Option filterOpt = new Option("f", "filter", true, "Colon-separated task-specs used to filter the output results");
		filterOpt.setArgName("task-spec:...");
		opts.addOption(filterOpt);
		
		return opts;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getParameterDescription()
	 */
	@Override
	public String getParameterDescription() {
		return "";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "List all tasks in the build system";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#processOptions(org.apache.commons.cli.CommandLine)
	 */
	@Override
	public void processOptions(BuildStore buildStore, CommandLine cmdLine) {
		optionShort = cmdLine.hasOption("short");
		optionLong = cmdLine.hasOption("long");
		optionShowComps = cmdLine.hasOption("show-comps");
		
		/* do we want short or long command output? We can't have both */
		if (optionShort && optionLong) {
			CliUtils.reportErrorAndExit("Can't select --short and --long in the same command");
		}
		
		outputFormat = optionShort ? DisplayWidth.ONE_LINE :
							optionLong ? DisplayWidth.NOT_WRAPPED : DisplayWidth.WRAPPED;
		
		/* fetch the subset of tasks we should filter-in */
		BuildTasks bts = buildStore.getBuildTasks();
		String filterInString = cmdLine.getOptionValue("f");
		if (filterInString != null) {
			filterTaskSet = CliUtils.getCmdLineTaskSet(bts, filterInString);
			if (filterTaskSet != null) {
				filterTaskSet.populateWithParents();
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#invoke(com.arapiki.disco.model.BuildStore, java.lang.String[])
	 */
	@Override
	public void invoke(BuildStore buildStore, String[] args) {

		CliUtils.validateArgs(getName(), args, 0, 0, "No arguments expected");
		
		BuildTasks bts = buildStore.getBuildTasks();
		FileNameSpaces fns = buildStore.getFileNameSpaces();		
		Components cmpts = buildStore.getComponents();

		/* 
		 * Display the selected task set.
		 */
		CliUtils.printTaskSet(System.out, bts, fns, cmpts, null, filterTaskSet, outputFormat, optionShowComps);
	}

	/*-------------------------------------------------------------------------------------*/
}
