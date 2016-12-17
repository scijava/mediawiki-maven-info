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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.scijava.util.Manifest;
import org.scijava.util.POM;
import org.scijava.util.XML;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A tool for indexing dependencies of a particular Maven component, and
 * generating MediaWiki-formatted metadata tables describing those dependencies.
 * 
 * @author Curtis Rueden
 */
public class ComponentIndex {

	private static final HashMap<String, String> KNOWN_LICENSES = knownLicenses();

	private static HashMap<String, String> knownLicenses() {
		final HashMap<String, String> map = new HashMap<>();

		map.put("Apache 2", "Apache");
		map.put("Apache License 2", "Apache");
		map.put("Apache License, Version 2.0", "Apache");
		map.put("The Apache Software License, Version 2.0", "Apache");

		map.put("BSD", "BSD");
		map.put("Simplified BSD License", "BSD-2");
		map.put("New BSD License", "BSD-3");

		map.put("GNU GPL v3", "GPLv3");
		map.put("GNU General Public License v3+", "GPLv3");
		map.put("GNU General Public License v2+", "GPLv2");
		map.put("GNU Public License v2", "GPLv2");
		map.put("GPLv2", "GPLv2");

		map.put("LGPL", "LGPL");
		map.put("The GNU Lesser General Public License, Version 3.0", "LGPLv3");
		map.put("LGPLv2", "LGPLv2");

		map.put("The MIT License", "MIT");

		map.put("Public domain", "Public Domain");
		map.put("CC0 1.0 Universal License", "CC0");

		map.put("BIG", "BIG License|BIG");
		map.put("ImageScience", "ImageScience License|ImageScience");

		return map;
	}

	/** POM of the base project. */
	private final POM project;

	/** Collection of dependencies for the base project. */
	private final HashMap<String, String> deps = new HashMap<>();

	/** Cache of retrieved component POMs. */
	private final HashMap<String, POM> pomCache = new HashMap<>();

	/** List of POMs relevant to the base project. */
	private final List<POM> poms;

	/** Name of the base project whose components are being indexed. */
	private String baseName;

	public ComponentIndex(final String g, final String a, final String v)
		throws IOException, ParserConfigurationException, SAXException
	{
		this(g, a, v, POM.getAllPOMs());
	}

	public ComponentIndex(final String g, final String a, final String v,
		final Collection<POM> candidates) throws IOException,
		ParserConfigurationException, SAXException
	{
		project = fetchPOM(g, a, v);
		baseName = project.getProjectName();

		// build list of dependencies for the project
		final ArrayList<Element> depList = dependencies(project);
		for (final Element dep : depList) {
			final String dg = XML.cdata(dep, "groupId");
			final String da = XML.cdata(dep, "artifactId");
			final String dv = XML.cdata(dep, "version");
			deps.put(dg + ":" + da, dv);
		}

		// filter the candidate components
		poms = new ArrayList<>();
		for (final POM pom : candidates) {
			if (isRelevant(pom)) poms.add(pom);
		}
	}

	// -- ComponentIndex methods --

	public POM getProject() {
		return project;
	}

	public String getBaseName() {
		return baseName;
	}

	public void setBaseName(final String baseName) {
		this.baseName = baseName;
	}

	public List<POM> getPOMs() {
		return poms;
	}

	/**
	 * Generates a table containing basic information about each dependency of the
	 * associated project.
	 */
	public String generateMasterTable() {
		final Stringer s = new Stringer();
		s.println("{| class=\"wikitable\"");
		s.println("| '''Name'''");
		s.println("| '''Description'''");
		s.println("| '''Repository'''");
		s.println("| '''Artifact'''");
		s.println("| '''[[License]]'''");
		s.println("| '''[[Team]]'''");
		for (final POM pom : poms) {
			final String g = pom.getGroupId();
			final String a = pom.getArtifactId();

			final String name = pom.getProjectName();
			final String desc = pom.getProjectDescription();
			final String url = pom.getProjectURL();

			final String scmURL = pom.getSCMURL();

			s.println("|-");
			s.println("| ", link(name, url));
			s.println("| ", desc);
			s.println("| ", scmLink(scmURL));
			s.println("| ", mavenLink(g, a));
			s.println("| ", licenseLinks(pom));
			s.println("| ", teamLinks(pom));
		}
		s.println("|}");
		return s.toString();
	}

