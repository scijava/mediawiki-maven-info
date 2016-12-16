/*
 * #%L
 * Maven component MediaWiki page generator.
 * %%
 * Copyright (C) 2015 - 2016 Board of Regents of the University of
 * Wisconsin-Madison.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package org.scijava.maven.wiki;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.scijava.util.POM;

/** Tests {@link ComponentIndex}. */
public class ComponentIndexTest {

	private ComponentIndex index;

	@Before
	public void setUp() throws Exception {
		index = new ComponentIndex("org.scijava", "scijava-common", "2.48.0");
	}

	@Test
	public void testGetBaseName() {
		final String baseName = index.getBaseName();
		assertEquals("SciJava Common", baseName);
	}

	@Test
	public void testGenerateMasterTable() {
		final String masterTable = index.generateMasterTable();
		final String[] expected =
			{
				"{| class=\"wikitable\"", //
				"| '''Name'''", //
				"| '''Description'''", //
				"| '''Repository'''", //
				"| '''Artifact'''", //
				"| '''License'''", //
				"| '''Team'''", //
				"|-", //
				"| [http://code.google.com/p/gentyref/ GenTyRef]", //
				"| Generics type reflection library for Java", //
				"| ", //
				"| {{Maven | g=com.googlecode.gentyref | a=gentyref | label=gentyref}}", //
				"| ", //
				"| Wouter Coekaerts", //
				"|-", //
				"| [http://www.eventbus.org EventBus]", //
				"| A simple but powerful publish-subscribe API that is based on class semantics and/or string (topic) matching.", //
				"| [https://eventbus.dev.java.net/source/browse/eventbus/ https://eventbus.dev.java.net/source/browse/eventbus/]", //
				"| {{Maven | g=org.bushe | a=eventbus | label=eventbus}}", //
				"| [[Apache]]", //
				"| {{Person|MichaelBushe}}", //
				"|}" };
		assertArrayEquals(expected, masterTable.split("\\n"));
	}

	@Test
	public void testGenerateComponentTable() {
		final List<POM> poms = index.getPOMs();
		assertEquals(2, poms.size());

		final String[] gentyref =
			{
				"{{Component", //
				"| project = SciJava Common", //
				"| name = GenTyRef", //
				"| url = http://code.google.com/p/gentyref/", //
				"| release = {{Maven | g=com.googlecode.gentyref | a=gentyref | v=1.1.0 | label=1.1.0}}", //
				"| devStatus = {{DevStatus | developer=no | incubating=no | obsolete=no}}", //
				"| supportStatus = {{SupportStatus | debugger=no | reviewer=no | support=no}}", //
				"}}", //
			};
		final String table0 = index.generateComponentTable(poms.get(0));
		assertArrayEquals(gentyref, table0.split("\\n"));

		final String[] eventbus =
			{
				"{{Component", //
				"| project = SciJava Common", //
				"| name = EventBus", //
				"| url = http://www.eventbus.org", //
				"| source = [https://eventbus.dev.java.net/source/browse/eventbus/ https://eventbus.dev.java.net/source/browse/eventbus/]", //
				"| license = [[Apache]]", //
				"| release = {{Maven | g=org.bushe | a=eventbus | v=1.4 | label=1.4}}", //
				"| devStatus = {{DevStatus | developer=yes | incubating=no | obsolete=no}}", //
				"| supportStatus = {{SupportStatus | debugger=no | reviewer=no | support=no}}", //
				"| developers = {{Person|MichaelBushe}}", //
				"}}", //
			};
		final String table1 = index.generateComponentTable(poms.get(1));
		assertArrayEquals(eventbus, table1.split("\\n"));
	}

}
