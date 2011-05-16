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

package com.arapiki.disco.model;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.arapiki.utils.errors.ErrorCode;

/**
 * The FileAttributes class manages the attributes that can be applied to each path in
 * a FileNameSpaces object. Each attribute must first be added to the system (by name),
 * which provides a unique ID number for each attribute. Attributes (and their int/string
 * values) can then be associated with paths.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class FileAttributes {

	/**
	 * Our database manager object, used to access the database content. This is provided 
	 * to us when the FileAttributes object is first instantiated.
	 */
	private BuildStoreDB db;
	
	/**
	 * The FileNameSpaces object that these file attributes are associated with.
	 */
	private FileNameSpaces fileNameSpaces;
	
	/**
	 * Various prepared statement for database access.
	 */
	private PreparedStatement 
		selectIdFromNamePrepStmt = null,
		selectNameFromIdPrepStmt = null,
		insertFileAttrsNamePrepStmt = null,
		selectOrderedNamePrepStmt = null,
		deleteFileAttrsNamePrepStmt = null,
		countFileAttrUsagePrepStmt = null,
		insertFileAttrsPrepStmt = null,
		selectValueFromFileAttrsPrepStmt = null,
		updateFileAttrsPrepStmt = null,
		deleteFileAttrsPrepStmt = null,
		findAttrsOnPathPrepStmt = null,
		findPathsWithAttrPrepStmt = null, 
		findPathsWithAttrValuePrepStmt = null;
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create a new FileAttributes object.
	 * @param db The database manager who provides this object with database access
	 * @param fns The FileNameSpaces object that these attributes are attached to
	 */
	public FileAttributes(BuildStoreDB db, FileNameSpaces fns) {
		this.db = db;
		this.fileNameSpaces = fns;
		
		selectIdFromNamePrepStmt = db.prepareStatement("select id from fileAttrsName where name = ?");
		selectNameFromIdPrepStmt = db.prepareStatement("select name from fileAttrsName where id = ?");
		selectOrderedNamePrepStmt = db.prepareStatement("select name from fileAttrsName order by name");
		insertFileAttrsNamePrepStmt = db.prepareStatement("insert into fileAttrsName values (null, ?)");
		deleteFileAttrsNamePrepStmt = db.prepareStatement("delete from fileAttrsName where name = ?");
		countFileAttrUsagePrepStmt = db.prepareStatement("select count(*) from fileAttrs where attrId = ?");
		insertFileAttrsPrepStmt = db.prepareStatement("insert into fileAttrs values (?, ?, ?)");
		selectValueFromFileAttrsPrepStmt = db.prepareStatement("select value from fileAttrs where " +
				"pathId = ? and attrId = ?");
		updateFileAttrsPrepStmt = db.prepareStatement("update fileAttrs set value = ? "
				+ "where pathId = ? and attrId = ?");
		deleteFileAttrsPrepStmt = db.prepareStatement("delete from fileAttrs where " +
				"pathId = ? and attrId = ?");
		findAttrsOnPathPrepStmt = db.prepareStatement("select attrId from fileAttrs where pathId = ?");
		findPathsWithAttrPrepStmt = db.prepareStatement("select pathId from fileAttrs where attrId = ?");
		findPathsWithAttrValuePrepStmt = db.prepareStatement(
				"select pathId from fileAttrs where attrId = ? and value = ?");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Adds a new attribute name to the list of attributes that could possibly be associated
	 * with a path.
	 * @param attrName The name of the new attribute
	 * @return The new attribute's ID number, or ALREADY_USED if this attribute name is
	 * already in use. 
	 */
	public int newAttrName(String attrName) {
		
		/* if this name is already used, return an error */
		int existingId = getAttrIdFromName(attrName);
		if (existingId != ErrorCode.NOT_FOUND) {
			return ErrorCode.ALREADY_USED;
		}
		
		/* else, add the name to the fileAttrsName table, and retrieve its unique ID */
		try {
			insertFileAttrsNamePrepStmt.setString(1, attrName);
			db.executePrepUpdate(insertFileAttrsNamePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return db.getLastRowID();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * For a given attribute name, return the corresponding ID number.
	 * @param The attribute's name
	 * @return The attributes ID number, or NOT_FOUND if the attribute name isn't defined.
	 */
	public int getAttrIdFromName(String attrName) {
		
		Integer results[];
		try {
			selectIdFromNamePrepStmt.setString(1, attrName);
			results = db.executePrepSelectIntegerColumn(selectIdFromNamePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}

		/* if there's no entry at all, return NOT_FOUND */
		if (results.length == 0) {
			return ErrorCode.NOT_FOUND;
		}
		
		/* if there's one entry, that's good - just return that number. */
		if (results.length == 1) {
			return results[0];
		}
		
		/* else, problem - too many records */
		throw new FatalBuildStoreError("Too many records in fileAttrsName table for " + attrName);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * For a given attribute ID, return the corresponding attribute name.
	 * @param The attribute's ID number
	 * @return The attributes name, or null if the attribute name isn't defined.
	 */
	public String getAttrNameFromId(int attrId) {
		
		String results[];
		try {
			selectNameFromIdPrepStmt.setInt(1, attrId);
			results = db.executePrepSelectStringColumn(selectNameFromIdPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}

		/* if there's no entry at all, return NOT_FOUND */
		if (results.length == 0) {
			return null;
		}
		
		/* if there's one entry, that's good - just return that number. */
		if (results.length == 1) {
			return results[0];
		}
		
		/* else, problem - too many records */
		throw new FatalBuildStoreError("Too many records in fileAttrsName table for " + attrId);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return a list of all attribute names.
	 * @return A String array of attribute names. The names will be returned in 
	 * alphabetical order.
	 */
	public String[] getAttrNames() {
		
		return db.executePrepSelectStringColumn(selectOrderedNamePrepStmt);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Remove the attribute's name from the list of attributes that could possibly be associated
	 * with a path.
	 * @param attrName The name of the attribute to be removed
	 * @return OK on successful deletion, NOT_FOUND if the attribute name doesn't exist, or
	 * CANT_REMOVE if there are files still making use of this attribute.
	 */
	public int deleteAttrName(String attrName) {
		
		/* attribute names can't be deleted if they're in use - check this first */
		int attrId = getAttrIdFromName(attrName);
		try {
			countFileAttrUsagePrepStmt.setInt(1, attrId);

			/* 
			 * A select count(*) should always return 1 result. This count will be non-0
			 * if the attribute name is "in use" in the fileAttrs table.
			 */
			Integer results[] = db.executePrepSelectIntegerColumn(countFileAttrUsagePrepStmt);
			if (results[0] != 0) {
				return ErrorCode.CANT_REMOVE;
			}
		} catch (SQLException ex) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", ex);
		}
		
		/* 
		 * Try to delete the attribute name record, but also take note of whether anything was deleted.
		 * If nothing was deleted, the name is considered "not found"
		 */
		try {
			deleteFileAttrsNamePrepStmt.setString(1, attrName);
			int rowCount = db.executePrepUpdate(deleteFileAttrsNamePrepStmt);
			if (rowCount == 0) {
				return ErrorCode.NOT_FOUND;
			}
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return ErrorCode.OK;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * For the specific path (pathId), set the attribute (attrId) to the specified String
	 * value (attrValue). Note that for performance reasons, there's no error checking on
	 * the pathId and attrId values - Any integer values are acceptable, and could potentially
	 * overwrite the existing value of this attribute for this path.
	 * @param pathId The path to attach the attribute to.
	 * @param attrId The attribute to be set
	 * @param attrValue The String value to set the attribute to
	 */
	public void setAttr(int pathId, int attrId, String attrValue) {

		/* if attrValue is null, that's equivalent to deleting the record */
		if (attrValue == null) {
			deleteAttr(pathId, attrId);
			return;
		}
		
		/* 
		 * Try to update the record, but take note of whether it really was updated.
		 * If not, we'll need to insert a new record.
		 */
		try {
			updateFileAttrsPrepStmt.setString(1, attrValue);
			updateFileAttrsPrepStmt.setInt(2, pathId);
			updateFileAttrsPrepStmt.setInt(3, attrId);
			int rowCount = db.executePrepUpdate(updateFileAttrsPrepStmt);
			
			/* did the record exist? If not, insert it */
			if (rowCount == 0) {
				insertFileAttrsPrepStmt.setInt(1, pathId);
				insertFileAttrsPrepStmt.setInt(2, attrId);
				insertFileAttrsPrepStmt.setString(3, attrValue);
				db.executePrepUpdate(insertFileAttrsPrepStmt);
			}
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * For the specific path (pathId), set the attribute (attrId) to the specified integer
	 * value (attrValue). Note that for performance reasons, there's no error checking on
	 * the pathId and attrId values - Any integer values are acceptable, and could potential
	 * overwrite the existing value of this attribute for this path.
	 * @param pathId The path to attach the attribute to
	 * @param attrId The attribute to be set
	 * @param attrValue The integer value to set the attribute to
	 * @return OK if the attribute was added successfully, or BAD_VALUE if the integer
	 * is not valid (i.e. not >= 0).
	 */
	public int setAttr(int pathId, int attrId, int attrValue) {
		
		if (attrValue < 0) {
			return ErrorCode.BAD_VALUE;
		}
		setAttr(pathId, attrId, Integer.toString(attrValue));
		return ErrorCode.OK;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Fetch the specified attribute value from the specified path, returning it as a String.
	 * @param pathId The path on which the attribute is attached
	 * @param attrId The attribute whose value we want to fetch
	 * @return The attributes String value, or null if the attribute isn't set on this path
	 */
	public String getAttrAsString(int pathId, int attrId) {
		String results[];
		try {
			selectValueFromFileAttrsPrepStmt.setInt(1, pathId);
			selectValueFromFileAttrsPrepStmt.setInt(2, attrId);
			results = db.executePrepSelectStringColumn(selectValueFromFileAttrsPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}

		/* if there was no result, return null */
		if (results.length == 0) {
			return null;
		}
		
		/* ok, there's exactly one result */
		else if (results.length == 1) {
			return results[0];
		}
		
		/* else, problem - too many records */
		throw new FatalBuildStoreError("Too many records in fileAttrs table for " + 
				pathId + " / " + attrId);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Fetch the specified attribute value from the specified path, returning it as an int.
	 * @param pathId The path on which the attribute is attached
	 * @param attrId The attribute whose value we want to fetch
	 * @return The attributes positive integer value, BAD_VALUE if the attribute's
	 * value isn't a positive integer, or NOT_FOUND if the attribute wasn't set.
	 */
	public int getAttrAsInteger(int pathId, int attrId) {
		
		/* fetch the attribute's value as a String */
		String result = getAttrAsString(pathId, attrId);
		
		/* if it's not set, return NOT_FOUND */
		if (result == null) {
			return ErrorCode.NOT_FOUND;
		}
		
		/* if the string isn't in the format of an integer, return BAD_VALUE */
		int iValue;
		try {
			iValue = Integer.valueOf(result);
		} catch (NumberFormatException ex) {
			return ErrorCode.BAD_VALUE;
		}
		
		/* we can only have positive integers */
		if (iValue < 0) {
			return ErrorCode.BAD_VALUE;
		}
		
		/* return the attribute's value */
		return iValue;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Remove the attribute (attrId) that's currently associated with the specified 
	 * path (pathId). For performance reasons, no error checking is done to validate
	 * the path or attribute values. This method succeeds regardless of whether the
	 * attribute is set or not.
	 * @param pathId The path on which the attribute is attached
	 * @param attrId The attribute to be removed
	 */
	public void deleteAttr(int pathId, int attrId) {
		
		/* delete the record, whether it exists in the database or not */
		try {
			deleteFileAttrsPrepStmt.setInt(1, pathId);
			deleteFileAttrsPrepStmt.setInt(2, attrId);
			db.executePrepUpdate(deleteFileAttrsPrepStmt);
		
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a file path ID, return an array of attributes that are attached to that file.
	 * @param pathId The ID of the path the attributes are attached to
	 * @return An Integer [] array of all attributes attached to this path
	 */
	public Integer[] getAttrsOnPath(int pathId) {

		Integer results[];
		try {
			findAttrsOnPathPrepStmt.setInt(1, pathId);
			results = db.executePrepSelectIntegerColumn(findAttrsOnPathPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return results;
	}
		
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the FileSet of all paths that have the attribute set (to any value).
	 * @param The attribute to test for.
	 * @return The FileSet of all files that have this attribute set
	 */
	public FileSet getPathsWithAttr(int attrId) {

		Integer results[] = null;
		try {
			findPathsWithAttrPrepStmt.setInt(1, attrId);
			results = db.executePrepSelectIntegerColumn(findPathsWithAttrPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
				
		return new FileSet(fileNameSpaces, results);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the FileSet of all paths that have the attribute set to the specified value.
	 * @param The attribute to test for
	 * @param The value to compare against
	 * @return The FileSet of all files that have this attribute set to the specified value.
	 */
	public FileSet getPathsWithAttr(int attrId, String value) {

		Integer results[] = null;
		try {
			findPathsWithAttrValuePrepStmt.setInt(1, attrId);
			findPathsWithAttrValuePrepStmt.setString(2, value);
			results = db.executePrepSelectIntegerColumn(findPathsWithAttrValuePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return new FileSet(fileNameSpaces, results);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the FileSet of all paths that have the attribute set to the specified value.
	 * @param The attribute to test for
	 * @param The value to compare against
	 * @return The FileSet of all files that have this attribute set to the specified value.
	 */
	public FileSet getPathsWithAttr(int attrId, int value) {
		return getPathsWithAttr(attrId, String.valueOf(value));
	}
	
	/*-------------------------------------------------------------------------------------*/

}