	/**
	 * Generates a sidebar table with detailed statistics about the given
	 * component.
	 */
	public String generateComponentTable(final POM pom) {
		final Stringer s = new Stringer();

		// coordinates
		final String g = pom.getGroupId();
		final String a = pom.getArtifactId();
		final String v = pom.getVersion();

		// team members
		final Items founders = founderLinks(pom);
		final Items leads = roleLinks(pom, "lead");
		final Items developers = roleLinks(pom, "developer");
		final Items debuggers = roleLinks(pom, "debugger");
		final Items reviewers = roleLinks(pom, "reviewer");
		final Items support = roleLinks(pom, "support");
		final Items maintainers = roleLinks(pom, "maintainer");
		final Items contributors = contributorLinks(pom);
		final Items otherDevs =
			otherDevs(pom, "founder", "lead", "developer", "debugger", "reviewer",
				"support", "maintainer");

		// NB: Parse scijava.team.<role> values.
		// These indicate the number of people needed in each role.
		// If these values are less than the _actual_ number of people currently
		// filling each role, the table will indicate that more help is needed.
		final int leadCount = propertyNumber(pom, "scijava.team.leads");
		final int developerCount = propertyNumber(pom, "scijava.team.developers");
		final int debuggerCount = propertyNumber(pom, "scijava.team.debuggers");
		final int reviewerCount = propertyNumber(pom, "scijava.team.reviewers");
		final int supportCount = propertyNumber(pom, "scijava.team.supports");
		final int maintainerCount = propertyNumber(pom, "scijava.team.maintainers");

		final boolean obsolete = propertyFlag(pom, "scijava.obsolete");
		final String devStatus = devStatus(v, developers, obsolete);
		final String supportStatus = supportStatus(debuggers, reviewers, support);

		s.println("{{Component");
		s.printRow("project", getBaseName());
		s.printRow("name", pom.getProjectName());
		s.printRow("url", pom.getProjectURL());
		s.printRow("source", scmLink(pom.getSCMURL(), pom.getSCMTag(), a, v));
		s.printRow("license", licenseLinks(pom));
		s.printRow("release", mavenLink(g, a, v));
		s.printRow("date", releaseDate(pom));
		s.printRow("devStatus", devStatus);
		s.printRow("supportStatus", supportStatus);
		s.printRow("founders", founders);
		s.printRow("leads", leads);
		s.printRow("developers", developers);
		s.printRow("debuggers", debuggers);
		s.printRow("reviewers", reviewers);
		s.printRow("support", support);
		s.printRow("maintainers", maintainers);
		s.printRow("contributors", contributors);
		s.printRow("otherDevs", otherDevs);
		s.printRow("neededRoles", neededRoles(leadCount, leads, developerCount,
			developers, debuggerCount, debuggers, reviewerCount, reviewers,
			supportCount, support, maintainerCount, maintainers));
		s.println("}}");
		return s.toString();
	}

	// -- Internal methods --

	private boolean isRelevant(final POM pom) {
		return deps.containsKey(pom.getGroupId() + ":" + pom.getArtifactId());
	}

	// -- Helper methods - link building --

	private String mavenLink(final String g, final String a) {
		return "{{Maven | g=" + g + " | a=" + a + " | label=" + a + "}}";
	}

	private String mavenLink(final String g, final String a, final String v) {
		return "{{Maven | g=" + g + " | a=" + a + " | v=" + v + " | label=" + v +
			"}}";
	}

	private String scmLink(final String scmURL) {
		return scmLink(scmURL, null, null, null);
	}

	private String scmLink(final String scmURL, final String scmTag,
		final String a, final String v)
	{
		if (scmURL == null) return null;
		final String gitHubRegex =
			"^https?:\\/\\/github\\.com\\/([^/]*)\\/([^/.]*)(\\.git)?$";
		final Pattern p = Pattern.compile(gitHubRegex);
		final Matcher m = p.matcher(scmURL);
		if (m.matches()) {
			final String org = m.group(1);
			final String repo = m.group(2);
			final String tag = isValidTag(scmTag) ? scmTag : defaultTag(a, v);
			final String tagPart = tag == null ? "" : " | tag=" + tag;
			return "{{GitHub | org=" + org + " | repo=" + repo + tagPart + "}}";
		}

		return "[" + scmURL + " " + scmURL + "]";
	}

	private boolean isValidTag(final String scmTag) {
		return scmTag != null && !scmTag.equals("HEAD");
	}

	private String defaultTag(final String a, final String v) {
		if (a == null || v == null) return null;

		// HACK: Special case for non-conformant net.imagej:ij tags.
		if ("ij".equals(a)) return "v" + v;

		return a + "-" + v;
	}

	private String licenseLinks(final POM pom) {
		final ArrayList<Element> licenses = licenses(pom);
		final StringBuilder sb = new StringBuilder();
		for (final Element license : licenses) {
			final String name = XML.cdata(license, "name");
			if (name == null) continue;
			final String url = XML.cdata(license, "url");

			final String knownLicense = KNOWN_LICENSES.get(name);

			if (sb.length() > 0) sb.append(", ");
			if (knownLicense == null) sb.append(link(name, url));
			else sb.append("[[" + knownLicense + "]]");
		}
		return sb.toString();
	}

