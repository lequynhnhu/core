/**
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
package org.araqne.cron.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.araqne.cron.TickService;
import org.araqne.cron.TickTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "tick-service")
@Provides(specifications = { TickService.class })
public class TickServiceImpl implements TickService, Runnable {
	private final Logger slog = LoggerFactory.getLogger(TickServiceImpl.class);

	private CopyOnWriteArraySet<TickTimer> listeners = new CopyOnWriteArraySet<TickTimer>();
	private PriorityQueue<DelayedEvent> queue = new PriorityQueue<DelayedEvent>();
	private HashMap<TickTimer, DelayedEvent> eventMap = new HashMap<TickTimer, DelayedEvent>();

	private volatile boolean doStop;

	private Thread ticker;
	private ThreadPoolExecutor executor;

	private long lastTime;

	@Validate
	public void start() {
		executor = new ThreadPoolExecutor(0, 200, 10L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
				new NamedThreadFactory());
		executor.setRejectedExecutionHandler(new CallerRunsPolicy());
		ticker = new Thread(this, "Ticker");
		ticker.start();
	}

	@Invalidate
	public void stop() {
		doStop = true;
	}

	@Override
	public void run() {
		doStop = false;

		try {
			slog.info("araqne cron: ticker started");

			lastTime = System.currentTimeMillis();
			while (!doStop) {
				try {
					long now = System.currentTimeMillis();

					if (Math.abs(now - lastTime) >= 5000)
						handleClockReset(lastTime, now);

					lastTime = now;

					List<TickTimer> targets = new ArrayList<TickTimer>();
					synchronized (queue) {
						while (true) {
							DelayedEvent ev = null;
							ev = queue.peek();
							if (ev == null)
								break;

							if (ev.scheduleTime <= now) {
								eventMap.remove(ev.listener);
								queue.poll();
								executor.execute(ev);

								targets.add(ev.listener);
							} else {
								break;
							}
						}
					}

					for (TickTimer target : targets)
						queueNextTick(now, target);

					Thread.sleep(10);
				} catch (InterruptedException e) {
					if (slog.isDebugEnabled())
						slog.debug("araqne cron: tick interrupted");
				} catch (Throwable t) {
					slog.error("araqne cron: tick error", t);
				}
			}
		} catch (Throwable t) {
			slog.error("araqne cron: ticker error", t);
		} finally {
			slog.info("araqne cron: ticker stopped");
		}
	}

	private void queueNextTick(long baseTime, TickTimer listener) {
		synchronized (queue) {
			DelayedEvent ev = new DelayedEvent(baseTime + listener.getInterval(), listener);
			eventMap.put(listener, ev);
			queue.add(ev);
		}
	}

	private void handleClockReset(long oldTime, long newTime) {
		synchronized (queue) {
			queue.clear();
			eventMap.clear();
		}

		long now = System.currentTimeMillis();
		for (TickTimer listener : listeners) {
			listener.onResetClock(oldTime, newTime);
			queueNextTick(now, listener);
		}
	}

	@Override
	public List<TickTimer> getListeners() {
		return new ArrayList<TickTimer>(listeners);
	}

	@Override
	public void addTimer(TickTimer listener) {
		boolean added = listeners.add(listener);
		if (!added)
			throw new IllegalStateException("duplicated tick listener: " + listener);

		queueNextTick(System.currentTimeMillis(), listener);
	}

	@Override
	public void removeTimer(TickTimer listener) {
		boolean removed = listeners.remove(listener);
		if (!removed)
			throw new IllegalStateException("tick listener not found: " + listener);

		synchronized (queue) {
			DelayedEvent ev = eventMap.remove(listener);
			if (ev != null) {
				queue.remove(ev);
			}
		}
	}

	private class DelayedEvent implements Comparable<DelayedEvent>, Runnable {
		private long scheduleTime;
		private TickTimer listener;

		public DelayedEvent(long scheduleTime, TickTimer listener) {
			this.scheduleTime = scheduleTime;
			this.listener = listener;
		}

		@Override
		public int compareTo(DelayedEvent o) {
			return (int) (scheduleTime - o.scheduleTime);
		}

		@Override
		public void run() {
			listener.onTick();
		}

		@Override
		public String toString() {
			return "tick " + scheduleTime + ", listener " + listener;
		}
	}

	private class NamedThreadFactory implements ThreadFactory {
		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r, "Tick Thread Pool");
		}
	}
}
