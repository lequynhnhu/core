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
package org.araqne.ssh;

import org.apache.mina.core.session.IoSession;
import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;

public class SshCommandFactory implements Factory<Command> {

	public static ThreadLocal<IoSession> session = new ThreadLocal<IoSession>();

	@Override
	public Command create() {
		return new SshShell(session.get());
	}
}
