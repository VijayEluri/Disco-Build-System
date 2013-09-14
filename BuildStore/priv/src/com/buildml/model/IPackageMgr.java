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

import com.buildml.model.types.FileSet;
import com.buildml.model.types.ActionSet;

/**
 * The interface conformed-to by any PackageMgr object, which represents a
 * subset of the functionality managed by a BuildStore object. A PackageMgr
 * deals with all information related to grouping files and actions
 * into packages.
 * <p>
 * There should be exactly one PackageMgr object per BuildStore object. Use
 * the BuildStore's getPackageMgr() method to obtain that one instance.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public interface IPackageMgr {

	/**
	 * Numeric constants for each of the scopes.
	 */
	public static final int SCOPE_NONE = 0;
	public static final int SCOPE_PRIVATE = 1;
	public static final int SCOPE_PUBLIC = 2;
	public static final int SCOPE_MAX = 2;

	/**
	 * @return The ID of the root folder. This is the special folder that always exists
	 * (can't be deleted). Newly created folders and packages will be inserted underneath
	 * this folder.
	 */
	public abstract int getRootFolder();
	
	/**
	 * @return The ID of the &lt;import%gt; package. This is the default package for
	 * newly imported files. 
	 */
	public abstract int getImportPackage();

	/**
	 * Add a new package to the BuildStore. By default, packages are added immediately
	 * beneath the root folder.
	 * 
	 * @param packageName The name of the new package to be added.
	 * @return The package's ID if the addition was successful, ErrorCode.INVALID_NAME
	 * if the package's name isn't valid, or ErrorCode.ALREADY_USED if the package 
	 * name is already in use (packages and folders share a name space).
	 */
	public abstract int addPackage(String packageName);

	/**
	 * Add a new folder to the BuildStore. By default, folders are added immediately
	 * beneath the root folder.
	 * 
	 * @param folderName The name of the new folder to be added.
	 * @return The folder's ID if the addition was successful, ErrorCode.INVALID_NAME
	 * if the folder's name isn't valid, or ErrorCode.ALREADY_USED if the folder's 
	 * name is already in use (packages and folders share a name space).
	 */	
	public abstract int addFolder(String folderName);
	
	/**
	 * Given an ID number, return the package or folder's name.
	 * 
	 * @param folderOrPackageId The package or folder's ID number.
	 * @return The package or folder's name, or null if the ID is invalid.
	 */
	public abstract String getName(int folderOrPackageId);
	
	/**
	 * Set the name of the folder or package. The name must be unique across
	 * all folders/packages.
	 * 
	 * @param folderOrPackageId		ID of the package or folder to rename.
	 * @param newName				The new name for the package or folder.
	 * @return ErrorCode.OK on success, ErrorCode.ALREADY_USED if the name is already
	 * use by another package or folder, ErrorCode.INVALID_NAME if the name doesn't
	 * match naming standards, and ErrorCode.NOT_FOUND if the folderOrPackageId value
	 * doesn't reference a valid package/folder.
	 */
	public abstract int setName(int folderOrPackageId, String newName);
	
	/**
	 * Given a package or folder's name, return its ID number.
	 * 
	 * @param folderOrPackageName The package or folder name.
	 * @return The package's ID number, ErrorCode.NOT_FOUND if there's no package or
	 * folder with this name.
	 */
	public abstract int getId(String folderOrPackageName);

	/**
	 * Remove the specified package from the BuildStore. The package can only be removed
	 * if there are no files or actions associated with it. A folder can only be removed
	 * if it doesn't have any child packages or folders.
	 * 
	 * @param folderOrPackageId The ID of the folder or package to be removed.
	 * @return ErrorCode.OK if the package was successfully removed, ErrorCode.CANT_REMOVE
	 * if the package is still in use, and ErrorCode.NOT_FOUND if there's no package
	 * with this name.
	 */
	public abstract int remove(int folderOrPackageId);

	/**
	 * Return an alphabetically sorted array of all the packages. The case (upper versus
	 * lower) is ignored when sorting the results. This list does not include folder
	 * names.
	 * 
	 * @return A non-empty array of package names (will always contain the "<import>" package).
	 */
	public abstract String[] getPackages();
		
	/**
	 * Obtain a list of all child folders and packages that reside immediately beneath
	 * the specified folder ID.
	 * 
	 * @param folderId The parent folder ID we are querying.
	 * @return An array of the folder's children, with folders listed first, followed by
	 * packages. The folder and package lists are both sorted alphabetically.
	 */
	public abstract Integer[] getFolderChildren(int folderId);
	
	/**
	 * Obtain the ID of the folder that contains the specified package/folder. The
	 * parent of the root folder is itself.
	 * 
	 * @param folderOrPackageId  The package or folder to find the parent of.
	 * @return The folder ID of the specified package/folder, or ErrorCode.NOT_FOUND
	 * if the folderOrPackageId doesn't refer to a valid package or folder.
	 */
	public abstract int getParent(int folderOrPackageId);
	
	/**
	 * Move the specified package or folder into a new parent folder. When moving a folder,
	 * it is important that the new parent is not the same as, or a descendant of the 
	 * folder being moved (this would cause a cycle). 
	 * 
	 * @param folderOrPackageId The folder or package for which the parent is being set.
	 * @param parentId The parent folder into which the folder/package will be moved.
	 * @return ErrorCode.OK on success, or one of the following errors:
	 *         ErrorCode.BAD_VALUE - one or both of the IDs is not valid.
	 *         ErrorCode.NOT_A_DIRECTORY - the parentId does not refer to a folder.
	 *         ErrorCode.BAD_PATH - The destination for the package/folder is invalid,
	 *         possibly because a cycle would be created.
	 */
	public abstract int setParent(int folderOrPackageId, int parentId);
	
	/**
	 * Indicates whether the folder or package ID refers to a folder, or a package.
     *
	 * @param folderOrPackageId The ID to query.
	 * @return True if the ID relates to a folder, else false.
	 */
	public abstract boolean isFolder(int folderOrPackageId);
	
	/**
	 * Indicates whether the ID refers to a valid package or folder.
	 *
     * @param folderOrPackageId The ID to query.
	 * @return True if the ID refers to a valid package or folder.
	 */
	public boolean isValid(int folderOrPackageId);

	/**
	 * Add the specified listener to the list of objects that are notified when
	 * a package changes in some way.
	 * 
	 * @param listener The object to be added as a listener.
	 */
	public void addListener(IPackageMgrListener listener);

	/**
	 * Remove the specified listener from the list of objects to be notified when
	 * a package changes in some way.
	 * 
	 * @param listener The object to be removed from the list of listeners.
	 */
	public void removeListener(IPackageMgrListener listener);
}