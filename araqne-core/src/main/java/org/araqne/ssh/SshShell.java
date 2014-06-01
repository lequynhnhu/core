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

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketAddress;

import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.sshd.common.PtyMode;
import org.apache.sshd.common.SshException;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.Signal;
import org.apache.sshd.server.SignalListener;
import org.araqne.ansicode.AnsiEscapeCode;
import org.araqne.api.ScriptOutputStream;
import org.araqne.api.TelnetCommand;
import org.araqne.console.ConsoleInputStream;
import org.araqne.console.QuitHandler;
import org.araqne.console.ShellSession;
import org.araqne.console.TelnetStateMachine;
import org.araqne.main.Araqne;
import org.araqne.script.ScriptContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SshShell implements Command, Runnable, QuitHandler {
	private final Logger logger = LoggerFactory.getLogger(SshShell.class.getName());
	private ShellSession session;
	private InputStream in;
	private ExitCallback callback;
	private Thread thread;
	private TelnetStateMachine tsm;
	private ScriptContextImpl context;
	private String username;
	private boolean closed = false;

	private IoSession ioSession;

	public SshShell(IoSession ioSession) {
		this.ioSession = ioSession;
		this.context = new ScriptContextImpl(Araqne.getContext(), this);
		this.session = new ShellSession(context);
		this.tsm = new TelnetStateMachine(new MessageReceiver(session), context);
	}

	@Override
	public void onQuit() {
		thread.interrupt();
	}

	@Override
	public void setInputStream(InputStream in) {
		this.in = in;
		session.getScriptContext().setInputStream(new ConsoleInputStream(session.getScriptContext()));
	}

	@Override
	public void setOutputStream(OutputStream out) {
		session.getScriptContext().setOutputStream(new SshOutputStream(out));
	}

	@Override
	public void setErrorStream(OutputStream err) {
	}

	@Override
	public void setExitCallback(ExitCallback callback) {
		this.callback = callback;
	}

	@Override
	public void start(Environment env) throws IOException {
		int width = Integer.parseInt(env.getEnv().get(Environment.ENV_COLUMNS));
		int height = Integer.parseInt(env.getEnv().get(Environment.ENV_LINES));

		context.setWindowSize(width, height);

		env.addSignalListener(new SignalListener() {
			@Override
			public void signal(Signal signal) {
				logger.info(signal.toString());
			}
		});

		env.getPtyModes().put(PtyMode.ONOCR, 1);

		username = env.getEnv().get(Environment.ENV_USER);
		session.setPrincipal(username);
		context.getController().setAutoCompletion(true);

		thread = new Thread(this, "SshShell");
		thread.start();
	}

	@Override
	public void destroy() {
		logger.debug("araqne core: destroy called");
		closed = true;
		thread.interrupt();
	}

	@Override
	public void run() {
		session.printBanner();
		context.printPrompt();
		try {
			for (;;) {
				if (closed)
					break;
				byte b = (byte) in.read();
				tsm.feed(b);
			}
		} catch (Exception e) {
			if (!(e instanceof InterruptedIOException))
				e.printStackTrace();
		} finally {
			callback.onExit(0);
			SocketAddress remoteAddr = null;
			if (ioSession != null)
				remoteAddr = ioSession.getRemoteAddress();

			logger.info("araqne core: closed ssh shell for user [{}] from [{}]", username, remoteAddr);
		}
	}

	static class MessageReceiver implements ProtocolDecoderOutput {
		private ShellSession session;

		public MessageReceiver(ShellSession session) {
			this.session = session;
		}

		@Override
		public void flush(NextFilter nextFilter, IoSession session) {
		}

		@Override
		public void write(Object message) {
			try {
				session.handleMessage(message);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	class SshOutputStream implements ScriptOutputStream {
		private OutputStream out;

		public SshOutputStream(OutputStream out) {
			this.out = out;
		}

		@Override
		public ScriptOutputStream print(AnsiEscapeCode code) {
			try {
				out.write(code.toByteArray());
				out.flush();
			} catch (Exception e) {
				if (e.getMessage().toLowerCase().contains("already closed"))
					throw new IllegalStateException("Already closed", e);

				logger.error("araqne core: print error", e);
			}
			return this;
		}

		@Override
		public ScriptOutputStream printf(String format, Object... args) {
			try {
				String text = String.format(format, args);
				text = text.replaceAll("\n", "\r\n");
				byte[] b = text.getBytes("utf-8");

				out.write(b);
				out.flush();
			} catch (UnsupportedEncodingException e) {
				logger.error("araqne core: printf error", e);
			} catch (IOException e) {
				logger.error("araqne core: printf error", e);
			}

			return this;
		}

		@Override
		public ScriptOutputStream print(String value) {
			try {
				value = value.replaceAll("\n", "\r\n");
				byte[] b = value.getBytes("utf-8");
				out.write(b);
				out.flush();
			} catch (IOException e) {
				if (e instanceof SshException && e.getMessage().equals("Already closed")) {
					throw new IllegalStateException("SSH: Already Closed");
				}

				logger.error("araqne core: print error", e);
			}
			return this;
		}

		@Override
		public ScriptOutputStream println(String value) {
			print(value);
			print("\r\n");
			return this;
		}

		@Override
		public ScriptOutputStream print(TelnetCommand command) {
			try {
				out.write(TelnetCommand.InterpretAsControl);
				out.write(command.toByteArray());
			} catch (IOException e) {
				logger.error("araqne core: print error", e);
			}
			return this;
		}

	}
}
