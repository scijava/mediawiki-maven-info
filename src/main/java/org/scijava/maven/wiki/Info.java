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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * A spiffy software component table analyzer.
 * <p>
 * It generates MediaWiki-formatted metadata tables, and uploads them to the
 * given MediaWiki instance.
 * </p>
 * <p>
 * Run it from the CLI on a given component via:
 * </p>
 * <code><pre>
 * mvn -Pexec,info \
 *     -Dinfo.url=http://imagej.net/ \
 *     -Dinfo.groupId=net.imagej \
 *     -Dinfo.artifactId=imagej \
 *     -Dinfo.version=2.0.0-rc-42
 * </pre></code>
 * <p>
 * The {@code info.url} is optional; without it, the analyzer performs a dry
 * run, dumping the resultant tables to stdout.
 * </p>
 * 
 * @author Curtis Rueden
 * @see ComponentIndex
 * @see WikiUpdater
 */
public class Info {

	// -- Main method --

	public static void main(final String[] args) throws Exception {
		final String urlPath = arg("info.url", false);
		final URL url = urlPath == null ? null : new URL(urlPath);

		final int maxProjects = 9;
		final ArrayList<ComponentIndex> indices = new ArrayList<>(maxProjects);
		final HashSet<ComponentIndex> includeBase = new HashSet<>();
		for (int p=1; p<=9; p++) {
			final boolean first = p == 1;
			final String prefix = first ? "info." : "info.project" + p + ".";
			final String g = arg(prefix + "groupId", first);
			final String a = arg(prefix + "artifactId", first);
			final String v = arg(prefix + "version", first);
			if (g == null) break; // no more projects to process
			final ComponentIndex index = new ComponentIndex(g, a, v);
			final String name = arg(prefix + "name", false);
			if (name != null) index.setBaseName(name);
			indices.add(index);
			if (arg(prefix + "includeBase", false) != null) includeBase.add(index);
		}

		final WikiUpdater wikiUpdater = new WikiUpdater(url);
		for (final ComponentIndex index : indices) {
			wikiUpdater.update(index, includeBase.contains(index));
		}
	}

	// -- Helper methods --

	private static String arg(final String var, final boolean required) {
		final String value = property(var);
		if (required && value == null) {
			throw new RuntimeException("The property " + var +
				" is required but unset.");
		}
		return value;
	}

	private static String property(String var) {
		final String value = System.getProperty(var);
		return value == null || value.equals("${" + var + "}") ? null : value;
	}

}
