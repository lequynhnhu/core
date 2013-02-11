/*
 * Copyright 2011 Future Systems
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
 package org.araqne.auth;

import org.araqne.api.Script;
import org.araqne.api.ScriptFactory;
import org.araqne.auth.api.AuthService;

public class AuthScriptFactory implements ScriptFactory {

	private AuthService auth;

	public AuthScriptFactory(AuthService auth) {
		this.auth = auth;
	}

	@Override
	public Script createScript() {
		return new AuthScript(auth);
	}
}
