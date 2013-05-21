package org.araqne.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ScriptOptionParser {
	public static class ScriptOption {
		public String name;
		public String longName;
		public List<String> values;

		public ScriptOption(String shortName, String longName) {
			this.name = shortName;
			this.longName = longName;
		}

		public void addArg(String arg) {
			if (values == null)
				values = new ArrayList<String>();
			values.add(arg);
		}
		
		public String getName() {
			return name != null ? name : longName; 
		}
	}

	private ArrayList<String> args;

	public ScriptOptionParser(String[] args) {
		this.args = new ArrayList<String>(Arrays.asList(args));
	}

	public ScriptOption getOption(String shortOpt, boolean hasArg) {
		return getOption(shortOpt, null, hasArg);
	}

	public ScriptOption getOption(String shortOpt, String longOpt, boolean hasArg) {
		ScriptOption option = null;
		Iterator<String> iterator = args.iterator();
		boolean needsArg = false;
		String pfshort = "-" + shortOpt;
		String pflong = "--" + longOpt;

		while (iterator.hasNext()) {
			String arg = iterator.next();
			if (arg.equals("--")) {
				break;
			}
			if (arg.startsWith("-") && (arg.startsWith(pfshort) || arg.startsWith(pflong))) {
				if (option == null)
					option = new ScriptOption(shortOpt, longOpt);
				boolean isShort = arg.startsWith(pfshort);
				iterator.remove();
				if (hasArg) {
					if (isShort && arg.length() > 2)
						option.addArg(arg.substring(2));
					else if (!isShort && arg.startsWith(pflong + "="))
						option.addArg(arg.substring(pflong.length() + 1));
					else
						needsArg = true;
				}
				continue;
			}
			if (needsArg) {
				option.addArg(arg);
				iterator.remove();
				needsArg = false;
				continue;
			}
		}
		if (option != null && needsArg)
			throw new IllegalArgumentException("no argument supplied for option \"" + option.name + "\"");
		return option;
	}

	public List<String> getArguments() {
		List<String> result = new ArrayList<String>(args.size());
		for (String arg : args) {
			if (arg.equals("--"))
				continue;
			result.add(arg);
		}
		return result;
	}
}
