/*******************************************************************************
 * Copyright (c) 2010 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

/* define helper macros for using CUnit */
#include "cunit-helper.h"

/* each suite initializes itself via one of these functions */
extern int init_regress_glibc_suite();

/*======================================================================
 * main - The main entry point for unit-testing the CFS interposer.
 *======================================================================*/

int main(int argc, char *argv[])
{

	/* initialize the CUnit test registry */
	NEW_REGISTRY();

	/*
	 * Make sure glibc calls don't lose their normal behavior when accessing files
	 * outside of the build tree.
	 */
	if (init_regress_glibc_suite() != CUE_SUCCESS) {
		return CU_get_error();
	}

	/* Run all tests using the CUnit Basic interface */
	RUN_TESTS();

	/* clean up, and return a non-zero exit code if there were failures */
	int failures = 	CU_get_number_of_tests_failed();
	CU_cleanup_registry();
	return failures != 0;
}
