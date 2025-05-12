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

package org.eclipse.packager.rpm;

import java.util.BitSet;

public final class RpmVersionValidator {
    private static final String DOT_DOT = "..";

    private static final int NBITS = 128;

    static final char TILDE_CHAR = '~';

    static final char CARAT_CHAR = '^';

    static final BitSet ALPHA = new BitSet(NBITS);

    static {
        ALPHA.set('A', 'Z' + 1);
        ALPHA.set('a', 'z' + 1);
    }

    static final BitSet DIGIT = new BitSet(NBITS);

    static {
        DIGIT.set('0', '9' + 1);
    }

    private static final BitSet ALPHANUM = new BitSet(NBITS);

    static {
        ALPHANUM.or(ALPHA);
        ALPHANUM.or(DIGIT);
    }

    static final BitSet SIGNIFICANT = new BitSet(NBITS);

    static {
        SIGNIFICANT.or(ALPHANUM);
        SIGNIFICANT.set(TILDE_CHAR);
        SIGNIFICANT.set(CARAT_CHAR);
    }

    private static final BitSet NAME = new BitSet(NBITS);

    static {
        NAME.or(ALPHANUM);
        NAME.set('.');
        NAME.set('-');
        NAME.set('_');
        NAME.set('+');
        NAME.set('%');
        NAME.set('{');
        NAME.set('}');
    }

    private static final BitSet FIRST_CHARS_NAME = new BitSet(NBITS);

    static {
        FIRST_CHARS_NAME.or(ALPHANUM);
        FIRST_CHARS_NAME.set('_');
        FIRST_CHARS_NAME.set('%');
    }

    private static final BitSet VERREL = new BitSet(NBITS);

    static {
        VERREL.or(SIGNIFICANT);
        VERREL.set('.');
        VERREL.set('_');
        VERREL.set('+');
    }

    private static final BitSet EVR = new BitSet(NBITS);

    static {
        EVR.or(VERREL);
        EVR.set('-');
        EVR.set(':');
    }

    private RpmVersionValidator() {

    }

    public static void validateName(final String name) {
        validateChars(name, NAME, FIRST_CHARS_NAME);
    }

    public static void validateEpoch(final String epoch) {
        validateChars(epoch, DIGIT);
    }

    public static void validateVersion(final String version) {
        validateChars(version, VERREL);
    }

    public static void validateRelease(final String release) {
        validateVersion(release);
    }

    public static void validateEVR(final String evr) {
        validateChars(evr, EVR);
    }

    private static void validateChars(final String field, final BitSet allowedChars) {
        validateChars(field, allowedChars, null);
    }

    private static void validateChars(final String field, final BitSet allowedChars, final BitSet allowedFirstChars) {
        final int start;

        if (allowedFirstChars == null) {
            start = 0;
        } else {
            final char c = field.charAt(0);

            if (!allowedFirstChars.get(c)) {
                throw new IllegalArgumentException("Illegal char '" + c + "' (0x" + Integer.toHexString(c) + ") in '" + field + "'");
            }

            start = 1;
        }

        final int length = field.length();

        for (int i = start; i < length; i++) {
            final char c = field.charAt(i);
            final boolean allowed = allowedChars.get(c);

            if (!allowed) {
                throw new IllegalArgumentException("Illegal char '" + c + "' (0x" + Integer.toHexString(c) + ") in '" + field + "'");
            }
        }

        if (field.contains(DOT_DOT)) {
            throw new IllegalArgumentException("Illegal sequence '..' in '" + field + "'");
        }
   }
}
