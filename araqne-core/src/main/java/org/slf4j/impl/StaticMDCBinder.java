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
package org.slf4j.impl;

import java.util.Map;

import org.slf4j.spi.MDCAdapter;

public class StaticMDCBinder {
	public static final StaticMDCBinder SINGLETON = new StaticMDCBinder();

	public static class AraqneMDCAdapter extends Log4jMDCAdapter {
		public static String diagInfo = null;

		@Override
		public void put(String key, String val) {
			super.put(key, val);
			if (key.equals("araqne-diag-info"))
				diagInfo = val;
		}

		@Override
		public void remove(String key) {
			super.remove(key);
			if (key.equals("araqne-diag-info"))
				diagInfo = null;
		}

		@Override
		public void clear() {
			super.clear();
			diagInfo = null;
		}

		@Override
		public void setContextMap(Map contextMap) {
			super.setContextMap(contextMap);
			diagInfo = (String) contextMap.get("araqne-diag-info");
		}
	}

	private StaticMDCBinder() {
	}

	public MDCAdapter getMDCA() {
		return new AraqneMDCAdapter();
	}

	public String getMDCAdapterClassStr() {
		return Log4jMDCAdapter.class.getName();
	}
}