	private Date releaseDate(final POM pom) {
		try {
			final Manifest m = Manifest.getManifest(pom);
			if (m == null) return null;
			final String date = m.getImplementationDate();
			if (date == null) return null;
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(date);
		}
		catch (final IOException | ParseException exc) {
			return null;
		}
	}

	private String devStatus(final String v, final Items devDevelopers,
		final boolean obsolete)
	{
		return "{{DevStatus | developer=" + yn(devDevelopers) + " | incubating=" +
			yn(v.startsWith("0.x")) + " | obsolete=" + yn(obsolete) + "}}";
	}

	private String supportStatus(final Items debuggers, final Items reviewers,
		final Items support)
	{
		return "{{SupportStatus | debugger=" + yn(debuggers) + //
			" | reviewer=" + yn(reviewers) + " | support=" + yn(support) + "}}";
	}

	private String teamLinks(final POM pom) {
		final ArrayList<Element> developers = developers(pom);
		final StringBuilder sb = new StringBuilder();
		for (final Element developer : developers) {
			final String id = XML.cdata(developer, "id");
			final String name = XML.cdata(developer, "name");
			if (sb.length() > 0) sb.append(", ");
			sb.append(personLink(id, name));
		}
		return sb.toString();
	}

	private Items founderLinks(final POM pom) {
		final Items founders = roleLinks(pom, "founder");
		// NB: Also add founders currently classified as contributors.
		for (final Element contributor : contributors(pom)) {
			final String id = idProperty(contributor);
			final String name = XML.cdata(contributor, "name");
			final ArrayList<Element> roles = XML.elements(contributor, "role");
			for (final Element role : roles) {
				if ("founder".equalsIgnoreCase(role(role))) {
					founders.add(personLink(id, name));
					break;
				}
			}
		}
		return founders;
	}

	private Items roleLinks(final POM pom, final String roleName) {
		final Items devs = new Items();
		for (final Element developer : developers(pom)) {
			final String id = XML.cdata(developer, "id");
			final String name = XML.cdata(developer, "name");
			final ArrayList<Element> roles = XML.elements(developer, "role");
			for (final Element role : roles) {
				if (roleName.equalsIgnoreCase(role(role))) {
					devs.add(personLink(id, name));
					break;
				}
			}
		}
		return devs;
	}

	private Items otherDevs(final POM pom, final String... knownRoles) {
		final Items devs = new Items();
		for (final Element developer : developers(pom)) {
			final ArrayList<Element> roles = XML.elements(developer, "role");
			final String unknownRoles = unknownRoles(roles, knownRoles);
			if (!unknownRoles.isEmpty()) {
				final String name = XML.cdata(developer, "name");
				devs.add(personLink(null, name + " (" + unknownRoles + ")"));
			}
		}
		return devs;
	}

	private String unknownRoles(final ArrayList<Element> roles,
		final String[] knownRoles)
	{
		final StringBuilder sb = new StringBuilder();
		for (final Element role : roles) {
			final String r = role(role);
			if (!isKnownRole(r, knownRoles)) {
				if (sb.length() > 0) sb.append(", ");
				sb.append(r);
			}
		}
		return sb.toString();
	}

	private boolean isKnownRole(final String role, final String[] knownRoles) {
		for (final String knownRole : knownRoles) {
			if (knownRole.equals(role)) return true;
		}
		return false;
	}

	private String role(final Element role) {
		final String value = XML.cdata(role).trim();
		final int paren = value.indexOf('(');
		return paren < 0 ? value : value.substring(0, paren).trim();
	}

	private Items neededRoles(final int leadCount, final Items leads,
		final int developerCount, final Items developers, final int debuggerCount,
		final Items debuggers, final int reviewerCount, final Items reviewers,
		final int supportCount, final Items support, final int maintainerCount,
		final Items maintainers)
	{
		final Items neededRoles = new Items();
		neededRole(neededRoles, "leads", leadCount, leads.size());
		neededRole(neededRoles, "developers", developerCount, developers.size());
		neededRole(neededRoles, "debuggers", debuggerCount, debuggers.size());
		neededRole(neededRoles, "reviewers", reviewerCount, reviewers.size());
		neededRole(neededRoles, "support", supportCount, support.size());
		neededRole(neededRoles, "maintainers", maintainerCount, maintainers.size());
		return neededRoles;
	}

	private void neededRole(final Items neededRoles, final String name,
		final int total, final int current)
	{
		final int needed = total - current;
		if (needed > 0) neededRoles.add(name + " (" + needed + ")");
	}

	private Items contributorLinks(final POM pom) {
		final Items items = new Items();
		for (final Element contributor : contributors(pom)) {
			final String id = idProperty(contributor);
			final String name = XML.cdata(contributor, "name");
			final String url = XML.cdata(contributor, "url");
			items.add(id == null ? link(name, url) : personLink(id, name));
		}
		return items;
	}

