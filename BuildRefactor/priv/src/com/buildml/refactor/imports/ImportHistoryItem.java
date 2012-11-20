/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.refactor.imports;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IActionMgr;
import com.buildml.model.IFileMgr;
import com.buildml.model.IActionMgr.OperationType;
import com.buildml.model.IBuildStore;
import com.buildml.utils.errors.ErrorCode;

/**
 * A single indivisible operation, as performed by the ImportRefactorer class. A history item
 * contains all the adds/deletes performed by the single refactoring operation. This
 * operation can be executed, undone, and redone as necessary.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
/* package */ class ImportHistoryItem {
	
	/*=====================================================================================*
	 * PACKAGE-LEVEL TYPES/FIELDS
	 *=====================================================================================*/

	/**
	 * For each individual operation that's added to this history item, note
	 * the type of the operation (add a file, remove a file, add an action, etc).
	 */
	enum ItemOpType {
		
		/** Send a file to the trash */
		REMOVE_PATH,
		
		/** Send an action to the trash */
		REMOVE_ACTION,
		
		/** Remove an action-file relationship */
		REMOVE_ACTION_PATH_LINK,
	}

	/*=====================================================================================*
	 * PRIVATE TYPES/FIELDS
	 *=====================================================================================*/

	/** The FileMgr used by these refactorings */
	private IFileMgr fileMgr;

	/** The ActionMgr used by these refactorings */
	private IActionMgr actionMgr;
	
	/**
	 * There are multiple operations in this ImportHistoryItem. Each item has the 
	 * following format.
	 */
	private class ItemOp {
		
		/** type of the operation */
		ItemOpType type;
		
		/** path ID (for file-related operations) */
		int pathId;
		
		/** action ID (for action-related operations) */
		int actionId;
		
		/** type of action-file relationship */
		OperationType accessType;
	}
	
	/** The ordered List of operations */
	private List<ItemOp> opList;
	

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new ImportHistoryItem. This represents a single (atomic) operation
	 * that can be performed on the BuildStore, and then potentially be un-done and
	 * re-done at a later time.
	 * 
	 * @param buildStore The BuildStore on which to operate.
	 */
	ImportHistoryItem(IBuildStore buildStore) {
		this.fileMgr = buildStore.getFileMgr();
		this.actionMgr = buildStore.getActionMgr();
		
		opList = new ArrayList<ItemOp>();
	}

	/*=====================================================================================*
	 * PACKAGE-SCOPE METHODS
	 *=====================================================================================*/

	/**
	 * Perform (or re-perform) this history item. This will invoke all the necessary
	 * adds/deletes that change the underlying BuildStore. These operations should
	 * never fail (they've been precomputed as being correct), so we throw
	 * a FatalBuildStoreError if something goes wrong.
	 */
	void redo() {

		/* iterate through all the operations, invoking them as required */
		for (Iterator<ItemOp> iter = opList.iterator(); iter.hasNext();) {
			ItemOp op = (ItemOp) iter.next();
			
			switch (op.type) {
			case REMOVE_PATH:
				System.out.println(" - Moving file to trash: " + fileMgr.getPathName(op.pathId));
				if (fileMgr.movePathToTrash(op.pathId) != ErrorCode.OK) {
					throw new FatalBuildStoreError(
							"Unable to move file to trash: " + fileMgr.getPathName(op.pathId));
				}
				break;
				
			case REMOVE_ACTION:
				System.out.println(" - Moving action to trash: " + op.actionId);
				if (actionMgr.moveActionToTrash(op.actionId) != ErrorCode.OK) {
					throw new FatalBuildStoreError(
							"Unable to move action to trash: " + op.actionId);
				}
				break;
				
			case REMOVE_ACTION_PATH_LINK:
				System.out.println(" - Removing action/file access: " + 
							op.actionId + " " + fileMgr.getPathName(op.pathId));
				actionMgr.removeFileAccess(op.actionId, op.pathId);
				break;
				
			default:
				throw new FatalBuildStoreError("Encountered unrecognized operation type: " + op.type);
			}
						
		}
		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Undo a previously executed list of operations. Traverse the operation list in reverse,
	 * and perform the opposite action for each operation (e.g. for REMOVE_FILE, we actually
	 * add the file back).
	 */
	void undo() {

		/* iterate through all the operations in reverse, invoking them as required */
		for (int i = opList.size() - 1; i >= 0; i--) {
			ItemOp op = opList.get(i);
			
			switch (op.type) {
			case REMOVE_PATH:
				System.out.println(" - Reviving file from trash: " + fileMgr.getPathName(op.pathId));
				if (fileMgr.revivePathFromTrash(op.pathId) != ErrorCode.OK) {
					throw new FatalBuildStoreError(
							"Unable to revive file from trash: " + fileMgr.getPathName(op.pathId));
				}
				break;
				
			case REMOVE_ACTION:
				System.out.println(" - Reviving action from trash: " + op.actionId);
				if (actionMgr.reviveActionFromTrash(op.actionId) != ErrorCode.OK) {
					throw new FatalBuildStoreError(
							"Unable to revive action from trash: " + op.actionId);
				}
				break;
				
			case REMOVE_ACTION_PATH_LINK:
				System.out.println(" - Re-adding action/file access: " + 
						op.actionId + " " + fileMgr.getPathName(op.pathId));
				actionMgr.addFileAccess(op.actionId, op.pathId, op.accessType);
				break;
				
			default:
				throw new FatalBuildStoreError("Encountered unrecognized operation type: " + op.type);
			}
						
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add an individual action-related operation at the end of this history item.
	 * 
	 * @param op The type of operation to perform (e.g. REMOVE_ACTION)
	 * @param actionId The ID of the action on which to operate.
	 */
	void addActionOp(ItemOpType op, int actionId) {
		ItemOp item = new ItemOp();
		item.type = op;
		item.actionId = actionId;
		opList.add(item);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add an individual file-related operation at the end of this history item.
	 * 
	 * @param op The type of operation to perform (e.g. REMOVE_FILE).
	 * @param pathId The path on which to operate.
	 */
	void addPathOp(ItemOpType op, int pathId) {
		ItemOp item = new ItemOp();
		item.type = op;
		item.pathId = pathId;
		opList.add(item);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add an individual action/file-access operation at the end of this history item.
	 * @param op The type of operation to perform (e.g. REMOVE_ACTION_FILE_LINK).
	 * @param actionId The action that performs the path access.
	 * @param pathId The path being accessed.
	 * @param accessType The style of the access (OP_READ, OP_WRITE, etc).
	 */
	public void addPathAccessOp(ItemOpType op, int actionId, int pathId,  OperationType accessType) {
		ItemOp item = new ItemOp();
		item.type = op;
		item.actionId = actionId;
		item.pathId = pathId;
		item.accessType = accessType;
		opList.add(item);
	}

	/*-------------------------------------------------------------------------------------*/
}