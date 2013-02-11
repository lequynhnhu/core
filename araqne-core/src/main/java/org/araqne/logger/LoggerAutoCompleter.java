/*
 * Copyright 2012 Future Systems
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

import java.util.ArrayList;
import java.util.List;

import org.araqne.api.AutoCompleteHelper;
import org.araqne.api.ScriptAutoCompletion;
import org.araqne.api.ScriptAutoCompletionHelper;
import org.araqne.api.ScriptSession;
import org.slf4j.impl.AraqneLoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

public class LoggerAutoCompleter implements ScriptAutoCompletionHelper {

	@Override
	public List<ScriptAutoCompletion> matches(ScriptSession session, String prefix) {
		AraqneLoggerFactory loggerFactory = (AraqneLoggerFactory) StaticLoggerBinder.getSingleton().getLoggerFactory();
		List<String> loggers = loggerFactory.getLoggerList();

		List<String> found = new ArrayList<String>();
		for (String logger : loggers)
			if (logger.startsWith(prefix))
				found.add(logger);

		String commonPrefix = AutoCompleteHelper.extractCommonPrefix(found);
		List<ScriptAutoCompletion> completions = new ArrayList<ScriptAutoCompletion>(loggers.size());
		for (String logger : found)
			completions.add(new ScriptAutoCompletion(logger, logger.substring(commonPrefix.length())));

		return completions;
	}
}
