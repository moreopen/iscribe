/*
 * Copyright 2011 y.sdo.com, Inc. All rights reserved.
 * y.sdo.com PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.moreopen.iscribe;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncScribeAppenderTest {

	@Test
	public void testScribeAppender() throws Exception {
		Logger logger = LoggerFactory.getLogger("asyncScribe");
		Thread.sleep(2000);
			long start = System.currentTimeMillis();
			for (int i = 0; i < 50; i++) {
				logger.info("Hello, async scribe. 你好，async scribe {}", i);
				Thread.sleep(1000);
			}
			System.out.println("Time cost:" + (System.currentTimeMillis() - start));
		while (true)
			;
	}
}
