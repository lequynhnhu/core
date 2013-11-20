package org.araqne.logger;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

public class AraqneFileAppender extends DailyRollingFileAppender {
	private long lastCheckTime;
	private long lastLogTime;

	public AraqneFileAppender(Layout layout, String filename, String datePattern) throws IOException {
		super(layout, filename, datePattern);
		lastCheckTime = System.currentTimeMillis();
		File f = new File(filename);
		if (f.exists())
			lastLogTime = f.lastModified();
		else
			lastLogTime = lastCheckTime;
	}

	@Override
	public void append(LoggingEvent event) {
		checkFilehandler();
		super.append(event);
	}

	private void checkFilehandler() {
		long now = System.currentTimeMillis();
		if (now - lastCheckTime > 60000) {
			File f = new File(fileName);
			if (!f.exists() || lastLogTime - f.lastModified() > 60000)
				resetFile();
			lastCheckTime = System.currentTimeMillis();
		}
		lastLogTime = now;
	}

	private void resetFile() {
		try {
			setFile(fileName, fileAppend, false, bufferSize);
		} catch (IOException e) {
		}
	}
}
