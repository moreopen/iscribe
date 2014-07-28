package com.moreopen.iscribe.log4j;

import org.apache.log4j.AsyncAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ScribeAppender asynchronously sends the log messages to a specified scribe server,
 * 
 * which is specified by scribeHost, scribePort and scribeCategory.
 * 
 */
public class AsyncScribeAppender extends AsyncAppender {
	
	private final static Logger logger = LoggerFactory.getLogger(AsyncScribeAppender.class);

	/* hostname of application server */
	private String hostname;

	/* host of scribe server */
	private String scribeHost;

	/* port of scribe server */
	private int scribePort;

	/* scribe category */
	private String scribeCategory;
	
	//检查连接是否可用的时间间隔 (单位:秒)
	private int scribeClientCheckPeriod = 3;
		
		/*
	 * 当 scribe client 不可用时用于记录到本地文件的日志器名称 (应用方必须配置对应的日志器，否则文件可能会输出到 root logger 中)
	 * 如 scribeLocalLoggerName 为空，认为无需记录到本地，日志直接丢弃 
	 */
	private String scribeLocalLoggerName;

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getScribeHost() {
		return scribeHost;
	}

	public void setScribeHost(String scribeHost) {
		this.scribeHost = scribeHost;
	}

	public int getScribePort() {
		return scribePort;
	}

	public void setScribePort(int scribePort) {
		this.scribePort = scribePort;
	}

	public String getScribeCategory() {
		return scribeCategory;
	}

	public void setScribeCategory(String scribeCategory) {
		this.scribeCategory = scribeCategory;
	}

	public void setScribeClientCheckPeriod(int scribeClientCheckPeriod) {
		this.scribeClientCheckPeriod = scribeClientCheckPeriod;
	}

	public void setScribeLocalLoggerName(String scribeLocalLoggerName) {
		this.scribeLocalLoggerName = scribeLocalLoggerName;
	}

	@Override
	public void activateOptions() {
		logger.debug("activeOptions is called");
		super.activateOptions();
		synchronized (this) {
			ScribeAppender scribeAppender = new ScribeAppender();
			scribeAppender.setLayout(getLayout());
			scribeAppender.setHostname(getHostname());
			scribeAppender.setScribeHost(getScribeHost());
			scribeAppender.setScribePort(getScribePort());
			scribeAppender.setScribeCategory(getScribeCategory());
			scribeAppender.setScribeClientCheckPeriod(scribeClientCheckPeriod);
			scribeAppender.setScribeLocalLoggerName(scribeLocalLoggerName);
			scribeAppender.activateOptions();
			addAppender(scribeAppender);
		}
	}

	@Override
	public boolean requiresLayout() {
		return true;
	}
}