package org.araqne.cron.impl;

import org.araqne.api.Script;
import org.araqne.api.ScriptContext;
import org.araqne.cron.TickService;
import org.araqne.cron.TickTimer;

/**
 * @since 1.8.6
 */
public class TickScript implements Script {

	private TickService tickService;
	private ScriptContext context;

	public TickScript(TickService tickService) {
		this.tickService = tickService;
	}

	@Override
	public void setScriptContext(ScriptContext context) {
		this.context = context;
	}

	public void timers(String[] args) {
		context.println("Timers");
		context.println("--------");

		for (TickTimer timer : tickService.getTimers()) {
			context.println(timer + ", interval: " + timer.getInterval() + "ms");
		}
	}
}
