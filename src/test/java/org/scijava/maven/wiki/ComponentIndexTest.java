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
		index = new ComponentIndex("ch.qos.logback", "logback-classic", "1.2.3");
	}

	@Test
	public void testVersions() {
		// NB: If this test fails, double check that the version of
		// ch.qos.logback:logback-classic in test scope matches the one in setUp()
		// above, and that the surefire-maven-plugin does not itself have any
		// dependencies on ch.qos.logback:logback-classic or its dependencies.

		final POM pom0 = index.getPOMs().get(0);
		assertEquals("javax.servlet", pom0.getGroupId());
		assertEquals("javax.servlet-api", pom0.getArtifactId());
		assertEquals("3.1.0", pom0.getVersion());

		final POM pom1 = index.getPOMs().get(1);
		assertEquals("ch.qos.logback", pom1.getGroupId());
		assertEquals("logback-core", pom1.getArtifactId());
		assertEquals("1.2.3", pom1.getVersion());

		final POM pom2 = index.getPOMs().get(2);
		assertEquals("org.slf4j", pom2.getGroupId());
		assertEquals("slf4j-api", pom2.getArtifactId());
		assertEquals("1.7.25", pom2.getVersion());
	}

	@Test
	public void testGetBaseName() {
		final String baseName = index.getBaseName();
		assertEquals("Logback Classic Module", baseName);
	}

	@Test
	public void testGenerateMasterTable() {
		final String masterTable = index.generateMasterTable();
		final String[] expected = { //
			"{| class=\"component-table\"", //
			"| '''Name'''", //
			"| '''Description'''", //
			"| '''Repository'''", //
			"| '''Artifact'''", //
			"| '''[[License]]'''", //
			"| '''[[Team]]'''", //
			"|-", //
			"| [http://servlet-spec.java.net Java Servlet API]", //
			"| ", //
			"| [http://java.net/projects/glassfish/sources/svn/show/tags/javax.servlet-api-3.1.0 http://java.net/projects/glassfish/sources/svn/show/tags/javax.servlet-api-3.1.0]", //
			"| {{Maven | g=javax.servlet | a=javax.servlet-api | label=javax.servlet-api}}", //
			"| [https://glassfish.dev.java.net/nonav/public/CDDL+GPL.html CDDL + GPLv2 with classpath exception]", //
			"| {{Person|mode}}, {{Person|swchan2}}", //
			"|-", //
			"| Logback Core Module", //
			"| logback-core module", //
			"| ", //
			"| {{Maven | g=ch.qos.logback | a=logback-core | label=logback-core}}", //
			"| [http://www.eclipse.org/legal/epl-v10.html Eclipse Public License - v 1.0], [http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html GNU Lesser General Public License]", //
			"| {{Person|ceki}}, {{Person|hixi}}", //
			"|-", //
			"| [http://www.slf4j.org SLF4J API Module]", //
			"| The slf4j API", //
			"| ", //
			"| {{Maven | g=org.slf4j | a=slf4j-api | label=slf4j-api}}", //
			"| [http://www.opensource.org/licenses/mit-license.php MIT License]", //
			"| {{Person|ceki}}", //
			"|}", //
		};
		assertArrayEquals(expected, masterTable.split("\\n"));
	}

	@Test
	public void testGenerateComponentTable() {
		final List<POM> poms = index.getPOMs();
		assertEquals(3, poms.size());

		final String[] javaxServletApi = { //
			"{{Component", //
			"| project = Logback Classic Module", //
			"| name = Java Servlet API", //
			"| url = http://servlet-spec.java.net", //
			"| source = [http://java.net/projects/glassfish/sources/svn/show/tags/javax.servlet-api-3.1.0 http://java.net/projects/glassfish/sources/svn/show/tags/javax.servlet-api-3.1.0]", //
			"| license = [https://glassfish.dev.java.net/nonav/public/CDDL+GPL.html CDDL + GPLv2 with classpath exception]", //
			"| release = {{Maven | g=javax.servlet | a=javax.servlet-api | v=3.1.0 | label=3.1.0}}", //
			"| devStatus = {{DevStatus | developer=yes | incubating=no | obsolete=no}}", //
			"| supportStatus = {{SupportStatus | debugger=no | reviewer=no | support=no}}", //
			"| leads = {{Person|mode}}, {{Person|swchan2}}", //
			"| developers = {{Person|swchan2}}", //
			"}}", //
		};
		final String table0 = index.generateComponentTable(poms.get(0));
		assertArrayEquals(javaxServletApi, table0.split("\\n"));

		final String[] logbackCore = { //
			"{{Component", //
			"| project = Logback Classic Module", //
			"| name = Logback Core Module", //
			"| license = [http://www.eclipse.org/legal/epl-v10.html Eclipse Public License - v 1.0], [http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html GNU Lesser General Public License]", //
			"| release = {{Maven | g=ch.qos.logback | a=logback-core | v=1.2.3 | label=1.2.3}}", //
			"| devStatus = {{DevStatus | developer=no | incubating=no | obsolete=no}}", //
			"| supportStatus = {{SupportStatus | debugger=no | reviewer=no | support=no}}", //
			"}}", //
		};
		final String table1 = index.generateComponentTable(poms.get(1));
		assertArrayEquals(logbackCore, table1.split("\\n"));

		final String[] logbackClassic = { //
			"{{Component", //
			"| project = Logback Classic Module", //
			"| name = SLF4J API Module", //
			"| url = http://www.slf4j.org", //
			"| license = [http://www.opensource.org/licenses/mit-license.php MIT License]", //
			"| release = {{Maven | g=org.slf4j | a=slf4j-api | v=1.7.25 | label=1.7.25}}", //
			"| devStatus = {{DevStatus | developer=no | incubating=no | obsolete=no}}", //
			"| supportStatus = {{SupportStatus | debugger=no | reviewer=no | support=no}}", //
			"}}", //
		};
		final String table2 = index.generateComponentTable(poms.get(2));
		assertArrayEquals(logbackClassic, table2.split("\\n"));
	}

}
