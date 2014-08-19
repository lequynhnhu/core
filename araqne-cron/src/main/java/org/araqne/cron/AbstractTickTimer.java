package org.araqne.cron;

public abstract class AbstractTickTimer implements TickTimer {
	@Override
	public void onResetClock(long oldTime, long newTime) {
	}
}
