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

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Scanner;

import org.araqne.api.FunctionKeyEvent;
import org.araqne.api.FunctionKeyEventListener;
import org.araqne.api.Script;
import org.araqne.api.ScriptArgument;
import org.araqne.api.ScriptContext;
import org.araqne.api.ScriptFactory;
import org.araqne.api.ScriptInputStream;
import org.araqne.api.ScriptUsage;
import org.araqne.main.Araqne;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptRunner implements Runnable {
	public class StringScriptInputStream implements ScriptInputStream {

		private final String backString;
		private StringReader reader;
		private Scanner scanner;

		public StringScriptInputStream(String inputStreamString) {
			backString = inputStreamString;
			reader = new StringReader(backString);
			scanner = new Scanner(reader);
		}

		@Override
		public void supplyInput(char character) {
		}

		@Override
		public void supplyFunctionKey(FunctionKeyEvent keyEvent) {
			// maybe needed C-c handling?
		}

		@Override
		public char read() throws InterruptedException {
			try {
				return (char) reader.read();
			} catch (IOException e) {
				return (char) -1;
			}
		}

		@Override
		public String readLine() throws InterruptedException {
			return scanner.nextLine();
		}

		@Override
		public String readLine(String initialValue) throws InterruptedException {
			return scanner.nextLine();
		}

		@Override
		public void flush() {
			try {
				while (reader.read() != -1)
					;
			} catch (IOException e) {
			}
		}

		@Override
		public void flush(Collection<Character> drain) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void addFunctionKeyEventListener(FunctionKeyEventListener callback) {
		}

		@Override
		public void removeFunctionKeyEventListener(FunctionKeyEventListener callback) {
		}

	}

	private Logger slog = LoggerFactory.getLogger(ScriptRunner.class.getName());
	private ScriptContext context;
	private String methodName;
	private String[] args;
	private boolean isPromptEnabled = true;
	private String inputStreamString;
	private String line;

	public ScriptRunner(ScriptContext context, String line) {
		this.line = line;
		String[] tokens = ScriptArgumentParser.tokenize(line.trim());
		String[] commandTokens = tokens[0].split("\\.");
		String alias = null;
		if (commandTokens.length != 2) {
			alias = "core";
			this.methodName = commandTokens[0];
		} else {
			alias = commandTokens[0];
			this.methodName = commandTokens[1];
		}

		this.context = context;
		this.args = getArguments(tokens);

		prepareScript(context, alias);

		// reset script command history
		context.setInputStream(new ConsoleInputStream(context));
	}

	private void prepareScript(ScriptContext context, String alias) {
		ServiceReference<?>[] refs = null;
		try {
			refs = Araqne.getContext().getServiceReferences(ScriptFactory.class.getName(), "(alias=" + alias + ")");
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
		}

		if (refs == null || refs.length == 0)
			throw new NullPointerException("script not found.");

		Script foundScript = null;
		for (ServiceReference<?> ref : refs) {
			ScriptFactory scriptFactory = (ScriptFactory) Araqne.getContext().getService(ref);
			if (scriptFactory == null)
				continue;

			Script script = scriptFactory.createScript();
			try {
				script.getClass().getDeclaredMethod(methodName, new Class[] { String[].class });
				foundScript = script;
			} catch (Exception e) {
			}

			Araqne.getContext().ungetService(ref);
		}

		if (foundScript == null) {
			throw new NullPointerException("script not found.");
		}

		context.setCurrentScript(foundScript);
	}

	public void setPrompt(boolean enabled) {
		this.isPromptEnabled = enabled;
	}

	private String[] getArguments(String[] tokens) {
		String[] arguments = new String[tokens.length - 1];
		for (int i = 1; i < tokens.length; ++i) {
			arguments[i - 1] = tokens[i];
		}
		return arguments;
	}

	public void setInputString(String str) {
		inputStreamString = str;
	}

	@Override
	public void run() {
		String user = null;
		if (context.getSession() != null)
			user = (String) context.getSession().getProperty("araqne.user");

		slog.info("araqne core: user [{}] execute command [{}]", user, line.trim());

		context.turnEchoOn();
		context.getInputStream().flush();

		ScriptInputStream oldInputStream = null;
		if (inputStreamString != null) {
			oldInputStream = context.getInputStream();
			context.setInputStream(new StringScriptInputStream(inputStreamString));
		}

		Script script = context.getCurrentScript();
		script.setScriptContext(context);
		invokeScript(script);

		if (isPromptEnabled)
			context.printPrompt();

		if (inputStreamString != null)
			context.setInputStream(oldInputStream);
		context.setCurrentScript(null);
	}

	private void invokeScript(Script script) {
		Method method;
		ScriptUsage usage = null;
		try {
			method = script.getClass().getDeclaredMethod(methodName, new Class[] { String[].class });

			usage = method.getAnnotation(ScriptUsage.class);
			verifyScriptArguments(usage, args);

			method.invoke(script, (Object) args);
		} catch (IllegalArgumentException e) {
			if (usage == null) {
				context.println("IllegalArgumentException, but no usage found. Please ask script author to add usage information.");
				return;
			}

			if (usage.description() != null) {
				context.println("Description");
				context.println("");
				context.println("\t" + usage.description());
				context.println("");
			}

			if (usage.arguments() == null || usage.arguments().length == 0)
				return;

			context.println("Arguments\n");

			int i = 1;
			for (ScriptArgument argument : usage.arguments()) {
				String optional = argument.optional() ? " (optional)" : " (required)";
				context.println("\t" + i++ + ". " + argument.name() + ": " + argument.description() + optional);
			}
		} catch (SecurityException e) {
			context.println(e.toString());
			slog.warn("script runner: ", e);
		} catch (NoSuchMethodException e) {
			context.println("syntax error.");
			slog.warn("script runner: {}.{} not found", script.getClass().getName(), methodName);
		} catch (IllegalAccessException e) {
			context.println("syntax error.");
			slog.warn("script runner: {}.{} forbidden", script.getClass().getName(), methodName);
		} catch (InvocationTargetException e) {
			context.println(e.getTargetException().toString());
			slog.warn("script runner: ", e);
		}
	}

	private void verifyScriptArguments(ScriptUsage usage, String[] args) {
		if (usage == null || usage.arguments() == null || usage.arguments().length == 0)
			return;

		if (countRequiredArguments(usage.arguments()) > args.length)
			throw new IllegalArgumentException("arguments length does not match.");

		// TODO: type match
	}

	private int countRequiredArguments(ScriptArgument[] args) {
		int count = 0;
		for (ScriptArgument arg : args) {
			if (arg.optional() == false)
				count++;
		}
		return count;
	}

	public static void main(String[] args) {
		String input = "Line1\nline2\nline3\n\nline5\n";
		StringReader reader = new StringReader(input);
		Scanner scanner = null;

		try {
			scanner = new Scanner(reader);
			System.out.println(new String(new int[] { reader.read() }, 0, 1));
			System.out.println(scanner.nextLine());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (scanner != null)
				scanner.close();
		}
	}
}
