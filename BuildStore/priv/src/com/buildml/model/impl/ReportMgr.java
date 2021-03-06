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

package com.buildml.model.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IActionMgr;
import com.buildml.model.IActionMgr.OperationType;
import com.buildml.model.IActionTypeMgr;
import com.buildml.model.IFileMgr;
import com.buildml.model.IFileMgr.PathType;
import com.buildml.model.ISlotTypes.SlotDetails;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IReportMgr;
import com.buildml.model.types.FileRecord;
import com.buildml.model.types.FileSet;
import com.buildml.model.types.PackageSet;
import com.buildml.model.types.ActionSet;
import com.buildml.utils.errors.ErrorCode;

/**
 * A manager class (that supports the BuildStore class) that handles reporting of
 * information from the BuildStore. These reports are able to access the database
 * directly, rather than using the standard BuildStore APIs.
 * <p>
 * There should be exactly one ReportMgr object per BuildStore object. Use the
 * BuildStore's getReportMgr() method to obtain that one instance.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
/* package private */ class ReportMgr implements IReportMgr {
	
	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/
	
	/**
	 * Our database manager object, used to access the database content. This is provided 
	 * to us when the ReportMgr object is first instantiated.
	 */
	private BuildStoreDB db = null;
	
	/**
	 * The BuildStore managers we interact with.
	 */
	private IFileMgr fileMgr = null;
	private IActionMgr actionMgr = null;
	private IActionTypeMgr actionTypeMgr = null;

	/**
	 * Various prepared statement for database access.
	 */
	private PreparedStatement 
		selectFileAccessCountPrepStmt = null,
		selectFileIncludesCountPrepStmt = null,
		selectFilesNotUsedPrepStmt = null,
		selectFilesWithMatchingNamePrepStmt = null,
		selectDerivedFilesPrepStmt = null,
		selectInputFilesPrepStmt = null,
		selectActionsAccessingFilesPrepStmt = null,
		selectActionsAccessingFilesAnyPrepStmt = null,
		selectFilesAccessedByActionPrepStmt = null,
		selectFilesAccessedByActionAnyPrepStmt = null,
		selectWriteOnlyFilesPrepStmt = null,
		selectAllFilesPrepStmt = null,
		selectAllActionsPrepStmt = null;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new Reports manager object, which performs a lot of the reporting work
	 * on behalf of the BuildStore.
	 * 
	 * @param buildStore The BuildStore than owns this Reports object.
	 */
	public ReportMgr(BuildStore buildStore) {
		this.db = buildStore.getBuildStoreDB();
		this.fileMgr = buildStore.getFileMgr();
		this.actionMgr = buildStore.getActionMgr();
		this.actionTypeMgr = buildStore.getActionTypeMgr();
		
		selectFileAccessCountPrepStmt = db.prepareStatement(
				"select fileId, count(*) as usage from actionFiles, files " +
					"where pathType=? and (actionFiles.fileId = files.id) and (files.trashed = 0)" +
					"group by fileId order by usage desc");
		
		selectFileIncludesCountPrepStmt = db.prepareStatement(
				"select fileId1, usage from fileIncludes where fileId2 = ? order by usage desc");
		
		selectFilesNotUsedPrepStmt = db.prepareStatement("" +
				"select files.id from files left join actionFiles on (files.id = actionFiles.fileId)" +
					" where (files.pathType = " + PathType.TYPE_FILE.ordinal() + 
					") and (actionFiles.actionId is null) and (files.trashed = 0)");
		
		selectFilesWithMatchingNamePrepStmt = db.prepareStatement(
				"select files.id from files where (name like ?) and (files.trashed = 0) and " +
		        "(pathType = " + PathType.TYPE_FILE.ordinal() + ")");

		selectDerivedFilesPrepStmt = db.prepareStatement(
				"select distinct fileId from actionFiles where actionId in " +
				"(select actionId from actionFiles where fileId = ? and " +
				"operation = " + OperationType.OP_READ.ordinal() + ") " + 
				"and operation = " + OperationType.OP_WRITE.ordinal());
		
		selectInputFilesPrepStmt = db.prepareStatement(
				"select distinct fileId from actionFiles where actionId in " +
				"(select actionId from actionFiles where fileId = ? and " +
				"operation = " + OperationType.OP_WRITE.ordinal() + ") " + 
				"and operation = " + OperationType.OP_READ.ordinal());
		
		selectActionsAccessingFilesPrepStmt = db.prepareStatement(
				"select actionId from actionFiles where fileId = ? and operation = ?");
		
		selectActionsAccessingFilesAnyPrepStmt = db.prepareStatement(
				"select actionId from actionFiles where fileId = ?");
		
		selectFilesAccessedByActionPrepStmt = db.prepareStatement(
				"select fileId from actionFiles where actionId = ? and operation = ?");
		
		selectFilesAccessedByActionAnyPrepStmt = db.prepareStatement(
				"select fileId from actionFiles where actionId = ?");
		
		selectWriteOnlyFilesPrepStmt = db.prepareStatement(
				    "select writeFileId from (select distinct fileId as writeFileId from " +
				    "actionFiles where operation = " + OperationType.OP_WRITE.ordinal() + ") " +
				    "left join (select distinct fileId as readFileId from actionFiles " +
				    "where operation = " + OperationType.OP_READ.ordinal() + ") on writeFileId = readFileId " +
				      "where readFileId is null");
		
		selectAllFilesPrepStmt = db.prepareStatement("select id from files where trashed = 0");
		selectAllActionsPrepStmt = db.prepareStatement("select actionId from buildActions");
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportMostCommonlyAccessedFiles()
	 */
	@Override
	public FileRecord[] reportMostCommonlyAccessedFiles() {
		
		ArrayList<FileRecord> results = new ArrayList<FileRecord>();
		try {
			selectFileAccessCountPrepStmt.setInt(1, PathType.TYPE_FILE.ordinal());
			ResultSet rs = db.executePrepSelectResultSet(selectFileAccessCountPrepStmt);

			while (rs.next()) {
				FileRecord record = new FileRecord(rs.getInt(1));
				record.setCount(rs.getInt(2));
				results.add(record);
			}
			rs.close();
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return results.toArray(new FileRecord[0]);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportMostCommonIncludersOfFile(int)
	 */
	@Override
	public FileRecord[] reportMostCommonIncludersOfFile(int includedFile) {
		
		ArrayList<FileRecord> results = new ArrayList<FileRecord>();
		try {
			selectFileIncludesCountPrepStmt.setInt(1, includedFile);
			ResultSet rs = db.executePrepSelectResultSet(selectFileIncludesCountPrepStmt);

			while (rs.next()) {
				FileRecord record = new FileRecord(rs.getInt(1));
				record.setCount(rs.getInt(2));
				results.add(record);
			}
			rs.close();
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return results.toArray(new FileRecord[0]);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportFilesNeverAccessed()
	 */
	@Override
	public FileSet reportFilesNeverAccessed() {
		
		FileSet results = new FileSet(fileMgr);
		try {
			ResultSet rs = db.executePrepSelectResultSet(selectFilesNotUsedPrepStmt);

			while (rs.next()) {
				results.add(rs.getInt(1));
			}
			rs.close();
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return results;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportFilesThatMatchName(java.lang.String)
	 */
	@Override
	public FileSet reportFilesThatMatchName(String fileArg) {
		
		/* map any occurrences of * into %, since that's what SQL requires */
		if (fileArg != null) {
			fileArg = fileArg.replace('*', '%');
		}
		
		FileSet results = new FileSet(fileMgr);
		try {
			selectFilesWithMatchingNamePrepStmt.setString(1, fileArg);
			ResultSet rs = db.executePrepSelectResultSet(selectFilesWithMatchingNamePrepStmt);

			while (rs.next()) {
				results.add(rs.getInt(1));
			}
			rs.close();
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return results;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportActionsThatMatchName(java.lang.String)
	 */
	@Override
	public ActionSet reportActionsThatMatchName(String pattern) {
		
		Integer results[] = actionMgr.getActionsWhereSlotIsLike(IActionMgr.COMMAND_SLOT_ID, "%" + pattern + "%");
		return new ActionSet(actionMgr, results);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportDerivedFiles(com.buildml.model.types.FileSet, boolean)
	 */
	@Override
	public FileSet reportDerivedFiles(FileSet sourceFileSet, boolean reportIndirect) {
		return reportDerivedFilesHelper(sourceFileSet, reportIndirect, selectDerivedFilesPrepStmt);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportInputFiles(com.buildml.model.types.FileSet, boolean)
	 */
	@Override
	public FileSet reportInputFiles(FileSet targetFileSet, boolean reportIndirect) {
		return reportDerivedFilesHelper(targetFileSet, reportIndirect, selectInputFilesPrepStmt);
	}

	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportActionsThatAccessFiles(com.buildml.model.types.FileSet, com.buildml.model.IActionMgr.OperationType)
	 */
	@Override
	public ActionSet reportActionsThatAccessFiles(FileSet fileSet,
			OperationType opType) {
		
		/* create an empty result ActionSet */
		ActionSet results = new ActionSet(actionMgr);
		
		/* for each file in the FileSet */
		for (Iterator<Integer> iterator = fileSet.iterator(); iterator.hasNext();) {		
			int fileId = (Integer) iterator.next();
			
			/* find the actions that access this file */
			try {
				ResultSet rs;
				
				/* the case where we care about the operation type */
				if (opType != OperationType.OP_UNSPECIFIED) {
					selectActionsAccessingFilesPrepStmt.setInt(1, fileId);
					selectActionsAccessingFilesPrepStmt.setInt(2, opType.ordinal());
					rs = db.executePrepSelectResultSet(selectActionsAccessingFilesPrepStmt);
				} 
				
				/* the case where we don't care */
				else {
					selectActionsAccessingFilesAnyPrepStmt.setInt(1, fileId);
					rs = db.executePrepSelectResultSet(selectActionsAccessingFilesAnyPrepStmt);	
				}
				
				/* add the results into our ActionSet */
				while (rs.next()) {
					int actionId = rs.getInt(1);
				
					/* only add the result if it's not in the set */
					if (!results.isMember(actionId)){
						results.add(rs.getInt(1));
					}
				}
				rs.close();
			
			} catch (SQLException e) {
				throw new FatalBuildStoreError("Unable to execute SQL statement", e);
			}
		}
		
		return results;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IReportMgr#getActionsInDirectory(com.buildml.model.types.FileSet)
	 */
	@Override
	public ActionSet reportActionsInDirectory(FileSet directories) {
		ActionSet results = new ActionSet(actionMgr);
		for (int pathId : directories) {
			Integer actions[] = actionMgr.getActionsWhereSlotEquals(IActionMgr.DIRECTORY_SLOT_ID, pathId);
			if (actions != null) {
				for (int i = 0; i < actions.length; i++) {
					results.add(actions[i]);
				}
			}
		}
		return results;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportFilesAccessedByActions(com.buildml.model.types.ActionSet, com.buildml.model.IActionMgr.OperationType)
	 */
	@Override
	public FileSet reportFilesAccessedByActions(ActionSet actionSet, OperationType opType) {
		
		/* create an empty result FileSet */
		FileSet results = new FileSet(fileMgr);
		
		/* for each action in the ActionSet */
		for (Iterator<Integer> iterator = actionSet.iterator(); iterator.hasNext();) {		
			int actionId = (Integer) iterator.next();
			
			/* find the actions that access this file */
			try {
				ResultSet rs;
				
				/* the case where we care about the operation type */
				if (opType != OperationType.OP_UNSPECIFIED) {
					selectFilesAccessedByActionPrepStmt.setInt(1, actionId);
					selectFilesAccessedByActionPrepStmt.setInt(2, opType.ordinal());
					rs = db.executePrepSelectResultSet(selectFilesAccessedByActionPrepStmt);
				} 
				
				/* the case where we don't care */
				else {
					selectFilesAccessedByActionAnyPrepStmt.setInt(1, actionId);
					rs = db.executePrepSelectResultSet(selectFilesAccessedByActionAnyPrepStmt);	
				}
				
				/* add the results into our FileSet */
				while (rs.next()) {
					int fileId = rs.getInt(1);
				
					/* only add the result if it's not in the set */
					if (!results.isMember(fileId)){
						results.add(rs.getInt(1));
					}
				}
				rs.close();
			
			} catch (SQLException e) {
				throw new FatalBuildStoreError("Unable to execute SQL statement", e);
			}
		}
		
		return results;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportWriteOnlyFiles()
	 */
	@Override
	public FileSet reportWriteOnlyFiles() {
		
		FileSet results = new FileSet(fileMgr);
		try {
			ResultSet rs = db.executePrepSelectResultSet(selectWriteOnlyFilesPrepStmt);

			while (rs.next()) {
				results.add(rs.getInt(1));
			}
			rs.close();
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return results;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportAllFiles()
	 */
	@Override
	public FileSet reportAllFiles() {
		FileSet results = new FileSet(fileMgr);
		try {
			ResultSet rs = db.executePrepSelectResultSet(selectAllFilesPrepStmt);

			while (rs.next()) {
				results.add(rs.getInt(1));
			}
			rs.close();
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return results;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportAllActions()
	 */
	@Override
	public ActionSet reportAllActions() {
		ActionSet results = new ActionSet(actionMgr);
		try {
			ResultSet rs = db.executePrepSelectResultSet(selectAllActionsPrepStmt);

			while (rs.next()) {
				results.add(rs.getInt(1));
			}
			rs.close();
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return results;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportFilesFromPackageSet(com.buildml.model.types.PackageSet)
	 */
	@Override
	public FileSet reportFilesFromPackageSet(PackageSet pkgSet) {
		FileSet results = new FileSet(fileMgr);
		IPackageMgr pkgMgr = pkgSet.getBuildStore().getPackageMgr();
		
		/*
		 * Form the (complex) query string, which considers each package/scope individually.
		 */
		StringBuffer sb = new StringBuffer(256);
		sb.append("select memberId from packageMembers where memberType = " + 
						IPackageMemberMgr.TYPE_FILE + " and ");
		int memberCount = 0;
		
		String pkgList[] = pkgMgr.getPackages();
		for (String pkgName : pkgList) {
			int pkgId = pkgMgr.getId(pkgName);
			if (pkgId != ErrorCode.NOT_FOUND) {
				
				/* is this package in the set? */
				boolean hasPrivate = pkgSet.isMember(pkgId, IPackageMemberMgr.SCOPE_PRIVATE);
				boolean hasPublic = pkgSet.isMember(pkgId, IPackageMemberMgr.SCOPE_PUBLIC);
		
				/* do we need a "or" between neighboring tests? */
				if (hasPrivate || hasPublic) {
					memberCount++;
					if (memberCount > 1) {
						sb.append(" or ");
					}
				}
				
				/* form the condition for comparing the file's packages/scope */
				if (hasPrivate && hasPublic) {
					sb.append("(pkgId == " + pkgId + ")");
				} else if (hasPrivate) {
					sb.append("((pkgId == " + pkgId + 
								") and (scopeId == " + IPackageMemberMgr.SCOPE_PRIVATE + "))");
				} else if (hasPublic) {
					sb.append("((pkgId == " + pkgId + 
								") and (scopeId == " + IPackageMemberMgr.SCOPE_PUBLIC + "))");
				}
				
			}
		}
		
		/* if the package set was empty, so to is the result set */
		if (memberCount == 0) {
			return results;
		}
		
		ResultSet rs = db.executeSelectResultSet(sb.toString());
		try {
			while (rs.next()) {
				results.add(rs.getInt(1));
			}
			rs.close();
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return results;		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportActionsFromPackageSet(com.buildml.model.types.PackageSet)
	 */
	@Override
	public ActionSet reportActionsFromPackageSet(PackageSet pkgSet) {
		ActionSet results = new ActionSet(actionMgr);
		IPackageMgr pkgMgr = pkgSet.getBuildStore().getPackageMgr();
		
		/*
		 * Form the (complex) query string.
		 */
		StringBuffer sb = new StringBuffer(256);
		sb.append("select memberId from packageMembers where memberType = " + 
						IPackageMemberMgr.TYPE_ACTION + " and ");
		int memberCount = 0;
		
		String pkgList[] = pkgMgr.getPackages();
		for (String pkgName : pkgList) {
			int pkgId = pkgMgr.getId(pkgName);
			if (pkgId != ErrorCode.NOT_FOUND) {
				
				/* is this package in the set? */
				boolean isMember = pkgSet.isMember(pkgId, IPackageMemberMgr.SCOPE_PUBLIC);
		
				/* do we need a "or" between neighboring tests? */
				if (isMember) {
					memberCount++;
					if (memberCount > 1) {
						sb.append(" or ");
					}
					
					/* form the condition for comparing the action's package. */
					sb.append("(pkgId == " + pkgId + ")");
				}
			}
		}
		
		/* if the package set was empty, so too is the result set */
		if (memberCount == 0) {
			return results;
		}
		
		ResultSet rs = db.executeSelectResultSet(sb.toString());
		try {
			while (rs.next()) {
				results.add(rs.getInt(1));
			}
			rs.close();
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return results;
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * A helper method for reportDerivedFiles and reportInputFiles that both use the same
	 * algorithm, but a slightly different SQL query.
	 * 
	 * @param startFileSet The set of files that we're deriving from, or that are used as
	 * the target of the derivation.
	 * @param reportIndirect True if we should do multiple iterations of derivation.
	 * @param sqlStatement The prepared SQL statement to find derived/input files.
	 * @return The result FileSet.
	 */
	private FileSet reportDerivedFilesHelper(FileSet startFileSet, boolean reportIndirect,
			PreparedStatement sqlStatement) {
		
		/* 
		 * Create a new empty FileSet for tracking all the results. Each time we
		 * iterate through the loop, we merge the results into this FileSet
		 */
		FileSet results = new FileSet(fileMgr);
		
		/* the first set of files to analyze */
		FileSet nextFileSet = startFileSet;
		
		/* 
		 * This variable is used to track the progress of finding new results.
		 * If it doesn't change from one iteration to the next, we stop the iterations.
		 */
		int lastNumberOfResults = 0;

		/* 
		 * Start the iterations. If "reportIndirect" is false, we'll only execute
		 * the loop once. Each time we go around the loop, we take the files
		 * from the previous round and treat them as the new set of start files.
		 */
		boolean done = false;
		do {
			
			/* empty FileSet to collect this round's set of results */
			FileSet thisRoundOfResults = new FileSet(fileMgr);

			/* iterate through each of the files in this round's FileSet */
			for (int fileId : nextFileSet) {

				/* determine the direct input/output files for this file, and add them to the result set */
				try {
					sqlStatement.setInt(1, fileId);
					ResultSet rs = db.executePrepSelectResultSet(sqlStatement);

					while (rs.next()) {
						thisRoundOfResults.add(rs.getInt(1));
					}
					rs.close();

				} catch (SQLException e) {
					throw new FatalBuildStoreError("Unable to execute SQL statement", e);
				}
			}

			/* 
			 * Prepare to repeat the process by using the results from this iteration as
			 * the input to the next iteration.
			 */
			nextFileSet = thisRoundOfResults;
		
			/*
			 * Merge this cycle's results into our final result set
			 */
			results.mergeSet(thisRoundOfResults);
			
			/* are we done? Did we find any new results in this iteration? */
			int thisNumberOfResults = results.size();
			done = (thisNumberOfResults == lastNumberOfResults);
			lastNumberOfResults = thisNumberOfResults;
			
		} while (reportIndirect && !done);
		
		/* return the combined set of results */
		return results;
	}

	/*-------------------------------------------------------------------------------------*/
}
