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

import java.io.File;

import org.araqne.api.BatchMapping;
import org.araqne.api.FieldOption;
import org.araqne.confdb.CollectionName;

@CollectionName("batch")
public class DefaultBatchMapping implements BatchMapping {
	private String alias;
	private String filepath;

	@FieldOption(skip = true)
	private File scriptFile;

	@SuppressWarnings("unused")
	private DefaultBatchMapping() {
		// for primitive parse
	}

	public DefaultBatchMapping(String alias, File scriptFile) {
		this.alias = alias;
		this.filepath = scriptFile.getAbsolutePath();
		this.scriptFile = scriptFile;
	}

	public String getAlias() {
		return alias;
	}

	public String getFilepath() {
		return filepath;
	}

	public File getScriptFile() {
		return scriptFile;
	}

	public void setScriptFileFromFilepath() {
		this.scriptFile = new File(filepath);
	}

	@Override
	public String toString() {
		return "[" + alias + "] " + scriptFile;
	}
}
