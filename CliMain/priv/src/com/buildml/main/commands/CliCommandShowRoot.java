/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.main.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import com.buildml.main.CliUtils;
import com.buildml.main.ICliCommand;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageRootMgr;
import com.buildml.utils.errors.ErrorCode;

/**
 * BuildML CLI Command class that implements the "show-root" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandShowRoot implements ICliCommand {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/show-root.txt");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "show-root";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getOptions()
	 */
	@Override
	public Options getOptions() {
		return new Options();
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getParameterDescription()
	 */
	@Override
	public String getParameterDescription() {
		return "[ <root-name> ]";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "Show the file system path roots.";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#processOptions(org.apache.commons.cli.CommandLine)
	 */
	@Override
	public void processOptions(IBuildStore buildStore, CommandLine cmdLine) {
		/* no options */
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#invoke(com.buildml.model.BuildStore, java.lang.String[])
	 */
	@Override
	public void invoke(IBuildStore buildStore, String buildStorePath, String[] args) {
		
		CliUtils.validateArgs(getName(), args, 0, 1, "Only one root name can be specified.");

		IFileMgr fileMgr = buildStore.getFileMgr();
		IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();

		/* no arguments == display all roots */
		if (args.length == 0) {
			String [] roots = pkgRootMgr.getRoots();
			for (int i = 0; i < roots.length; i++) {
				String rootName = roots[i];
				String associatedPath = fileMgr.getPathName(pkgRootMgr.getRootPath(rootName));
				System.out.println(rootName + " " + associatedPath);
			}
		}

		/* else, one arg == show the path for this specific root */
		else {
			String rootName = args[0];
			int rootId = pkgRootMgr.getRootPath(rootName);
			if (rootId == ErrorCode.NOT_FOUND) {
				CliUtils.reportErrorAndExit("Root name not found - " + rootName + ".");
			}
			String associatedPath = fileMgr.getPathName(rootId);
			System.out.println(associatedPath);
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
