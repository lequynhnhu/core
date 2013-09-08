package org.araqne.cron;

public abstract class CronTask implements Runnable {

	@Override
	public void run() {
		run(null);
	}

	public abstract void run(Schedule schedule);
}
