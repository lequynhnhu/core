/*
 * Copyright 2009 NCHOVY
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
package org.araqne.console;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.araqne.api.Script;
import org.araqne.api.ScriptArgument;
import org.araqne.api.ScriptAutoCompletion;
import org.araqne.api.ScriptAutoCompletionHelper;
import org.araqne.api.ScriptFactory;
import org.araqne.api.ScriptSession;
import org.araqne.api.ScriptUsage;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleAutoComplete {
	private final Logger logger = LoggerFactory.getLogger(ConsoleAutoComplete.class);
	private BundleContext bundleContext;

	public ConsoleAutoComplete(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	public List<ScriptAutoCompletion> search(ScriptSession session, String prefix, String[] hint) {
		String[] tokens = ScriptArgumentParser.tokenize(prefix);

		List<ScriptAutoCompletion> terms = new ArrayList<ScriptAutoCompletion>();
		if (tokens.length <= 1 && !prefix.endsWith(" ")) {
			if (isScriptFactoryDetermined(prefix)) {
				addScriptMethods(prefix, terms);
			} else {
				addScriptAliases(prefix, terms);
				addScriptMethods("core." + prefix, terms);
			}
		} else {
			// argument completion using autocompletion helper
			try {
				int p = tokens[0].indexOf('.');
				String alias = p > 0 ? tokens[0].substring(0, p) : "core";
				String method = p > 0 ? tokens[0].substring(p + 1) : tokens[0];
				Method m = findScriptMethod(alias, method);
				if (m == null)
					return terms;

				ScriptUsage usage = m.getAnnotation(ScriptUsage.class);
				if (usage == null)
					return terms;

				int argIndex = prefix.endsWith(" ") ? tokens.length - 1 : tokens.length - 2;

				ScriptArgument arg = null;
				if (argIndex < usage.arguments().length)
					arg = usage.arguments()[argIndex];

				if (arg == null || arg.autocompletion() == ScriptAutoCompletionHelper.class)
					return terms;

				String nextToken = prefix.endsWith(" ") ? "" : tokens[tokens.length - 1];
				ScriptAutoCompletionHelper helper = (ScriptAutoCompletionHelper) arg.autocompletion().newInstance();
				return helper.matches(session, nextToken);
			} catch (Throwable e) {
				logger.error("araqne core: cannot auto-complete", e);
			}
		}

		Collections.sort(terms, new Comparator<ScriptAutoCompletion>() {

			@Override
			public int compare(ScriptAutoCompletion o1, ScriptAutoCompletion o2) {
				return o1.getSuggestion().compareTo(o2.getSuggestion());
			}
		});

		return terms;
	}

	private boolean isScriptFactoryDetermined(String prefix) {
		return prefix.indexOf('.') >= 0;
	}

	private void addScriptMethods(String prefix, List<ScriptAutoCompletion> terms) {
		try {
			String token = prefix.split(" ")[0];
			int dotPos = token.indexOf('.');
			String alias = token.substring(0, dotPos);
			String methodPrefix = token.substring(dotPos + 1);

			for (Script script : getScripts(alias)) {
				for (Method m : script.getClass().getMethods()) {
					Class<?>[] paramTypes = m.getParameterTypes();
					if (paramTypes == null || paramTypes.length == 0) {
						continue;
					}

					if (!paramTypes[0].isArray())
						continue;

					String lname = m.getName().toLowerCase();
					if (methodPrefix.length() == 0 || (methodPrefix.length() > 0 && lname.startsWith(methodPrefix.toLowerCase()))) {
						terms.add(new ScriptAutoCompletion(m.getName()));
					}
				}
			}
			
			// try more match with fuzzing
			if (terms.isEmpty()) {
				Matcher nameMatcher = Pattern.compile(makePattern(methodPrefix)).matcher("");
				for (Script script : getScripts(alias)) {
					for (Method m : script.getClass().getMethods()) {
						Class<?>[] paramTypes = m.getParameterTypes();
						if (paramTypes == null || paramTypes.length == 0) {
							continue;
						}

						if (!paramTypes[0].isArray())
							continue;

						nameMatcher.reset(m.getName().toLowerCase());
						if (methodPrefix.length() == 0 || (methodPrefix.length() > 0 && nameMatcher.matches())) {
							terms.add(new ScriptAutoCompletion(m.getName()));
						}
					}
				}
			}
		} catch (NullPointerException e) {
			// ignore
		}
	}

	private String makePattern(String methodPrefix) {
		String lc = methodPrefix.toLowerCase();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < lc.length(); ++i) {
			sb.append(".*");
			sb.append(lc.charAt(i));
		}
		sb.append(".*");
		return sb.toString();
	}

	private Method findScriptMethod(String alias, String method) {
		ServiceReference<?>[] refs;
		try {
			refs = bundleContext.getServiceReferences(ScriptFactory.class.getName(), "(alias=" + alias + ")");
			if (refs == null || refs.length == 0) {
				return null;
			}

			for (ServiceReference<?> ref : refs) {
				ScriptFactory scriptFactory = (ScriptFactory) bundleContext.getService(ref);
				Script script = scriptFactory.createScript();
				try {
					return script.getClass().getDeclaredMethod(method, new Class[] { String[].class });
				} catch (Exception e) {
				}
			}
		} catch (InvalidSyntaxException e) {
		}
		return null;
	}

	private List<Script> getScripts(String alias) {
		List<Script> scripts = new ArrayList<Script>();
		ServiceReference<?>[] refs;
		try {
			refs = bundleContext.getServiceReferences(ScriptFactory.class.getName(), "(alias=" + alias + ")");
			if (refs == null || refs.length == 0) {
				return null;
			}

			for (ServiceReference<?> ref : refs) {
				ScriptFactory scriptFactory = (ScriptFactory) bundleContext.getService(ref);
				scripts.add(scriptFactory.createScript());
			}
			return scripts;
		} catch (InvalidSyntaxException e) {
			return null;
		}
	}

	private void addScriptAliases(String prefix, List<ScriptAutoCompletion> terms) {
		HashSet<String> aliases = new HashSet<String>();
		try {
			ServiceReference<?>[] refs = bundleContext.getServiceReferences(ScriptFactory.class.getName(), null);
			if (refs == null)
				return;

			for (int i = 0; i < refs.length; i++) {
				if (refs[i].getProperty("alias") != null) {
					String alias = refs[i].getProperty("alias").toString();
					if (alias.startsWith(prefix) && !aliases.contains(alias)) {
						terms.add(new ScriptAutoCompletion(alias));
						aliases.add(alias);
					}
				}
			}

		} catch (InvalidSyntaxException e) {
			// ignore
		}
	}
}
