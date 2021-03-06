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

package com.buildml.model;

/**
 * The interface conformed-to by any FileGroupMgr object, which represents a
 * subset of the functionality managed by a BuildStore object. A FileGroupMgr
 * deals with groupings of files.
 * <p>
 * There should be exactly one FileGroupMgr object per BuildStore object. Use the
 * BuildStore's getFileGroupMgr() method to obtain that one instance.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public interface IFileGroupMgr {

	/*=====================================================================================*
	 * METHODS THAT APPLY TO ALL GROUP TYPES
	 *=====================================================================================*/
	
	/** indicates that a file group is a "Source" file group */ 
	public static final int SOURCE_GROUP = 0;
	
	/** indicates that a file group is a "Generated" file group */ 	
	public static final int GENERATED_GROUP = 1;
	
	/** indicates that a file group is a "Merge" file group */ 
	public static final int MERGE_GROUP = 2;
	
	/** indicates that a file group is a "Filter" file group */ 
	public static final int FILTER_GROUP = 3;
	
	/**
	 * Return the type of the specified file group.
	 * 
	 * @param groupId The ID of the group to determine the type of.
	 * @return The type of the group (SOURCE_GROUP, GENERATED_GROUP, etc), or 
	 * ErrorCode.NOT_FOUND if groupId is invalid.
	 */
	int getGroupType(int groupId);
	
	/**
	 * Return the number of persistent entries in this file group (that is, paths
	 * added by addTransientPathString() are not counted).
	 * 
	 * @param groupId The ID of the file group to query.
	 * @return        The number of elements in this group, or 
	 *                ErrorCode.NOT_FOUND if the group ID is invalid.
	 */
	int getGroupSize(int groupId);
	
	/**
	 * Return the complete ordered list of files in this file group, with all paths
	 * expanded into their \@pkg_root/path form.
	 * 
	 * @param groupId The ID of the file group to query.
	 * @return        An array of paths containing the ordered list of
	 *                paths that the group expands into. Each path is
	 *                expressed relative to the \@workspace root, or a
	 *                package root, or null if groupId is invalid.
	 */
	String[] getExpandedGroupFiles(int groupId);
	
	/**
	 * Move the specified group entry from one index position to another.
	 * 
	 * @param groupId   The ID of the group that contains the entry.
	 * @param fromIndex	The index within the group from which to move the entry.
	 * @param toIndex   The index within the group to which the entry will be moved.
	 * @return          ErrorCode.OK on success, ErrorCode.NOT_FOUND if the
	 *                  groupId is invalid, or ErrorCode.OUT_OF_RANGE if either
	 *                  fromIndex or toIndex are not valid for this group.
	 */
	int moveEntry(int groupId, int fromIndex, int toIndex);
	
	/**
	 * Remove an entry from within a file group. This command can be used for all types
	 * of file group (source, generated, etc) 
	 * 
	 * @param groupId The ID of the group from which to remove an entry.
	 * @param index   The index (within the group) of the entry to remove.
	 * @return        ErrorCode.OK on success, ErrorCode.NOT_FOUND if the group
	 *                ID is not valid, or ErrorCode.OUT_OF_RANGE if the index
	 *                is out of range.
	 */
	int removeEntry(int groupId, int index);
	
	/**
	 * Delete an existing file group. After deletion, no more operations
	 * can be performed on this group. A group can only be deleted once
	 * all of its entries have first been removed.
	 * 
	 * @param  groupId The ID of the group to be removed. 
	 * @return         ErrorCode.OK on success, ErrorCode.CANT_REMOVE if the
	 *                 group still contains entries, or ErrorCode.NOT_FOUND
	 *                 if the group ID is invalid.
	 */
	int removeGroup(int groupId);
	
	/*=====================================================================================*
	 * SOURCE GROUP METHODS
	 *=====================================================================================*/
	
	/**
	 * Create a new "Source File Group" into which source files can be added.
	 * A source file group is an ordered list of path IDs from the FileMgr,
	 * each representing a source file. A particular path can appear multiple
	 * times within the group, allowing for repetition.
	 * 
	 * @param pkgId The ID of the package that this group will belong to.
	 * 
	 * @return The newly allocated unique ID number of this group, or ErrorCode.NOT_FOUND
	 * if the package ID is invalid.
	 */
	int newSourceGroup(int pkgId);
	
	/**
	 * Append the specified path to the end of the file group. The new file group entry
	 * is persisted into the BuildStore.
	 * 
	 * @param groupId The ID of the group to append to.
	 * @param pathId  The ID of the path to append to the group.
	 * @return        The 0-based index of the new entry (within the file group), or 
	 *                ErrorCode.NOT_FOUND if the group ID is invalid, 
	 *                ErrorCode.BAD_VALUE if the pathId is invalid, or
	 *                ErrorCode.INVALID_OP if the file group is not a source group.
	 */
	int addPathId(int groupId, int pathId);
	
	/**
	 * A variant of addPathId that takes an index at which the pathId will be
	 * inserted (as opposed to appending to the end of the group).
	 * 
	 * @param groupId The ID of the group to insert into.
	 * @param pathId  The ID of the path to append to the group.
	 * @param index   Index within the file group at which the new path
	 *                will be inserted.
	 * @return        The 0-based index of the new entry (within the file group), 
	 *                ErrorCode.NOT_FOUND if the group ID is invalid,
	 *                ErrorCode.BAD_VALUE if the pathId is invalid,
	 *                ErrorCode.OUT_OF_RANGE if the index is invalid, or
	 *                ErrorCode.INVALID_OP if the file group is not a source group.
	 */
	int addPathId(int groupId, int pathId, int index);

	/**
	 * Return the Path ID of the source file at the specified 0-based index.
	 * 
	 * @param groupId The ID of the group to retrieve from.
	 * @param index	  The index of the entry to be retrieved.
	 * @return The pathID at the specified index (within the group), or:
	 * 				  ErrorCode.NOT_FOUND if groupId is invalid,
	 * 				  ErrorCode.OUT_OF_RANGE if index is out of range for this group, or
	 * 				  ErrorCode.INVALID_OP if the file group is not a source group.
	 */
	int getPathId(int groupId, int index);
	
	/**
	 * Return the IDs of all members within the file group.
     *
	 * @param groupId   The file group being queried.
	 * @return			An array of all the path IDs, in their position order, or
	 *                  null if the groupId is invalid or doesn't relate to a source group.
	 */
	Integer[] getPathIds(int groupId);
	
	/**
	 * Set all the members of a specified file group. All existing members of the group
	 * are first removed.
	 * 
	 * @param groupId   The file group whose content is being set.
	 * @param members   The new members of the group.
	 * @return			ErrorCode.OK on success, ErrorCode.NOT_FOUND if groupId is invalid
	 *                  or isn't for a source file group.
	 */
	int setPathIds(int groupId, Integer[] members);
		
	/**
	 * Return the complete list of all source groups that contain the specified path
	 * as a member.
	 * 
	 * @param pathId	The ID of the path to search for.
	 * @return			A possibly-empty array of source file group IDs, or null if
	 *                  pathId is invalid.
	 */
	Integer[] getSourceGroupsContainingPath(int pathId);
	
	/*=====================================================================================*
	 * MERGE GROUP METHODS
	 *=====================================================================================*/

	/**
	 * Create a new "Merge File Group" which merges together multiple file groups.
	 * Each entry of this group is itself a group. Entries are maintained in order,
	 * and a particular sub file group may appear multiple times.
	 * 
	 * @param pkgId The ID of the package that this group will belong to.
	 * 
	 * @return The unique ID number of this new group, or ErrorCode.NOT_FOUND
	 * if the package ID is invalid.
	 */
	int newMergeGroup(int pkgId);
	
	/**
	 * Append a new sub group to the end of an existing merge file group. This
	 * change is persisted to the database.
	 * 
	 * @param groupId		The ID of the group to be modified.
	 * @param subGroupId	The ID of the new sub group to be added. 
	 * @return The index of the newly added sub group, or 
	 * 						ErrorCode.NOT_FOUND if the groupId is invalid,
	 * 						ErrorCode.BAD_VALUE if the subGroupId is invalid, 
	 * 						ErrorCode.INVALID_OP if the main group is not a merge group,
	 * 						ErrorCode.BAD_PATH if the addition would create a cycle.
	 */
	int addSubGroup(int groupId, int subGroupId);
	
	/**
	 * A variant of addSubGroup that allows a sub group to be added at a specified index
	 * within the main merge group.
	 *
	 * @param groupId		The ID of the group to be modified.
	 * @param subGroupId	The ID of the new sub group to be added. 
	 * @param index         The index (within the merge group) at which the entry will be
	 * 						inserted.
	 * @return The index of the newly added sub group, or 
	 * 						ErrorCode.NOT_FOUND if the groupId is invalid,
	 * 						ErrorCode.BAD_VALUE if the subGroupId is invalid, 
	 * 						ErrorCode.INVALID_OP if the main group is not a merge group,
	 * 						ErrorCode.OUT_OF_RANGE if the index is out of range,
	 * 						ErrorCode.BAD_PATH if the addition would create a cycle. 
	 */
	int addSubGroup(int groupId, int subGroupId, int index);
	
	/**
	 * Return the ID of the sub-file group at the specified index.
	 * 
	 * @param groupId  	The merge file group being queried.
	 * @param index    	The index of the sub-group, within the main group.
	 * @return			The group ID of the sub-group, or ErrorCode.NOT_FOUND if groupID
	 * 					is invalid, or ErrorCode.OUT_OF_RANGE if index is not valid.
	 */
	int getSubGroup(int groupId, int index);
	
	/**
	 * Return the IDs of all sub groups within the merge group.
     *
	 * @param groupId   The merge file group being queried.
	 * @return			An array of all the sub group IDs, in their position order, or
	 *                  null if the groupId is invalid or doesn't relate to a merge file group.
	 */
	Integer[] getSubGroups(int groupId);
	
	/**
	 * Set all the sub groups within the specified merge file group. All existing members
	 * of the group are first removed.
	 * 
	 * @param groupId   The file group whose content is being set.
	 * @param members   The new members of the group.
	 * @return			ErrorCode.OK on success, ErrorCode.NOT_FOUND if groupId is invalid
	 *                  or not a merge file group.
	 */
	int setSubGroups(int groupId, Integer[] members);
	
	/*=====================================================================================*
	 * GENERATED GROUP AND FILTER GROUP METHODS
	 *=====================================================================================*/

	/**
	 * Create a new "Generated File Group" into which generated files can be added.
	 * A generated file group contains an ordered list of path strings (each path 
	 * possibly appearing multiple times). Files are specified as root-relative paths
	 * (as opposed to FileMgr IDs). That is, each entry in the group has the form: 
	 * 
	 * 		\@workspace/path/to/file
	 * OR
	 *  	\@pkg_src/path/to/file
	 *  
	 * Entries may be temporary (in-memory only) or persistent (saved in the database),
	 * allowing a generated file group to hold files that are auto-generated by an
	 * action, or that are hard-coded. 
	 * 
	 * @param pkgId The ID of the package that this group will belong to.
	 * 
	 * @return The unique ID number of this new group, or ErrorCode.NOT_FOUND
	 * if the package ID is invalid.
	 */
	int newGeneratedGroup(int pkgId);
	
	/**
	 * Create a new "Filter File Group" which filters the content of an existing
	 * file group, providing a subset of the original list of files. Filtering is
	 * done based on a chain of regular expressions. Those files from the existing
	 * file group that match the regular expressions will be output from the filter
	 * file group.
	 * 
	 * @param pkgId			The package in which this filter appears.
	 * @param predGroupId	The ID of the predecessor file group we are filtering.
	 * @return The unique ID number of this new group, ErrorCode.NOT_FOUND
	 * if the package ID is invalid, or ErrorCode.BAD_VALUE if the predGroupId
	 * is not a valid file group that is already in the same package (pkgId).
	 */
	int newFilterGroup(int pkgId, int predGroupId);
	
	/**
	 * For filter groups, there must always be some other file group that this
	 * group depends on. Return the ID of this predecessor group.
	 * @param groupId	ID of the filter group.
	 * @return The ID of the filter group's predecessor group, or ErrorCode.NOT_FOUND
	 * if groupId is invalid, or ErrorCode.INVALID_OP if this is not a filter group.
	 */
	int getPredId(int groupId);
	
	/**
	 * Append a new path string into the file group. The path is expressed as being relative
	 * to the root of a package (such as \@pkg_gen/output.o). The path will be persisted to
	 * the database. This method is intended for use when hard-coding the output of an action,
	 * rather than requiring the action to auto-generate the names of its output files.
	 * 
	 * For filter groups, this method is used for adding a regular expression.
	 * 
	 * @param groupId 		The ID of the group to append to.
	 * @param path    		The package-relative path to the generated file (e.g. \@pkg_gen/output.o)
	 * @return				The index of the newly added entry, or
	 * 						ErrorCode.NOT_FOUND if the groupId is invalid,
	 * 						ErrorCode.BAD_VALUE if the path string is malformed, or
	 * 						ErrorCode.INVALID_OP if groupId isn't a generated or filter file group.
	 */
	int addPathString(int groupId, String path);

	/**
	 * A variant of addPathString that adds the new path at the specified index (within the group)
	 * rather than appending to the end of the group. All existing entries at higher index positions
	 * will be shifted up one position.
	 * 
	 * For filter groups, this method is used for adding a regular expression.
     *
	 * @param groupId 		The ID of the group to append to.
	 * @param path    		The package-relative path to the generated file (e.g. \@pkg_gen/output.o)
	 * @param index         The index within the group, at which the new entry will be added.
	 * @return				The index of the newly added entry, or
	 * 						ErrorCode.NOT_FOUND if the groupId is invalid,
	 * 						ErrorCode.OUT_OF_RANGE if the index is invalid for this group,
	 * 						ErrorCode.BAD_VALUE if the path string is malformed, or
	 * 						ErrorCode.INVALID_OP if groupId isn't a generated or filter file group.
	 */
	int addPathString(int groupId, String path, int index);
	
	/**
	 * Return the path string at the specified index within the file group.
	 * 
	 * @param groupId		The ID of the group to query.
	 * @param index			The index within the group to query.
	 * 
	 * @return The string path at the specified index within the group, or null if the
	 * groupId or index are invalid.
	 */
	String getPathString(int groupId, int index);
	
	/**
	 * Return all the elements within the file group, as Strings.
     *
	 * @param groupId   The file group being queried.
	 * @return			An array of all members, in their position order, or
	 *                  null if the groupId is invalid or doesn't relate to a filter or 
	 *                  generated group.
	 */
	String[] getPathStrings(int groupId);
	
	/**
	 * Set all the members of a specified file group. All existing members of the group
	 * are first removed.
	 * 
	 * @param groupId   The file group whose content is being set.
	 * @param members   The new members of the group.
	 * @return			ErrorCode.OK on success, 
	 * 					ErrorCode.NOT_FOUND if groupId is invalid,
	 *                  ErrorCode.BAD_VALUE if the members list is invalid, or
	 *                  ErrorCode.INVALID_OP if groupId isn't a generated or filter file group.
	 */
	int setPathStrings(int groupId, String[] members);
	
	/**
	 * Add a transient path string to the specified file group. This path is not persisted
	 * to the database, so will only exist while the current BuildStore is open. This method
	 * is intended for use when a generated file group is populated by the action that 
     * generates the group. All paths will be appended to the end of the file in the order
     * in which they're added. Since paths are not persisted, they aren't accessible via
     * getPathString(), removeEntry(), or moveEntry() and don't contribute to
	 * the size of the group via getGroupSize(). They will however appear at the end of
	 * the list returned by getExpandedGroupFiles().
	 * 
	 * @param groupId		The ID of the group to append to.
	 * @param path			The path string (e.g. \@root/path/to/file) to be appended.
	 * @return				ErrorCode.OK on success, ErrorCode.NOT_FOUND if the groupId is
	 * 						invalid, ErrorCode.BAD_VALUE if the path string is malformed, or
	 * 						ErrorCode.INVALID_OP if groupId isn't a generated file group.
	 */
	int addTransientPathString(int groupId, String path);
	
	/**
	 * Clear any transient path strings that have been added for this file group.
	 * 
	 * @param groupId The ID of the group containing the transient paths.
	 * @return ErrorCode.OK on success, or ErrorCode.NOT_FOUND if the group ID is invalid.
	 */
	int clearTransientPathStrings(int groupId);
	
	/*=====================================================================================*
	 * SUB-PACKAGE GROUP METHODS
	 *=====================================================================================*/
	
	/**
	 * Create a new "Sub-Package Group" which is essentially a reference to one of the
	 * file groups that is exported by a sub-package into one of its output slots. Since
	 * sub-packages can only appear within a single package, there's no requirement to
	 * specify the package ID.
	 * 
	 * @param subPkgId The ID of the sub-package that this file group is exported from.
	 * @param slotId   The ID of the output slot that the file group is exported into.
	 * 
	 * @return The newly allocated unique ID number of this group, or 
	 *       ErrorCode.NOT_FOUND     If the sub-package ID is invalid.
	 *       ErrorCode.BAD_VALUE     If slotId isn't a valid output slot for the sub-package,
	 *       ErrorCode.LOOP_DETECTED If creating this new sub-package would create a cycle
	 *                               in the package/sub-package hierarchy.
	 */
	int newSubPackageGroup(int subPkgId, int slotId);
	
	/**
	 * Return this sub-package group's associated sub-package.
	 * 
	 * @param groupId The file group from which to obtain the associated sub-package.
	 * 
	 * @return The associated sub-package, or
	 *      ErrorCode.NOT_FOUND  if groupId is invalid,
	 * 		ErrorCode.INVALID_OP if this is not a sub-package group.
	 */
	int getSubPkgId(int groupId);
	
	/**
	 * Return this sub-package group's associated output slot.
	 * 
	 * @param groupId The file group from which to obtain the associated output slot.
	 * 
	 * @return The associated output slot, or
	 *      ErrorCode.NOT_FOUND  if groupId is invalid,
	 * 		ErrorCode.INVALID_OP if this is not a sub-package group.
	 */
	int getSubPkgSlotId(int groupId);
	
	/*=====================================================================================*
	 * OTHER METHODS
	 *=====================================================================================*/

	/**
	 * @return The BuildStore that delegates to this FileGroupMgr.
	 */
	IBuildStore getBuildStore();
	
	/**
	 * Add the specified listener to the list of objects that are notified when
	 * a file group changes in some way.
	 * 
	 * @param listener The object to be added as a listener.
	 */
	public void addListener(IFileGroupMgrListener listener);

	/**
	 * Remove the specified listener from the list of objects to be notified when
	 * a file group changes in some way.
	 * 
	 * @param listener The object to be removed from the list of listeners.
	 */
	public void removeListener(IFileGroupMgrListener listener);
}
