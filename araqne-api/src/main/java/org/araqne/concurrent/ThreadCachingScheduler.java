package org.araqne.concurrent;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ThreadCachingScheduler implements ScheduledExecutorService {
	private ThreadPoolExecutor executor;

	private DelayQueue<ScheduledFutureTask<?>> queue = new DelayQueue<ScheduledFutureTask<?>>();

	private final AtomicLong sequencer = new AtomicLong(0);

	private String thrPrefix;

	private Thread schedThread;

	private AtomicInteger thrCounter;

	private class ScheduledFutureTask<T> extends FutureTask<T> implements ScheduledFuture<T> {
		long period;
		long sequenceNum; // used to tie-breaking to ensure FIFO
		long time;

		public ScheduledFutureTask(Runnable r, long initialDelay, long delay, TimeUnit unit) {
			super(r, null);
			this.time = System.nanoTime() + unit.toNanos(initialDelay);
			this.sequenceNum = sequencer.getAndIncrement();
			this.period = unit.toNanos(delay);
		}

		public ScheduledFutureTask(Runnable r, long delay, TimeUnit unit) {
			super(r, null);
			this.time = System.nanoTime() + unit.toNanos(delay);
			this.sequenceNum = sequencer.getAndIncrement();
			this.period = 0;
		}

		public ScheduledFutureTask(Callable<T> callable, long delay, TimeUnit unit) {
			super(callable);
			this.time = System.nanoTime() + unit.toNanos(delay);
			this.sequenceNum = sequencer.getAndIncrement();
			this.period = 0;
		}

		protected void onBeforeRun() {
		}

		protected long calcNextExecTime() {
			return System.nanoTime() + period;
		}

		public boolean isPeriodic() {
			return period != 0;
		}

		public int compareTo(Delayed o) {
			if (this == o)
				return 0;
			else if (o instanceof ScheduledFutureTask) {
				ScheduledFutureTask<?> sft = (ScheduledFutureTask<?>) o;
				long sftd = this.time - sft.time;
				if (sftd == 0) {
					long snd = this.sequenceNum - sft.sequenceNum;
					return snd < 0 ? -1 : 1;
				} else {
					return sftd < 0 ? -1 : 1;
				}
			}
			long d = getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS);
			return d == 0 ? 0 : ((d < 0) ? -1 : 1);
		}

		public long getDelay(TimeUnit unit) {
			return time - System.nanoTime();
		}

		@Override
		public void run() {
			boolean periodic = isPeriodic();
			onBeforeRun();
			if (!periodic)
				ScheduledFutureTask.super.run();
			else if (ScheduledFutureTask.super.runAndReset()) {
				this.time = calcNextExecTime();
				this.sequenceNum = sequencer.getAndIncrement();
				queue.add(this);
			}
		}
	}

	private class PeriodicFutureTask<T> extends ScheduledFutureTask<Void> {
		private long lastRun;

		public PeriodicFutureTask(Runnable r, long initialDelay, long delay, TimeUnit timeUnit) {
			super(r, initialDelay, delay, timeUnit);
		}

		@Override
		protected void onBeforeRun() {
			super.onBeforeRun();
			lastRun = time;
		}

		@Override
		protected long calcNextExecTime() {
			long current = System.nanoTime();
			long nextRun = lastRun + period;
			if (nextRun < current) {
				while (nextRun + period <= current) {
					nextRun += period;
				}
				return nextRun;
			} else
				return nextRun;
		}
	}

	private class ShutdownIndicator extends ScheduledFutureTask<Void> {
		public ShutdownIndicator() {
			super(new Runnable() {
				public void run() {
				}
			}, 0, MILLISECONDS);
		}
	}

	private class Scheduler implements Runnable {
		public void run() {
			DelayQueue<ScheduledFutureTask<?>> queue = ThreadCachingScheduler.this.queue;
			while (!executor.isShutdown()) {
				try {
					ScheduledFutureTask<?> polled = queue.poll(100, MILLISECONDS);
					if (polled != null) {
						if (polled instanceof ShutdownIndicator)
							break;
						executor.execute(polled);
					}
				} catch (RejectedExecutionException e) {
					// ignore
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}
	}

	public ThreadCachingScheduler(String threadNamePrefix) {
		this.thrPrefix = threadNamePrefix;
		this.thrCounter = new AtomicInteger();
		executor =
				new ThreadPoolExecutor(
						0, Integer.MAX_VALUE, 10L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(false),
						new ThreadFactory() {
							public Thread newThread(Runnable r) {
								return new Thread(r, thrPrefix + "-" + thrCounter.incrementAndGet());
							}
						});
		executor.prestartAllCoreThreads();
		schedThread = new Thread(new Scheduler(), thrPrefix + "-task-scheduler");
		schedThread.start();
	}

	public Object getPoolSize() {
		return executor.getPoolSize();
	}
	
	public void shutdown() {
		executor.shutdown();
		queue.add(new ShutdownIndicator());
	}

	public List<Runnable> shutdownNow() {
		List<Runnable> ret = executor.shutdownNow();
		queue.add(new ShutdownIndicator());
		return ret;
	}

	public boolean isShutdown() {
		return executor.isShutdown();
	}

	public boolean isTerminated() {
		return executor.isTerminated();
	}

	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return executor.awaitTermination(timeout, unit);
	}

	public <T> Future<T> submit(Callable<T> task) {
		return schedule(task, 0, MILLISECONDS);
	}

	public <T> Future<T> submit(final Runnable task, final T result) {
		return schedule(new Callable<T>() {
			public T call() throws Exception {
				task.run();
				return result;
			}
		}, 0, MILLISECONDS);
	}

	public Future<?> submit(Runnable task) {
		return schedule(task, 0, MILLISECONDS);
	}

	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		return executor.invokeAll(tasks);
	}

	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		return executor.invokeAll(tasks, timeout, unit);
	}

	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		return executor.invokeAny(tasks);
	}

	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return executor.invokeAny(tasks, timeout, unit);
	}

	public void execute(Runnable command) {
		schedule(command, 0, MILLISECONDS);
	}

	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		ScheduledFutureTask<?> task = new ScheduledFutureTask<Void>(command, delay, unit);
		queue.add(task);
		return task;
	}

	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
		ScheduledFutureTask<V> task = new ScheduledFutureTask<V>(callable, delay, unit);
		queue.add(task);
		return task;
	}

	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		ScheduledFutureTask<?> task = new PeriodicFutureTask<Void>(command, initialDelay, period, unit);
		queue.add(task);
		return task;
	}

	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		ScheduledFutureTask<?> task = new ScheduledFutureTask<Void>(command, initialDelay, delay, unit);
		queue.add(task);
		return task;
	}

	/********************************************************
	 * main methods for test and development
	 * @throws IOException 
	 ********************************************************/


}
