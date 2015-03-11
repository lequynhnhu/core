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
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.araqne.api.AccountManager;
import org.araqne.main.Araqne;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SshPasswordAuthenticator implements PasswordAuthenticator {
	private final Logger slog = LoggerFactory.getLogger(SshPasswordAuthenticator.class);

	@Override
	public boolean authenticate(String username, String password, ServerSession session) {
		BundleContext bc = Araqne.getContext();
		ServiceReference<?> ref = bc.getServiceReference(AccountManager.class.getName());
		AccountManager manager = (AccountManager) bc.getService(ref);

		IoSession ioSession = null;
		try {
			ioSession = session.getIoSession();
			SshCommandFactory.session.set(ioSession);
		} catch (Throwable t) {
			slog.warn("araqne core: cannot obtain ssh session for user " + username, t);
		}

		String remoteAddr = null;
		if (ioSession != null) {
			remoteAddr = ioSession.getRemoteAddress().toString();
		}

		boolean matched = manager.verifyPassword(username, password);
		if (matched)
			slog.info("araqne core: ssh user [{}] login from [{}]", username, remoteAddr);
		else
			slog.error("araqne core: ssh login failed for user [{}] from [{}]", username, remoteAddr);

		return matched;
	}
}
