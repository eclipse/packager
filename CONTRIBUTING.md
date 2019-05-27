# How to contribute to Eclipse Packager™

First of all, thanks for considering to contribute to Eclipse Packager.
We really appreciate the time and effort you want to spend helping to
improve things around here. And help we can use :-)

Here is a (non-exclusive, non-prioritized) list of things you might be able to help us with:

* bug reports
* bug fixes
* improvements regarding code quality e.g. improving readability, performance, modularity etc.
* documentation (Getting Started guide, Examples, Deployment instructions for cloud environments)
* features (both ideas and code are welcome)

You may also want to have a look at the list of open issues marked with
the "need help" label.

In order to get you started as fast as possible we need to go through some organizational
issues first, though.

## Legal Requirements

Packager is an [Eclipse Foundation](https://eclipse.org) project and as such is
governed by the Eclipse Development process. This process helps us in creating
great open source software within a safe legal framework.

For you as a contributor, the following preliminary steps are required in order
for us to be able to accept your contribution:

* Sign the [Eclipse Contributor Agreement](https://www.eclipse.org/legal/ECA.php).

  In order to do so:

  * Obtain an Eclipse Foundation user ID. Anyone who currently uses Eclipse
    Bugzilla or Gerrit systems already has one of those.
    
    If you don't already have an account simply [register on the Eclipse web site](https://accounts.eclipse.org/user/register).
  * Once you have your account, log in to the [projects portal](https://projects.eclipse.org/), select *My Account* and then the *Contributor License Agreement* tab.

* Add your GitHub username to your Eclipse Foundation account. Log in to Eclipse and go to [Edit my account](https://accounts.eclipse.org/user).

The easiest way to contribute code/patches/whatever is by creating a GitHub pull request (PR). When you do make sure that you add the `Signed-off-by` footer your commit records using the same email address used for your Eclipse account.

You do this by adding the `-s` flag when you make the commit(s), e.g.

    git commit -s -m "Shave the yak some more"

You can find all the details in the section [Git Commit Records](https://www.eclipse.org/projects/handbook/#resources-commit) of the [Eclipse Project Handbook](https://www.eclipse.org/projects/handbook).

## Making your Changes

* Fork the repository on GitHub
* Create a new branch for your changes
* Make your changes
* When you create new files make sure you include a proper license header at the top of the file (see License Header section below).
* Make sure you include test cases for non-trivial features
* Make sure the test suite passes after your changes
* Commit your changes into that branch
* Use descriptive and meaningful commit messages
* If you have a lot of commits squash them into a single commit
* Make sure you use the `-s` flag when committing as explained above
* Push your changes to your branch in your forked repository

## Submitting the Changes

Submit a pull request via the normal GitHub UI.

## After Submitting

* Do not use your branch for any other development, otherwise further changes that you make will be visible in the PR.

## License Header

Please make sure any file you newly create contains a proper license header like this:

````
/**
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
````
You should, of course, adapt this header to use the specific mechanism for comments pertaining to the type of file you create, e.g. using something like

````
<!--
 Copyright (c) 2019 Contributors to the Eclipse Foundation

 See the NOTICE file(s) distributed with this work for additional
 information regarding copyright ownership.

 This program and the accompanying materials are made available under the
 terms of the Eclipse Public License 2.0 which is available at
 https://www.eclipse.org/legal/epl-2.0

 SPDX-License-Identifier: EPL-2.0
-->
````

when adding an XML file.
