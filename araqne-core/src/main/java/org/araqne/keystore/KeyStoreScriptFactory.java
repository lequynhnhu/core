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
package org.araqne.keystore;

import java.util.Properties;

import org.araqne.api.KeyStoreManager;
import org.araqne.api.Script;
import org.araqne.api.ScriptFactory;
import org.araqne.confdb.ConfigService;
import org.araqne.main.Araqne;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;

public class KeyStoreScriptFactory implements ScriptFactory {
	private KeyStoreManager manager;

	public KeyStoreScriptFactory() {
		this.manager = new KeyStoreManagerImpl(getConfigService(), getPreferences());
		BundleContext bc = Araqne.getContext();
		bc.registerService(KeyStoreManager.class.getName(), manager, new Properties());
	}

	@Override
	public Script createScript() {
		return new KeyStoreScript(manager);
	}

	private ConfigService getConfigService() {
		BundleContext bc = Araqne.getContext();
		ServiceReference ref = bc.getServiceReference(ConfigService.class.getName());
		return (ConfigService) bc.getService(ref);
	}

	private Preferences getPreferences() {
		BundleContext bc = Araqne.getContext();
		ServiceReference ref = bc.getServiceReference(PreferencesService.class.getName());
		PreferencesService prefsService = (PreferencesService) bc.getService(ref);
		return prefsService.getSystemPreferences();
	}

}
