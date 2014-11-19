package org.araqne.cron.impl;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.araqne.api.Script;
import org.araqne.api.ScriptFactory;
import org.araqne.cron.TickService;

/**
 * @since 1.8.6
 */
@Component(name = "tick-script-factory")
@Provides
public class TickScriptFactory implements ScriptFactory {

	@ServiceProperty(name = "alias", value = "tick")
	private String alias;

	@Requires
	private TickService tickService;

	public TickScriptFactory() {
	}

	public TickScriptFactory(TickService tickService) {
		this.tickService = tickService;
	}

	@Override
	public Script createScript() {
		return new TickScript(tickService);
	}
}