	private String personLink(final String id, final String name) {
		return id == null ? name : "{{Person|" + id + "}}";
	}

	private String link(final String label, final String url) {
		if (url == null || url.isEmpty()) return label;
		final String[] prefixes = { "http://imagej.net/", "http://fiji.sc/" };
		for (final String prefix : prefixes) {
			if (url.startsWith(prefix)) {
				final String path = url.substring(prefix.length()).replace('_', ' ');
				final String page = path.isEmpty() ? "Welcome" : path;
				return "[[" + page + "|" + label + "]]";
			}
		}
		return "[" + url + " " + label + "]";
	}

	// -- Helper methods - XML --

	private String idProperty(final Element el) {
		final NodeList propNodes = el.getElementsByTagName("properties");
		if (propNodes == null || propNodes.getLength() == 0) return null;
		return XML.cdata((Element) propNodes.item(0), "id");
	}

	private String property(final POM pom, final String key) {
		return pom.cdata("//project/properties/" + key);
	}

	private boolean propertyFlag(final POM pom, final String key) {
		return "true".equalsIgnoreCase(property(pom, key));
	}

	private int propertyNumber(final POM pom, final String key) {
		try {
			return Integer.parseInt(property(pom, key));
		}
		catch (final NumberFormatException exc) {
			return -1;
		}
	}

	private ArrayList<Element> dependencies(final POM pom) {
		return elements(pom, "//project/dependencies/dependency");
	}

	private ArrayList<Element> licenses(final POM pom) {
		return elements(pom, "//project/licenses/license");
	}

	private ArrayList<Element> developers(final POM pom) {
		return elements(pom, "//project/developers/developer");
	}

	private ArrayList<Element> contributors(final POM pom) {
		return elements(pom, "//project/contributors/contributor");
	}

	private ArrayList<Element> elements(final POM pom, final String expr) {
		if (pom == null) return new ArrayList<>();
		final ArrayList<Element> elements = pom.elements(expr);
		try {
			return elements.isEmpty() ? elements(parent(pom), expr) : elements;
		}
		catch (final ParserConfigurationException | SAXException
				| IOException exc)
		{
			throw new RuntimeException(exc);
		}
	}

	// -- Helper methods - POMs --

	private POM parent(final POM pom) throws ParserConfigurationException,
		SAXException, IOException
	{
		if (pom == null) return null;
		final String parentG = pom.getParentGroupId();
		final String parentA = pom.getParentArtifactId();
		final String parentV = pom.getParentVersion();
		if (parentG == null || parentA == null || parentV == null) return null;
		return fetchPOM(parentG, parentA, parentV);
	}

	private POM fetchPOM(final String g, final String a, final String v)
		throws ParserConfigurationException, SAXException, IOException
	{
		if (g == null) throw new NullPointerException("Null groupId");
		if (a == null) throw new NullPointerException("Null artifactId");
		if (v == null) throw new NullPointerException("Null version");
		final String gav = g + ":" + a + ":" + v;
		POM pom = pomCache.get(gav);
		if (pom == null) {
			final File file = new File(System.getProperty("user.home") +
				"/.m2/repository/" + g.replace('.', '/') + //
				"/" + a + "/" + v + "/" + a + "-" + v + ".pom");
			if (file.exists()) {
				// read from Maven local repository cache
				pom = new POM(file);
			}
			else {
				// read from remote ImageJ Maven repository
				final String url = "http://maven.imagej.net/content/groups/public/" + //
					g.replace('.', '/') + "/" + a + "/" + v + "/" + a + "-" + v + ".pom";
				pom = new POM(new URL(url));
			}
			pomCache.put(gav, pom);
		}
		return pom;
	}

	// -- Helper methods - succinctness --

	private String yn(final Items items) {
		return items.isEmpty() ? "no" : "yes";
	}

	private String yn(final boolean b) {
		return b ? "yes" : "no";
	}

	// -- Helper classes --

	private static class Stringer {

		private final StringBuilder sb = new StringBuilder();

		public void print(final Object... obj) {
			for (final Object o : obj)
				sb.append(o == null ? "" : o);
		}

		public void println(final Object... o) {
			print(o);
			sb.append("\n");
		}

		public void printRow(final String key, final Object value) {
			if (value == null) return;
			final String sValue = value.toString();
			if (sValue.isEmpty()) return;
			println("| ", key, " = ", sValue);
		}

		@Override
		public String toString() {
			return sb.toString();
		}
	}

	private static class Items extends ArrayList<String> {

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			for (final String s : this) {
				if (sb.length() > 0) sb.append(", ");
				sb.append(s);
			}
			return sb.toString();
		}

	}

}
