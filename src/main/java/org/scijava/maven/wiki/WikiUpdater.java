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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.xml.parsers.ParserConfigurationException;

import org.scijava.util.POM;
import org.wikipedia.Wiki;
import org.xml.sax.SAXException;

/**
 * The wiki updater knows how to upload information about a particular software
 * component, specified as a Maven GAV and indexed via a {@link ComponentIndex},
 * to a given remote MediaWiki server.
 * <p>
 * It uploads two things: 1) a dependency table named as
 * {@code Template:ComponentTable:<G>:<A>} that contains basic information about
 * each dependency of the given {@code G:A:V}; and 2) for each dependency
 * {@code dG:dA:dV}, a {@code Template:ComponentStats:<dG>:<dA>} containing a
 * sidebar table with detailed statistics about that component.
 * </p>
 * <p>
 * These statistics can then be included as needed using the normal template
 * transclusion mechanism&mdash;e.g.:
 * <code>{{ComponentStats:org.scijava:scijava-common}}</code>.
 * </p>
 * 
 * @author Curtis Rueden
 */
public class WikiUpdater {

	private static final String SEP = ":";

	private final String groupId;
	private final String artifactId;

	private final ComponentIndex index;
	private final Wiki wiki;
	private final String summary;

	public WikiUpdater(final String groupId, final String artifactId,
		final String version, final URL url) throws IOException,
		ParserConfigurationException, SAXException, FailedLoginException
	{
		this.groupId = groupId;
		this.artifactId = artifactId;
		index = new ComponentIndex(groupId, artifactId, version);

		if (url == null) wiki = null;
		else {
			final Credentials c = new Credentials(url);
			wiki = new Wiki(url.getHost(), url.getPath(), url.getProtocol());
			if (c.isValid()) wiki.login(c.user, c.pass);
		}

		summary = "Update to " + groupId + ":" + artifactId + ":" + version;
	}

	// -- WikiUpdater methods --

	/** Updates the wiki with the information from the associated Maven GAV. */
	public void update() throws LoginException, IOException {
		final String masterTable = index.generateMasterTable();
		upload("ComponentTable", groupId, artifactId, masterTable);

		for (final POM pom : index.getPOMs()) {
			final String componentTable = index.generateComponentTable(pom);
			upload("ComponentStats", pom.getGroupId(), pom.getArtifactId(),
				componentTable);
		}
	}

	// -- Helper methods --

	private void upload(final String base, final String g, final String a,
		final String text) throws LoginException, IOException
	{
		final String pageName = "Template:" + base + SEP + g + SEP + a;
		if (wiki == null) {
			// dry run
			System.out.println();
			System.out.println("[" + pageName + "]");
			System.out.println(text);
		}
		else wiki.edit(pageName, text, summary);
	}

	// -- Helper classes --

	private static class Credentials {
		public String user, pass;

		public Credentials(final URL url) {
			// parse from URL's userInfo
			final String userInfo = url.getUserInfo();
			if (userInfo != null) {
				final int colon = userInfo.indexOf(":");
				if (colon < 0) user = userInfo;
				else {
					user = userInfo.substring(0, colon);
					pass = userInfo.substring(colon + 1);
				}
			}

			if (user == null || pass == null) {
				// parse from ~/.netrc
				final File netrc = new File(System.getProperty("user.home"), ".netrc");
				boolean relevantMachine = false;
				if (netrc.exists()) {
					BufferedReader in = null;
					try {
						in = new BufferedReader(new FileReader(netrc));
						while (user == null || pass == null) {
							final String line = in.readLine();
							if (line == null) break; // EOF
							final int space = line.indexOf(" ");
							if (space < 0) continue; // non-key-value-pair

							final String key = line.substring(0, space);
							final String value = line.substring(space + 1);

							if (key.equals("machine")) {
								relevantMachine = value.equals(url.getHost());
							}
							if (!relevantMachine) continue;

							if (key.equals("login")) user = value;
							if (key.equals("password")) pass = value;
						}
					}
					catch (final IOException exc) {
						exc.printStackTrace();
					}
					finally {
						if (in != null) {
							try {
								in.close();
							}
							catch (final IOException exc) {
								exc.printStackTrace();
							}
						}
					}
				}
			}
		}

		public boolean isValid() {
			return user != null && pass != null;
		}

	}

}
