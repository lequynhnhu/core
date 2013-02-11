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
package org.araqne.bundle;

import org.araqne.api.BundleManager;
import org.araqne.api.Script;
import org.araqne.api.ScriptFactory;
import org.araqne.main.Araqne;
import org.osgi.framework.BundleContext;

public class BundleScriptFactory implements ScriptFactory {
	private BundleContext bc;
	private BundleManager manager;
	
	public BundleScriptFactory() {
		bc = Araqne.getContext();
		manager = new BundleManagerService(bc);
		bc.registerService(BundleManager.class.getName(), manager, null);
	}
	
	@Override
	public Script createScript() {
		return new BundleScript(bc, manager);
	}
}
