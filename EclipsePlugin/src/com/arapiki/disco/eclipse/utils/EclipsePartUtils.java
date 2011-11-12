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

package com.arapiki.disco.eclipse.utils;

import java.util.Iterator;

import org.eclipse.jface.viewers.TreeSelection;

import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.FileNameSpaces;
import com.arapiki.disco.model.FileRecord;
import com.arapiki.disco.model.FileSet;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class EclipsePartUtils {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Given an Eclipse command handler's selection, such as when a user selects a bunch of FileRecord
	 * nodes from a TreeViewer, convert the selection into a FileSet. Selected items that are not of type
	 * FileRecord are ignored.
	 * @param selection The Eclipse command handler's selection.
	 * @return The equivalent FileSet.
	 */
	public static FileSet getFileSetFromSelection(BuildStore buildStore, TreeSelection selection) {
		
		FileNameSpaces fns = buildStore.getFileNameSpaces();
		FileSet fs = new FileSet(fns);
		Iterator<?> iter = selection.iterator();
		while (iter.hasNext()) {
			Object item = iter.next();
			if (item instanceof FileRecord) {
				fs.add((FileRecord) item);
			}
		}
		return fs;
	}

	/*-------------------------------------------------------------------------------------*/
}