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
package org.araqne.bundle;

import java.util.ArrayList;
import java.util.List;

import org.araqne.api.PathAutoCompleter;
import org.araqne.api.ScriptAutoCompletion;
import org.araqne.api.ScriptSession;

/**
 * @since 2.7.6
 * @author xeraph
 * 
 */
public class BundlePathCompleter extends PathAutoCompleter {
	@Override
	public List<ScriptAutoCompletion> matches(ScriptSession session, String prefix) {
		if (prefix == null || !prefix.startsWith("file:///"))
			return new ArrayList<ScriptAutoCompletion>();

		List<ScriptAutoCompletion> l = super.matches(session, prefix.substring("file://".length()));
		for (ScriptAutoCompletion c : l)
			c.setCompletion("file://" + c.getCompletion());
		return l;
	}
}
