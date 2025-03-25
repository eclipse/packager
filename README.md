# Eclipse Packager™ [![Maven Central](https://img.shields.io/maven-central/v/org.eclipse.packager/packager)](https://search.maven.org/search?q=g:org.eclipse.packager "Eclipse Packager") [![License](https://img.shields.io/github/license/eclipse/packager)](https://github.com/eclipse/packager/blob/master/LICENSE) [![Matrix](https://img.shields.io/matrix/packager:matrix.eclipse.org)](https://matrix.to/#/#packager:matrix.eclipse.org)


Create Linux software packages in plain Java.

Currently, this project can:

* Read and create Debian packages (`.deb`)
* Read, create, and sign RPM packages (`.rpm`)

There are three modules: core, deb and rpm. To import, depend on the modules you require:

* `org.eclipse.packager:packager-deb:$version`
* `org.eclipse.packager:packager-rpm:$version`
