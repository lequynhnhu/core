package org.araqne.api;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.araqne.concurrent.ThreadCachingScheduler;

public class ThreadCachingSchedulerTest {
	private static class RandomlyCrashingTask implements Runnable {
		private Random r;
		private AtomicInteger sharedCounter;
		public RandomlyCrashingTask(AtomicInteger sharedCounter) {
			this.sharedCounter = sharedCounter;
			this.r = new Random(System.nanoTime());
		}
		public void run() {
			sharedCounter.incrementAndGet();
			if (r.nextInt(10) < 1)
				throw new IllegalStateException();
		}
	}

	private static class RandomLengthTask implements Runnable {
		private double mean;
		private double stddev;
		private Random r;
		private int seed;
		private AtomicInteger sharedRunCount;

		int runCount = 0;

		public RandomLengthTask(int seed, double mean, double stddev, AtomicInteger runCount) {
			this.sharedRunCount = runCount;
			this.seed = seed;
			r = new Random(seed);
		}

		public void run() {
			try {
				Thread.sleep((long) (r.nextGaussian() * stddev + mean));
				sharedRunCount.incrementAndGet();
				runCount += 1;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		public int getRunCount() {
			return runCount;
		}
		
		public int getSeed() {
			return seed;
		}
	}

	public static void main(String[] args) throws InterruptedException, IOException {
		final ThreadCachingScheduler runner = new ThreadCachingScheduler("tcached-scheduler");

		List<ScheduledFuture<?>> futures = new ArrayList<ScheduledFuture<?>>();

		final AtomicInteger counter = new AtomicInteger(0);

		final int interval = 1000;
		
		List<Runnable> tasks = new ArrayList<Runnable>();

		Random r = new Random();
		for (int i = 0; i < 1000; ++i) {
			Runnable task = new RandomlyCrashingTask(counter);
			tasks.add(task);
			ScheduledFuture<?> f =
					runner.scheduleAtFixedRate(task, 0, interval * 1000, TimeUnit.MICROSECONDS);
			futures.add(f);
		}

		System.out.println(futures.size());

		final AtomicBoolean running = new AtomicBoolean(true);
		new Thread(new Runnable() {
			public void run() {
				while (running.get()) {
					System.out.printf("%d, %d\n", runner.getPoolSize(), counter.getAndSet(0));
					synchronized(runner) {
						try {
							runner.wait(1000);
						} catch (InterruptedException e) {
						}
					}
				}
			}
		}).start();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			String line = reader.readLine();
			if (line.equals("quit"))
				break;
		}
		
		running.set(false);
		synchronized(runner) {
			runner.notifyAll();
		}
		runner.shutdown();
		runner.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

		int notdone = 0;
		int done = 0;
		int exc = 0;
		int i = 0;
		for (Future<?> f: futures) {
			try {
				Object object = f.get(0, MILLISECONDS);
				done++;
			} catch (ExecutionException e) {
				System.out.printf("%d: %s\n", i, e.getClass().getName());
				exc++;
			} catch (TimeoutException e) {
				System.out.printf("%d: not done\n", i);
				notdone++;
			}
			i += 1;
		}
		System.out.println("notdone:   " + notdone);
		System.out.println("exception: " + exc);
		System.out.println("done:      " + done);
	}
}
