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
package org.araqne.api;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Environment {
	public static String expandSystemProperties(String path) {
		Matcher m = Pattern.compile("\\$\\{(?!\\$\\{)(.*?)\\}").matcher(path);
		while (m.find()) {
			String replacement = System.getProperty(m.group(1));
			if (replacement == null)
				continue;
			path = path.replace(m.group(), replacement);
		}
		return path;
	}

	public static void setAraqneSystemProperties(String araqneDir) {
		if (System.getProperty("araqne.dir") == null) {
			System.setProperty("araqne.dir", new File(araqneDir).getAbsolutePath());
		}

		String araqneDirProp = System.getProperty("araqne.dir");
		if (System.getProperty("araqne.data.dir") == null)
			System.setProperty("araqne.data.dir", new File(araqneDirProp, "data").getAbsolutePath());
		if (System.getProperty("araqne.log.dir") == null)
			System.setProperty("araqne.log.dir", new File(araqneDirProp, "log").getAbsolutePath());
		if (System.getProperty("araqne.cache.dir") == null)
			System.setProperty("araqne.cache.dir", new File(araqneDirProp, "cache").getAbsolutePath());
		if (System.getProperty("araqne.download.dir") == null)
			System.setProperty("araqne.download.dir", new File(araqneDirProp, "download").getAbsolutePath());
		if (System.getProperty("araqne.cert.dir") == null)
			System.setProperty("araqne.cert.dir", new File(araqneDirProp, "cert").getAbsolutePath());
		if (System.getProperty("araqne.home.dir") == null)
			System.setProperty("araqne.home.dir", new File(araqneDirProp, "home").getAbsolutePath());
	}
}
