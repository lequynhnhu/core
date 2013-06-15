/*
 * Copyright 2010 NCHOVY
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.araqne.logger;

import org.araqne.api.LoggerControlService;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.AraqneLoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

public class AraqneLogService implements LogService, LoggerControlService {
	private AraqneLoggerFactory loggerFactory = (AraqneLoggerFactory) StaticLoggerBinder.getSingleton().getLoggerFactory();

	@Override
	public boolean hasLogger(String name) {
		return loggerFactory.hasLogger(name);
	}

	@Override
	public void setLogLevel(String name, String level, boolean isEnabled) {
		loggerFactory.setLogLevel(name, level, isEnabled);
	}

	@Override
	public void log(int level, String message, Throwable exception) {
		if (message.contains("A methodID cannot be associated with a method from the POJO class"))
			level = 4;

		Logger logger = (Logger) LoggerFactory.getLogger(AraqneLogService.class.getName());
		switch (level) {
		case 1:
			logger.error(message, exception);
			break;
		case 2:
			logger.warn(message, exception);
			break;
		case 3:
			logger.info(message, exception);
			break;
		case 4:
			logger.debug(message, exception);
			break;
		}
	}

	@Override
	public void log(int level, String message) {
		if (message.contains("A methodID cannot be associated with a method from the POJO class"))
			level = 4;

		Logger logger = (Logger) LoggerFactory.getLogger(AraqneLogService.class.getName());
		switch (level) {
		case 1:
			logger.error(message);
			break;
		case 2:
			logger.warn(message);
			break;
		case 3:
			logger.info(message);
			break;
		case 4:
			logger.debug(message);
			break;
		}
	}

	@Override
	public void log(@SuppressWarnings("rawtypes") ServiceReference sr, int level, String message, Throwable exception) {
		if (message.contains("A methodID cannot be associated with a method from the POJO class"))
			level = 4;

		Logger logger = (Logger) LoggerFactory.getLogger(AraqneLogService.class.getName());
		switch (level) {
		case 1:
			logger.error(message, exception);
			break;
		case 2:
			logger.warn(message, exception);
			break;
		case 3:
			logger.info(message, exception);
			break;
		case 4:
			logger.debug(message, exception);
			break;
		}
	}

	@Override
	public void log(@SuppressWarnings("rawtypes") ServiceReference sr, int level, String message) {
		if (message.contains("A methodID cannot be associated with a method from the POJO class"))
			level = 4;

		Logger logger = (Logger) LoggerFactory.getLogger(AraqneLogService.class.getName());
		switch (level) {
		case 1:
			logger.error(message);
			break;
		case 2:
			logger.warn(message);
			break;
		case 3:
			logger.info(message);
			break;
		case 4:
			logger.debug(message);
			break;
		}
	}

}
