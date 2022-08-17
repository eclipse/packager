/*
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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

/**
 * Constants to identify the RPM verification flags.
 * See https://github.com/ctron/rpm-builder/issues/41
 * The name of this enum is questionable.
 * It should rather be "VerifyFlag" (singular), but I leave it this way, cf. {@link FileFlags}.
 * The constants and their value are from http://ftp.rpm.org/api/4.14.0/group__rpmvf.html
 *
 * @since 0.15.2
 */
public enum VerifyFlags {
    // The following constants control the verify flags.
    // Each bit corresponds to an upper case character in the output of "rpm -V ..."
    MD5(1 << 0), // '5'
    SIZE(1 << 1), // 'S'
    LINKTO(1 << 2), // 'L'
    USER(1 << 3), // 'U'
    GROUP(1 << 4), // 'G'
    MTIME(1 << 5), // 'T'
    MODE(1 << 6), // 'M'
    RDEV(1 << 7), // 'D'
    CAPS(1 << 8), // 'P'

    // The purpose of the following constants is not clear to me.
    // Do they refer to the same bitmask? Oliver Matz
    // see discussion in https://github.com/eclipse/packager/pull/6
    VERIFY_CONTEXTS(1 << 15),
    VERIFY_FILES(1 << 16),
    VERIFY_DEPS(1 << 17),
    VERIFY_SCRIPT(1 << 18),
    VERIFY_DIGEST(1 << 19),
    VERIFY_SIGNATURE(1 << 20),
    VERIFY_PATCHES(1 << 21),
    VERIFY_HDRCHK(1 << 22),
    VERIFY_FOR_LIST(1 << 23),
    VERIFY_FOR_STATE(1 << 24),
    VERIFY_FOR_DOCS(1 << 25),
    VERIFY_FOR_CONFIG(1 << 26),
    VERIFY_FOR_DUMPFILES(1 << 27);

    private final int value;

    VerifyFlags(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
