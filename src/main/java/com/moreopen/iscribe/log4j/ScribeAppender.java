package com.moreopen.iscribe.log4j;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moreopen.iscribe.thrift.LogEntry;
import com.moreopen.iscribe.thrift.ResultCode;
import com.moreopen.iscribe.thrift.scribe;
import com.moreopen.iscribe.thrift.scribe.Client;

/**
 * ScribeAppender synchronously sends the log messages to a specified scribe server,
 * 
 * which is specified by scribeHost, scribePort and scribeCategory.
 * 
 */
public class ScribeAppender extends AppenderSkeleton {

	private final static Logger logger = LoggerFactory.getLogger(ScribeAppender.class);

//	private final static Executor executor = Executors.newFixedThreadPool(1);
	
	private final static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

	/* connected state between application server and Scribe Server */
	private AtomicBoolean isConnected = new AtomicBoolean(false);

	/* host name of application server */
	private String hostname;

	/* host of scribe server */
	private String scribeHost;

	/* port of scribe server */
	private int scribePort;

	/* scribe category */
	private String scribeCategory;

	private TTransport tTransport;

	private Client client;

	private List<LogEntry> logEntries = new ArrayList<LogEntry>(1);
	
	//检查连接是否可用的时间间隔 (单位:秒)
	private int scribeClientCheckPeriod = 3;
	
	/*
	 * 当 scribe client 不可用时用于记录到本地文件的日志器名称 (应用方必须配置对应的日志器，否则文件可能会输出到 root logger 中)
	 * 如 scribeLocalLoggerName 为空，认为无需记录到本地，日志直接丢弃 
	 */
	private String scribeLocalLoggerName;
	
	private org.apache.log4j.Logger scribeLocalLogger = null;
	
	public String getHostname() {
		return this.hostname;
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

	/*
	 * Activates this Appender by opening a transport to the Scribe Server.
	 */
	@Override
	public void activateOptions() {
		logger.debug("activeOptions is called");
		synchronized (this) {
			configScribe();
			/* asynchronously connect to Scribe Server */
//			executor.execute(new Runnable() {
//				@Override
//				public void run() {
//					connectScribe();
//				}
//			});
			connectScribe();
		}
	}

	/* initialize and check Scribe Server configuration */
	private void configScribe() {
		if (StringUtils.isEmpty(hostname)) {
			try {
				hostname = InetAddress.getLocalHost().getHostAddress();
				logger.warn("hostname is not configured, try to get it via local machine:{}", hostname);
			} catch (UnknownHostException uhe) {
				logger.error("hostname cannot be got.", uhe);
			}
		}

		if (StringUtils.isEmpty(scribeHost)) {
			throw new IllegalArgumentException("scribeHost cannot be empty.");
		}

		if (scribePort == 0) {
			throw new IllegalArgumentException("scribePort cannot be zero.");
		}

		if (StringUtils.isEmpty(scribeCategory)) {
			throw new IllegalArgumentException("scribeCategory cannot be empty.");
		}
		
		if (StringUtils.isNotBlank(scribeLocalLoggerName)) {
			scribeLocalLogger = org.apache.log4j.Logger.getLogger(scribeLocalLoggerName);
		}
	}

	private synchronized void connectScribe() {
		
		executor.scheduleWithFixedDelay(new Runnable() {
			
			@Override
			public void run() {
				if (isConnected()) {
					return;
				}
				try { 
					logger.warn("Connect to Scribe Server... {}:{}", scribeHost, scribePort);
					tTransport = new TFramedTransport(new TSocket(new Socket(scribeHost, scribePort)));
					TBinaryProtocol protocol = new TBinaryProtocol(tTransport, false, false);
					client = new scribe.Client(protocol, protocol);
					ScribeAppender.this.isConnected.set(true);
					logger.warn("Success to connect Scribe Server {}:{}", scribeHost, scribePort);
				} catch (Exception e) {
					logger.error(String.format("Fail to connect Scribe Server ... %s:%s", scribeHost, scribePort), e);
				}
			}
		}, 0, ScribeAppender.this.scribeClientCheckPeriod, TimeUnit.SECONDS);
		
		
//		try {
//			if (isConnected()) {
//				logger.warn("Scribe Server has been connected.");
//			} else {
//				logger.warn("Connect to Scribe Server... {}:{}", scribeHost, scribePort);
//				tTransport = new TFramedTransport(new TSocket(new Socket(scribeHost, scribePort)));
//				TBinaryProtocol protocol = new TBinaryProtocol(tTransport, false, false);
//				client = new scribe.Client(protocol, protocol);
//				this.isConnected.set(true);
//				logger.warn("Success to connect Scribe Server {}:{}", scribeHost, scribePort);
//			}
//		} catch (Exception e) {
//			logger.error("Fail to connect Scribe Server.", e);
//		}
	}

	/* check whether Scribe Server is connected or not */
	private boolean isConnected() {
		return this.isConnected.get();
	}

	/*
	 * Appends a log message to Scribe
	 */
	@Override
	public void append(LoggingEvent event) {
		if (!isConnected()) {
			//XXX scribe client is not ready, log to local
			if (scribeLocalLogger != null) {
				scribeLocalLogger.callAppenders(event);
			}
			return;
		}
		synchronized (this) {
			try {
				String message = String.format("serverip=%s, %s", hostname, layout.format(event));
				logEntries.add(new LogEntry(scribeCategory, message));
				ResultCode resultCode = client.Log(logEntries);
				if (logger.isDebugEnabled()) {
					logger.debug("Scribe Server result code:{}", resultCode.getValue());
				}
				if (resultCode == ResultCode.TRY_LATER) {
					logger.warn("Scribe Server result code, TRY_LATER, value:{}", resultCode);
				}
			} catch (Exception e) {
				logger.error("Fail to send log message to Scribe Server :{}", e.getMessage());
				this.isConnected.set(false);
				/* reconnect to Scribe Server, reconnect by ScheduledExecutorService */
				//connectScribe();
			} finally {
				logEntries.clear();
			}
		}
	}

	@Override
	public void close() {
		if (this.tTransport != null && this.tTransport.isOpen()) {
			this.tTransport.close();
		}
		this.isConnected.set(false);
	}

	@Override
	public boolean requiresLayout() {
		return true;
	}
}