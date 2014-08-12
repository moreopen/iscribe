/*
 * Copyright 2011 y.sdo.com, Inc. All rights reserved.
 * y.sdo.com PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.moreopen.iscribe;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScribeAppenderTest {

	@Test
	public void testScribeAppender() throws Exception {
		Logger logger = LoggerFactory.getLogger("scribe");
		long start = System.currentTimeMillis();
		for (int i = 0; i < 5; i++) {
			logger.info("Hello, scribe. 你好，scribe {}", i);
		}
		System.out.println("Time cost:" + (System.currentTimeMillis() - start));
//		while(true);
	}
}