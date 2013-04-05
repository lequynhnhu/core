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
package org.araqne.console;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.araqne.api.FunctionKeyEvent;
import org.araqne.api.FunctionKeyEventListener;
import org.araqne.api.ScriptContext;
import org.araqne.api.ScriptInputStream;

public class ConsoleInputStream implements ScriptInputStream {
	private Set<FunctionKeyEventListener> callbacks;
	private ReadLineHandler readline;

	public ConsoleInputStream(ScriptContext context) {
		this.callbacks = new HashSet<FunctionKeyEventListener>();
		this.readline = new ReadLineHandler(context);
	}

	@Override
	public void supplyInput(char c) {
		readline.offer(c);
	}

	@Override
	public void supplyFunctionKey(FunctionKeyEvent keyEvent) {
		for (FunctionKeyEventListener callback : callbacks)
			callback.keyPressed(keyEvent);
	}

	@Override
	public char read() throws InterruptedException {
		return readline.read();
	}

	@Override
	public String readLine() throws InterruptedException {
		try {
			addFunctionKeyEventListener(readline);
			return readline.getLine();
		} finally {
			removeFunctionKeyEventListener(readline);
		}
	}

	@Override
	public void flush() {
		readline.flush();
	}

	@Override
	public void flush(Collection<Character> drain) {
		readline.flush(drain);
	}

	@Override
	public void addFunctionKeyEventListener(FunctionKeyEventListener callback) {
		if (callback == null)
			throw new IllegalArgumentException("callback must be not null");

		callbacks.add(callback);
	}

	@Override
	public void removeFunctionKeyEventListener(FunctionKeyEventListener callback) {
		if (callback == null)
			throw new IllegalArgumentException("callback must be not null");

		callbacks.remove(callback);
	}
}
