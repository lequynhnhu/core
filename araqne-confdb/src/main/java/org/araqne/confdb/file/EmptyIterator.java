/*
 * Copyright 2011 Future Systems, Inc.
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
package org.araqne.confdb.file;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.araqne.api.PrimitiveParseCallback;
import org.araqne.confdb.Config;
import org.araqne.confdb.ConfigIterator;
import org.araqne.confdb.ConfigParser;
import org.araqne.confdb.ObjectBuilder;

class EmptyIterator implements ConfigIterator {
	@Override
	public boolean hasNext() {
		return false;
	}

	@Override
	public Config next() {
		return null;
	}

	@Override
	public void remove() {
	}

	@Override
	public void setParser(ConfigParser parser) {
	}

	@Override
	public List<Config> getConfigs(int offset, int limit) {
		return new ArrayList<Config>();
	}

	@Override
	public Collection<Object> getDocuments() {
		return new ArrayList<Object>();
	}

	@Override
	public <T> Collection<T> getDocuments(Class<T> cls) {
		return new ArrayList<T>();
	}

	@Override
	public <T> Collection<T> getDocuments(Class<T> cls, PrimitiveParseCallback callback) {
		return new ArrayList<T>();
	}

	@Override
	public <T> Collection<T> getDocuments(Class<T> cls, PrimitiveParseCallback callback, int offset, int limit) {
		return new ArrayList<T>();
	}

	@Override
	public <T> Collection<T> getObjects(ObjectBuilder<T> builder, int offset, int limit) {
		return new ArrayList<T>();
	}

	@Override
	public int count() {
		return 0;
	}

	@Override
	public void close() {
	}
}