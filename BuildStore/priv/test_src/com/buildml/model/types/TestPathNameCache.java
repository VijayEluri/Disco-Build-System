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

package com.buildml.model.types;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.buildml.model.types.PathNameCache;
import com.buildml.model.types.PathNameCache.PathNameCacheKey;
import com.buildml.model.types.PathNameCache.PathNameCacheValue;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestPathNameCache {

	/**
	 * The FileNameCache object under test
	 */
	PathNameCache fnc;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		/* create a cache with maximum size of 5. The perfect size for testing */
		fnc = new PathNameCache(5);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the basic manipulation of the FileNameCacheKey object type, which is a nested
	 * class within the FileNameCache class.
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testFileNameCacheKey() throws Exception {
		
		/* instantiate a bunch of objects */
		PathNameCacheKey key1 = fnc.new PathNameCacheKey(1, "banana");
		PathNameCacheKey key2 = fnc.new PathNameCacheKey(1, "peach");
		PathNameCacheKey key3 = fnc.new PathNameCacheKey(1, "cucumber");
		PathNameCacheKey key4 = fnc.new PathNameCacheKey(223456, "orange");
		PathNameCacheKey key5 = fnc.new PathNameCacheKey(1, "cucumber");
		
		/* test the accessors */
		assertEquals(1, key1.getParentPathId());
		assertEquals("banana", key1.getChildPathName());
		assertEquals(223456, key4.getParentPathId());
		assertEquals("orange", key4.getChildPathName());
		
		/* compare various keys for equality - testing our custom-built equals method */
		assertEquals(key1, key1);
		assertEquals(key2, key2);
		assertNotSame(key1, key2);
		assertNotSame(key2, key3);
		assertEquals(key3, key5);
		
		/* compare hashCode values - testing our custom-built hashCode method */
		assertEquals(key3.hashCode(), key5.hashCode());
		assertNotSame(key1.hashCode(), key2.hashCode());
		assertNotSame(key3.hashCode(), key4.hashCode());
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the creation and use of FileNameCacheValue objects. This is even simpler than
	 * the previous test case because there's no custom equals/hashCode.
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testFileNameCacheValue() throws Exception {
		
		/* create a number of objects */
		PathNameCacheValue value1 = fnc.new PathNameCacheValue(1, 1);
		PathNameCacheValue value2 = fnc.new PathNameCacheValue(11, 2);
		PathNameCacheValue value3 = fnc.new PathNameCacheValue(1011, 1);
		
		/* test the accessors */
		assertEquals(1, value1.getChildPathId());
		assertEquals(1, value1.getChildType());
		assertEquals(11, value2.getChildPathId());
		assertEquals(2, value2.getChildType());
		assertEquals(1011, value3.getChildPathId());
		assertEquals(1, value3.getChildType());
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test that objects can be added to the cache and then retrieved later.
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testSimpleAccess() throws Exception {

		/* check that initially the values don't exist */
		assertNull(fnc.get(1, "womble"));
		assertNull(fnc.get(200, "bungle"));
		
		/* now add a value and make sure it exists */
		fnc.put(1, "womble", 200, 1);
		PathNameCacheValue fncv = fnc.get(1, "womble");
		assertNotNull(fncv);
		assertEquals(200, fncv.getChildPathId());
		assertEquals(1, fncv.getChildType());

		/* other entries should still not exist */
		assertNull(fnc.get(2, "womble"));
		assertNull(fnc.get(1, "womblf"));
		assertNull(fnc.get(2, "womblf"));
		
		/* add the second object to the cache */
		fnc.put(200, "bungle", 234, 0);
		
		/* the first should still exist */
		fncv = fnc.get(1, "womble");
		assertNotNull(fncv);
		assertEquals(200, fncv.getChildPathId());
		assertEquals(1, fncv.getChildType());
		
		/* the second should too */
		fncv = fnc.get(200, "bungle");
		assertNotNull(fncv);
		assertEquals(234, fncv.getChildPathId());
		assertEquals(0, fncv.getChildType());
		
		/* overwrite the first with a new value */
		fnc.put(1, "womble", 34, 2);
		fncv = fnc.get(1, "womble");
		assertNotNull(fncv);
		assertEquals(34, fncv.getChildPathId());
		assertEquals(2, fncv.getChildType());
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the clear() method to ensure it empty the cache.
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testClear() throws Exception {
		
		/* add some values */
		fnc.put(1, "womble", 200, 1);
		fnc.put(2, "bungle", 200, 1);
		assertNotNull(fnc.get(1, "womble"));
		assertNotNull(fnc.get(2, "bungle"));
		
		/* clear the cache */
		fnc.clear();
		
		/* make sure they're no longer there */
		assertNull(fnc.get(1, "womble"));
		assertNull(fnc.get(2, "bungle"));		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the maximum size of the cache. Adding the 5th cache entry will cause the
	 * least recently accessed item to disappear.
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testOverflow() throws Exception {
		
		/* add four names, which will max-out the cache */
		fnc.put(1, "womble", 200, 1);
		fnc.put(2, "wamble", 202, 2);
		fnc.put(3, "wimble", 204, 1);
		fnc.put(4, "wemble", 206, 3);
		
		/* check that they're all there */
		assertNotNull(fnc.get(3, "wimble"));
		assertNotNull(fnc.get(1, "womble"));
		assertNotNull(fnc.get(2, "wamble"));
		assertNotNull(fnc.get(4, "wemble"));

		/* now add a fifth name, the LRU should disappear, but the others remain */
		fnc.put(5, "wumble", 208, 2);
		assertNotNull(fnc.get(1, "womble"));
		assertNull(fnc.get(3, "wimble"));
		assertNotNull(fnc.get(2, "wamble"));
		assertNotNull(fnc.get(4, "wemble"));
		assertNotNull(fnc.get(5, "wumble"));
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the removal of elements from the cache.
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testRemoval() throws Exception {

		/* add four names */
		fnc.put(1, "womble", 200, 1);
		fnc.put(2, "wamble", 202, 2);
		fnc.put(3, "wimble", 204, 1);
		fnc.put(4, "wemble", 206, 3);
		
		/* check that they're all there */
		assertNotNull(fnc.get(3, "wimble"));
		assertNotNull(fnc.get(1, "womble"));
		assertNotNull(fnc.get(2, "wamble"));
		assertNotNull(fnc.get(4, "wemble"));

		/* remove one of them, and check that it's no longer there */
		fnc.remove(3, "wimble");
		assertNull(fnc.get(3, "wimble"));

		/* remove a second entry */
		fnc.remove(1, "womble");
		assertNull(fnc.get(1, "womble"));

		/* check that the remaining two are still there */
		assertNotNull(fnc.get(2, "wamble"));
		assertNotNull(fnc.get(4, "wemble"));
	}
	
	/*-------------------------------------------------------------------------------------*/

}
