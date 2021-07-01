[![](https://github.com/scijava/mediawiki-maven-info/actions/workflows/build-main.yml/badge.svg)](https://github.com/scijava/mediawiki-maven-info/actions/workflows/build-main.yml)

# MediaWiki Maven Info #

A spiffy software component metadata analyzer and table generator.

It generates MediaWiki-formatted metadata tables, and uploads them to a
MediaWiki instance.

The [ImageJ](https://imagej.net/) and [Fiji](https://fiji.sc/) projects use
this tool to keep their respective lists of dependencies up-to-date and
documented online in a human-friendly form.

## Example of usage ##

Run it from the CLI on a given component via:

    mvn -Dmwmi.url=https://imagej.net/ \
        -Dmwmi.groupId=net.imagej \
        -Dmwmi.artifactId=imagej \
        -Dmwmi.version=2.0.0-rc-42

The `mwmi.url` is optional; without it, the analyzer performs a dry run,
dumping the resultant tables to stdout.

## Analyzing multiple projects ##

Here is an example invocation which layers multiple projects:

    mvn -Dinfo.url=https://imagej.net/ \
        -Dmwmi.groupId=net.imagej \
        -Dmwmi.artifactId=imagej \
        -Dmwmi.version=RELEASE \
        -Dmwmi.includeBase \
        -Dmwmi.groupId2=sc.fiji \
        -Dmwmi.artifactId2=fiji \
        -Dmwmi.version2=RELEASE \
        -Dmwmi.includeBase2

The above is what the ImageJ and Fiji projects use in order
to generate their component sidebars and project tables
which appear on the [ImageJ wiki](https://imagej.net/).
