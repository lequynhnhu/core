/*
 * Copyright 2013 Eediom, Inc.
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
package org.araqne.api;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @since 2.2.0
 * @author xeraph
 * 
 */
public interface BatchScriptManager {
	void register(String alias, File scriptFile);

	void unregister(String alias);

	File getPath(String alias);

	List<BatchMapping> getBatchMappings();

	void executeString(ScriptContext context, String script, boolean stopOnFail) throws IOException;

	void execute(ScriptContext context, String alias) throws IOException;

	void execute(ScriptContext context, String alias, boolean stopOnFail) throws IOException;

	void executeFile(ScriptContext context, File file, String[] scriptArgs) throws IOException;

	void executeFile(ScriptContext context, File file, boolean stopOnFail, String[] scriptArgs) throws IOException;

}
