/*
 * #%L
 * Maven component MediaWiki page generator.
 * %%
 * Copyright (C) 2015 Board of Regents of the University of
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

public class Info {

	// -- Main method --

	public static void main(final String[] args) throws Exception {
		final String g = arg("info.groupId", true);
		final String a = arg("info.artifactId", true);
		final String v = arg("info.version", true);
		final String urlPath = arg("info.url", false);
		final URL url = urlPath == null ? null : new URL(urlPath);

		final WikiUpdater wikiUpdater = new WikiUpdater(g, a, v, url);
		wikiUpdater.update();
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
