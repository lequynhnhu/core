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
package org.araqne.script.batch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.araqne.api.BatchMapping;
import org.araqne.api.BatchScriptManager;
import org.araqne.api.ScriptContext;
import org.araqne.confdb.Config;
import org.araqne.confdb.ConfigDatabase;
import org.araqne.confdb.ConfigService;
import org.araqne.confdb.Predicates;
import org.araqne.console.ScriptRunner;
import org.araqne.main.Araqne;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class BatchScriptManagerImpl implements BatchScriptManager {
	private ConfigService conf;

	public BatchScriptManagerImpl() {
		BundleContext bc = Araqne.getContext();
		ServiceReference<?> ref = bc.getServiceReference(ConfigService.class.getName());
		this.conf = (ConfigService) bc.getService(ref);
	}

	private ConfigDatabase getDatabase() {
		return conf.ensureDatabase("araqne-core");
	}

	@Override
	public void register(String alias, File scriptFile) {
		getDatabase().add(new DefaultBatchMapping(alias, scriptFile));
	}

	@Override
	public void unregister(String alias) {
		ConfigDatabase db = getDatabase();
		Config c = db.findOne(DefaultBatchMapping.class, Predicates.field("alias", alias));
		if (c != null)
			db.remove(c);
	}

	@Override
	public File getPath(String alias) {
		ConfigDatabase db = getDatabase();
		Config c = db.findOne(DefaultBatchMapping.class, Predicates.field("alias", alias));
		if (c == null)
			return null;
		return new File(c.getDocument(DefaultBatchMapping.class).getFilepath());
	}

	@Override
	public List<BatchMapping> getBatchMappings() {
		List<BatchMapping> mappings = new ArrayList<BatchMapping>();
		ConfigDatabase db = getDatabase();
		for (DefaultBatchMapping mapping : db.findAll(DefaultBatchMapping.class).getDocuments(DefaultBatchMapping.class)) {
			mapping.setScriptFileFromFilepath();
			mappings.add(mapping);
		}
		return mappings;
	}

	public void execute(ScriptContext context, String alias) throws IOException {
		execute(context, alias, true);
	}

	public void execute(ScriptContext context, String alias, boolean stopOnFail) throws IOException {
		File scriptFile = getPath(alias);
		if (scriptFile == null)
			throw new IOException("script not found");
		executeFile(context, scriptFile, stopOnFail, new String[0]);
	}

	public void executeFile(ScriptContext context, File file, String[] scriptArgs) throws IOException {
		executeFile(context, file, true, scriptArgs);
	}

	private static Pattern ptrnInlineRedir = Pattern.compile("<<([a-zA-Z0-9]+)\\s*$");

	public void executeFile(ScriptContext context, File file, boolean stopOnFail, String[] scriptArgs) throws IOException {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			runCommands(context, stopOnFail, scriptArgs, br);
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
			}
		}
	}

	@Override
	public void executeString(ScriptContext context, String script, boolean stopOnFail) throws IOException {
		BufferedReader br = new BufferedReader(new StringReader(script));
		runCommands(context, stopOnFail, new String[0], br);
	}

	private void runCommands(ScriptContext context, boolean stopOnFail, String[] scriptArgs, BufferedReader br)
			throws IOException {
		String cmd = null;
		while (true) {
			String line = br.readLine();
			String inlineRedirection = null;

			if (line == null)
				break;

			line = line.trim();

			if (line.startsWith("#"))
				continue;

			boolean followNextLine = line.endsWith("\\");

			if (followNextLine) {
				line = line.substring(0, line.length() - 1).trim();
			}

			if (cmd == null)
				cmd = line;
			else
				cmd += " " + line;

			if (followNextLine)
				continue;

			if (cmd.trim().isEmpty()) {
				cmd = null;
				inlineRedirection = null;

				continue;
			}

			cmd = replacePlaceholders(cmd, scriptArgs);

			Matcher matcher = ptrnInlineRedir.matcher(cmd);
			if (matcher.find()) {
				cmd = cmd.substring(0, matcher.start());
				String endOfInputMark = matcher.group(1);
				StringBuilder builder = new StringBuilder();
				while (true) {
					String l = br.readLine();
					if (l == null)
						throw new IllegalArgumentException("unexpected end of the file while reading inline redirection stream");
					if (l.equals(endOfInputMark))
						break;
					builder.append(l);
					builder.append("\n");
				}
				inlineRedirection = builder.toString();
			}

			try {
				context.printf("executing \"%s\"\n", cmd);
				ScriptRunner runner = new ScriptRunner(context, cmd);
				if (inlineRedirection != null)
					runner.setInputString(inlineRedirection);
				runner.setPrompt(false);
				runner.run();
			} catch (Exception e) {
				context.println(e.getMessage());
				if (stopOnFail)
					break;
			}
			cmd = null;
			inlineRedirection = null;
		}
	}

	private static Pattern ptrnPlaceHolder = Pattern.compile("\\$\\{([0-9]+)\\}");

	private String replacePlaceholders(String cmd, String[] scriptArgs) {
		Matcher matcher = ptrnPlaceHolder.matcher(cmd);
		StringBuilder sb = new StringBuilder();
		int oldStart = 0;
		while (matcher.find()) {
			sb.append(cmd.substring(oldStart, matcher.start()));
			sb.append(getArgs(Integer.parseInt(matcher.group(1)) - 1, scriptArgs));
			oldStart = matcher.end();
		}
		sb.append(cmd.substring(oldStart));

		return sb.toString();
	}

	private Object getArgs(int idx, String[] scriptArgs) {
		if (idx < scriptArgs.length) {
			return scriptArgs[idx];
		} else {
			return "";
		}
	}
}
