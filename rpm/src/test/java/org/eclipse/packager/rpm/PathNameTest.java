/*
 * Copyright (c) 2016, 2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.packager.rpm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class PathNameTest {
    @ParameterizedTest
    @CsvSource(value = {"/,'',''", "/foo,'',foo", "/foo/bar,foo,bar", "/foo/bar/baz,foo/bar,baz", "/foo//bar/baz,foo/bar,baz", "/foo//bar/baz/,foo/bar,baz"})
    void assertPath(final String pathName, final String dirname, final String basename) {
        final PathName result = PathName.parse(pathName);
        assertThat(result).extracting("dirname").isEqualTo(dirname);
        assertThat(result).extracting("basename").isEqualTo(basename);
    }
}
