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

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.arapiki.disco.model.BuildTasks.OperationType;
import com.arapiki.disco.model.Reports.FileRecord;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestReports {

	
	/* our BuildStore, and sub-objects, used for testing */
	private BuildStore bs;
	private FileNameSpaces fns;
	private BuildTasks bts;
	private FileIncludes fis;
	
	/* our Reports object, used for testing */
	private Reports reports;
	
	
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		bs = TestCommon.getEmptyBuildStore();
		fns = bs.getFileNameSpaces();
		bts = bs.getBuildTasks();
		fis = bs.getFileIncludes();
		reports = bs.getReports();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.Reports#reportMostCommonlyAccessedFiles()}.
	 */
	@Test
	public void testReportMostCommonlyAccessedFiles() {
		
		/* add some files */
		int foxFile = fns.addFile("root", "/mydir/fox");
		int boxFile = fns.addFile("root", "/mydir/box");
		int soxFile = fns.addFile("root", "/mydir/sox");
		int roxFile = fns.addFile("root", "/mydir/rox");
		int dir = fns.addDirectory("root", "myEmptydir");

		/* create three different tasks */
		int task1 = bts.addBuildTask("command");
		int task2 = bts.addBuildTask("command");		
		int task3 = bts.addBuildTask("command");
		int task4 = bts.addBuildTask("command");
		
		/* add references from the tasks to the files - task1 uses three of them */
		bts.addFileAccess(task1, foxFile, OperationType.OP_READ);
		bts.addFileAccess(task1, boxFile, OperationType.OP_READ);
		bts.addFileAccess(task1, soxFile, OperationType.OP_READ);
		
		/* task2 only uses 2 files */
		bts.addFileAccess(task2, foxFile, OperationType.OP_READ);
		bts.addFileAccess(task2, boxFile, OperationType.OP_READ);
		
		/* task3 uses 1 file - note that task4 uses none */
		bts.addFileAccess(task3, foxFile, OperationType.OP_READ);

		/*
		 * The results of the search should be:
		 * 		foxFile - 3
		 * 		boxFile - 2
		 * 		soxFile - 1
		 */
		FileRecord results [] = reports.reportMostCommonlyAccessedFiles();
		
		/* should be 3 results passed back - the total number of files accessed */
		assertEquals(3, results.length);
		
		/* validate the order in which they were returned */
		assertEquals(foxFile, results[0].pathId);
		assertEquals(3, results[0].count);
		assertEquals(boxFile, results[1].pathId);
		assertEquals(2, results[1].count);
		assertEquals(soxFile, results[2].pathId);
		assertEquals(1, results[2].count);
		
		/* now add roxFile into the mix - access it lots */
		bts.addFileAccess(task1, roxFile, OperationType.OP_READ);
		bts.addFileAccess(task2, roxFile, OperationType.OP_READ);
		bts.addFileAccess(task3, roxFile, OperationType.OP_READ);
		bts.addFileAccess(task4, roxFile, OperationType.OP_READ);
		
		/*
		 * Check again, roxFile should now be the most popular.
		 */
		results = reports.reportMostCommonlyAccessedFiles();
		assertEquals(4, results.length);
		
		/* validate the order in which they were returned */
		assertEquals(roxFile, results[0].pathId);
		assertEquals(4, results[0].count);
		assertEquals(foxFile, results[1].pathId);
		assertEquals(3, results[1].count);
		assertEquals(boxFile, results[2].pathId);
		assertEquals(2, results[2].count);
		assertEquals(soxFile, results[3].pathId);
		assertEquals(1, results[3].count);
				
		/*
		 * Access a directory, this shouldn't be in the results.
		 */
		bts.addFileAccess(task1, dir, OperationType.OP_READ);
		results = reports.reportMostCommonlyAccessedFiles();
		assertEquals(4, results.length);		
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test method for {@link com.arapiki.disco.model.Reports#reportMostCommonlyAccessedFiles()}.
	 */
	@Test
	public void testScalabilityOfReportMostCommonlyAccessedFiles() {

		int numFiles = 10000;
		int numTasks = 100;
		int filesPerTask = 2000;
		
		bs.setFastAccessMode(true);
		Random r = new Random();
		
		/* add a bunch of files */
		for (int i = 0; i != numFiles; i++) {
			fns.addFile("root", String.valueOf(r.nextInt()));
		}
		
		/* add a (small) bunch of tasks, and associate files with them. */
		for (int i = 0; i != numTasks; i++) {
			int taskId = bts.addBuildTask("command");
			
			for (int j = 0; j != filesPerTask; j++) {
				bts.addFileAccess(taskId, r.nextInt(numFiles), OperationType.OP_READ);
			}
		}
		bs.setFastAccessMode(false);

		/* now, run a report - we don't care about the results, just the response time */
		reports.reportMostCommonlyAccessedFiles();
	}
	
	/*-------------------------------------------------------------------------------------*/	
	
	/**
	 * Test method for {@link com.arapiki.disco.model.Reports#reportMostCommonIncludersOfFile()}.
	 */
	@Test
	public void testreportMostCommonIncludersOfFile() throws Exception {
	
		/* create some files */
		int file1 = fns.addFile("root", "/mydir/files/fileA.h");
		int file2 = fns.addFile("root", "/mydir/files/fileB.h");
		int file3 = fns.addFile("root", "/mydir/files/fileC.h");
		int file4 = fns.addFile("root", "/mydir/files/fileD.h");
		int file5 = fns.addFile("root", "/mydir/files/fileE.h");
	
		/* register the include relationships, all for file2 */
		fis.addFileIncludes(file1, file2);
		fis.addFileIncludes(file3, file2);
		fis.addFileIncludes(file1, file4);
		fis.addFileIncludes(file1, file2);
		fis.addFileIncludes(file3, file2);
		fis.addFileIncludes(file4, file2);
		fis.addFileIncludes(file2, file4);
		fis.addFileIncludes(file1, file2);
		fis.addFileIncludes(file1, file2);

		/* 
		 * Results should be:
		 * 		file1  - 4 times
		 *      file3  - 2 times
		 *      file4  - 1 time
		 */
		FileRecord results[] = reports.reportMostCommonIncludersOfFile(file2);
		assertEquals(3, results.length);
		assertEquals(file1, results[0].pathId);
		assertEquals(4, results[0].count);
		assertEquals(file3, results[1].pathId);
		assertEquals(2, results[1].count);
		assertEquals(file4, results[2].pathId);
		assertEquals(1, results[2].count);
		
		/* Check for file3 which isn't included by anybody - should be empty list */
		results = reports.reportMostCommonIncludersOfFile(file3);		
		assertEquals(0, results.length);
		
		/* Check for a file that doesn't exist at all - should be empty list */
		results = reports.reportMostCommonIncludersOfFile(file3);		
		assertEquals(0, results.length);
	}
	
	/*-------------------------------------------------------------------------------------*/	

	/**
	 * Test method for {@link com.arapiki.disco.model.Reports#reportMostCommonlyAccessedFiles()}.
	 */
	@Test
	public void testScalabilityOfReportMostCommonIncludesOfFile() {

		int numFiles = 20000;
		int numIncludes = 200000;
		
		bs.setFastAccessMode(true);
		Random r = new Random();
		
		int file1 = fns.addFile("root", "/file1");
		int file2 = fns.addFile("root", "/file2");
		
		/* add a bunch of files - some include file*/
		for (int i = 0; i != numIncludes; i++) {
			fis.addFileIncludes(r.nextInt(numFiles), file1);
			fis.addFileIncludes(r.nextInt(numFiles), file2);
		}
		
		bs.setFastAccessMode(false);

		/* now, run a report - we don't care about the results, just the response time */
		FileRecord results [] = reports.reportMostCommonIncludersOfFile(file1);
	}
	
	/*-------------------------------------------------------------------------------------*/	

	/**
	 * Test method for {@link com.arapiki.disco.model.Reports#reportFilesNeverAccessed()}.
	 */
	@Test
	public void testFilesNeverAccessed() throws Exception {
		
		/* without any files in the database, return the empty list */
		FileRecord results[] = reports.reportFilesNeverAccessed();
		assertEquals(0, results.length);
		
		/* add some files and a couple of tasks */
		int file1 = fns.addFile("root", "/home/psmith/myfile1");
		int file2 = fns.addFile("root", "/home/psmith/myfile2");
		int file3 = fns.addFile("root", "/home/psmith/myfile3");
		int file4 = fns.addFile("root", "/home/psmith/myfile4");
		
		int task1 = bts.addBuildTask("task1");
		int task2 = bts.addBuildTask("task2");

		/* access some */
		bts.addFileAccess(task1, file1, OperationType.OP_READ);
		bts.addFileAccess(task1, file2, OperationType.OP_WRITE);
		
		/* file3 and file4 should both be in the results, but we don't know the order */
		results = reports.reportFilesNeverAccessed();
		assertEquals(2, results.length);
		assertTrue((results[0].pathId == file3) && (results[1].pathId == file4) ||
				(results[0].pathId == file4) && (results[1].pathId == file3));
		
		/* another task should access those same files, and the results will be the same */
		bts.addFileAccess(task2, file2, OperationType.OP_READ);
		bts.addFileAccess(task2, file1, OperationType.OP_WRITE);
		results = reports.reportFilesNeverAccessed();
		assertEquals(2, results.length);
		assertTrue((results[0].pathId == file3) && (results[1].pathId == file4) ||
				(results[0].pathId == file4) && (results[1].pathId == file3));
		
		/* now access file 3 */
		bts.addFileAccess(task1, file3, OperationType.OP_READ);
		results = reports.reportFilesNeverAccessed();
		assertEquals(1, results.length);
		assertEquals(file4, results[0].pathId);
		
		/* finally access file 3 */
		bts.addFileAccess(task2, file4, OperationType.OP_READ);
		results = reports.reportFilesNeverAccessed();
		assertEquals(0, results.length);		
	}
}