package org.araqne.api;

import static org.junit.Assert.*;

import java.util.List;

import org.araqne.api.ScriptOptionParser.ScriptOption;
import org.junit.Test;

public class ScriptOptionParserTest {
	@Test
	public void usageTest() {
		String[] args = { "-v", "--factory=f1", "-ff2", "a1", "-f", "f3", "a2", "a3" };
		
		ScriptOptionParser sop = new ScriptOptionParser(args);
		ScriptOption verbOpt = sop.getOption("v", "verbose", false);
		ScriptOption fullVerbOpt = sop.getOption("V", "full-verbose", false);
		ScriptOption factFilter = sop.getOption("f", "factory", true);

		List<String> argl = sop.getArguments();
		assertTrue(verbOpt != null);
		assertTrue(fullVerbOpt == null);
		assertArrayEquals(new String[] { "f1", "f2", "f3" }, factFilter.values.toArray(new String[0]));
		assertArrayEquals(new String[] { "a1", "a2", "a3" }, argl.toArray(new String[0]));
	}

	@Test
	public void parseFailureTest() {
		try {
			String[] args = { "-v", "--factory=f1", "-ff2", "a1", "-f", "f3", "a2", "a3", "-f" };
			
			ScriptOptionParser sop = new ScriptOptionParser(args);
			@SuppressWarnings("unused")
			ScriptOption factFilter = sop.getOption("f", "factory", true);

			assertTrue("should not reach here", false);
		} catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "no argument supplied for option \"f\"");
		}
		
		try {
			String[] args = { "-v", "--factory", "-ff2", "a1", "-f", "f3", "a2", "a3" };
			
			ScriptOptionParser sop = new ScriptOptionParser(args);
			@SuppressWarnings("unused")
			ScriptOption factFilter = sop.getOption("f", "factory", true);

			assertTrue("should not reach here", false);
		} catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(), "no argument supplied for option \"f\"");
		}
	}
}
