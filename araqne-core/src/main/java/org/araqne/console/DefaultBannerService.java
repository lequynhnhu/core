/*
 * Copyright 2014 Eediom Inc.
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.araqne.api.BannerService;

public class DefaultBannerService implements BannerService {
	private String banner = "Araqne Core";

	public DefaultBannerService() throws IOException {
		JarFile jar = null;
		try {
			String jarFileName = System.getProperty("java.class.path").split(System.getProperty("path.separator"))[0];
			jar = new JarFile(jarFileName);
			Manifest mf = jar.getManifest();
			Attributes attrs = mf.getMainAttributes();
			banner = "Araqne Core " + attrs.getValue("Araqne-Version");
		} catch (FileNotFoundException e) {
			banner = "Araqne Core (Debug mode)";
		} finally {
			if (jar != null)
				jar.close();
		}
	}

	@Override
	public String getBanner() {
		return banner;
	}

	@Override
	public void setBanner(String banner) {
		this.banner = banner;
	}
}
