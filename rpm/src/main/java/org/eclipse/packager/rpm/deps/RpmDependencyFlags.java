/*
 * Copyright (c) 2015, 2019 Contributors to the Eclipse Foundation
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

package org.eclipse.packager.rpm.deps;

import java.util.EnumSet;
import java.util.Set;

public enum RpmDependencyFlags {
    LESS(1),
    GREATER(2),
    EQUAL(3),
    POSTTRANS(5),
    PREREQ(6),
    PRETRANS(7),
    INTERPRETER(8),
    SCRIPT_PRE(9),
    SCRIPT_POST(10),
    SCRIPT_PREUN(11),
    SCRIPT_POSTUN(12),
    SCRIPT_VERIFY(13),
    FIND_REQUIRES(14),
    FIND_PROVIDES(15),
    TRIGGER_IN(16),
    TRIGGER_UN(17),
    TRIGGER_POSTUN(18),
    MISSINGOK(19),
    RPMLIB(24),
    TRIGGER_PREIN(25),
    KEYRING(26),
    CONFIG(28);

    private int value;

    private RpmDependencyFlags(final int bit) {
        this.value = 1 << bit;
    }

    public static int encode(final Set<RpmDependencyFlags> flags) {
        int value = 0;
        for (final RpmDependencyFlags flag : flags) {
            value |= flag.value;
        }
        return value;
    }

    public static Set<RpmDependencyFlags> parse(final Long flags) {
        if (flags == null) {
            return null;
        }

        return parse(flags.intValue());
    }

    public static Set<RpmDependencyFlags> parse(final int flags) {
        final EnumSet<RpmDependencyFlags> result = EnumSet.noneOf(RpmDependencyFlags.class);

        for (final RpmDependencyFlags f : values()) {
            if ((flags & f.value) > 0) {
                result.add(f);
            }
        }

        return result;
    }
}
