# MediaWiki Maven Info #

A spiffy software component metadata analyzer and table generator.

It generates MediaWiki-formatted metadata tables, and uploads them to a
MediaWiki instance.

The [ImageJ](http://imagej.net/) and [Fiji](http://fiji.sc/) projects use this
tool to keep their respective lists of dependencies up-to-date and documented
online in a human-friendly form.

## Example of usage ##

Run it from the CLI on a given component via:

    mvn -Pexec,info \
        -Dinfo.url=http://imagej.net/ \
        -Dinfo.groupId=net.imagej \
        -Dinfo.artifactId=imagej \
        -Dinfo.version=2.0.0-rc-42

The `info.url` is optional; without it, the analyzer performs a dry run,
dumping the resultant tables to stdout.
