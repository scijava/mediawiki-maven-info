# MediaWiki Maven Info #

___This project is obsolete.___ It was used to generate MediaWiki content
describing the development and support status of components according to
metadata specified in each component's Maven POM file.

This code generates MediaWiki-formatted metadata tables, and can upload them
to a MediaWiki instance.

The [ImageJ](https://imagej.net/) and [Fiji](https://fiji.sc/) projects used
this tool to keep their respective lists of dependencies up-to-date and
documented online in a human-friendly form, before the
[ImageJ wiki](https://imagej.net/) moved to GitHub Pages.

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

The above is what the ImageJ and Fiji projects once used in order to generate
their component sidebars and project tables which appeared on the [MediaWiki
incarnation of the ImageJ wiki](https://imagej.net/imagej-wiki-static).
