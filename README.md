# Eclipse Packager™ [![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.packager/packager)](https://search.maven.org/search?q=g:org.eclipse.packager "Eclipse Packager")

Create Linux software packages in plain Java.

Currently, this project can:

* Read and create Debian packages (`.deb`)
* Read and create RPM packages (`.rpm`)

There are three modules: core, deb and rpm. To import, depend on the modules you require:

* `org.eclipse.packager:packager-deb:$version`
* `org.eclipse.packager:packager-rpm:$version`
