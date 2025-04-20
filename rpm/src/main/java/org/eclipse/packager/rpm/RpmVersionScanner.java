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

import java.nio.CharBuffer;
import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

final class RpmVersionScanner implements Iterator<CharSequence> {
    private static final char TILDE_CHAR = '~';

    private static final String TILDE_STRING = "~";

    private static final char CARAT_CHAR = '^';

    private static final String CARAT_STRING = "^";

    private static final BitSet ALPHA = new BitSet(128);

    static {
        ALPHA.set('A', 'Z');
        ALPHA.set('a', 'z');
    }

    private static final BitSet DIGIT = new BitSet(128);

    static {
        DIGIT.set('0', '9');
    }

    private static final BitSet SIGNIFICANT = new BitSet(128);

    static {
        SIGNIFICANT.or(ALPHA);
        SIGNIFICANT.or(DIGIT);
        SIGNIFICANT.set(TILDE_CHAR);
        SIGNIFICANT.set(CARAT_CHAR);
    }

    private final CharBuffer buf;

    private int position;

    public RpmVersionScanner(final CharSequence input) {
        this.buf = CharBuffer.wrap(Objects.requireNonNull(input));
    }

    @Override
    public boolean hasNext() {
        skipInsignificantChars();
        return (position < buf.length());
    }

    public boolean hasNextAlpha() {
        return hasNext(ALPHA);
    }

    public boolean hasNextDigit() {
        return hasNext(DIGIT);
    }

    public boolean hasNextTilde() {
        return hasNext(TILDE_CHAR);
    }

    public boolean hasNextCarat() {
        return hasNext(CARAT_CHAR);
    }

    @Override
    public CharSequence next() {
        if (position >= buf.length()) {
            throw new NoSuchElementException();
        }

        skipInsignificantChars();

        final char c = buf.charAt(position);

        if (c == TILDE_CHAR) {
            position++;
            return TILDE_STRING;
        }

        if (c == CARAT_CHAR) {
            position++;
            return CARAT_STRING;
        }

        return DIGIT.get(c) ? nextDigit() : nextAlpha();
    }

    private CharSequence nextAlpha() {
        return next(ALPHA);
    }

    private CharSequence nextDigit() {
        return next(DIGIT);
    }

    private void skipInsignificantChars() {
        while (position < buf.length() && !SIGNIFICANT.get(buf.charAt(position))) {
            position++;
        }
    }

    private boolean hasNext(BitSet bitSet) {
        return (hasNext() && bitSet.get(buf.charAt(position)));
    }

    private boolean hasNext(char c) {
        return (hasNext() && buf.charAt(position) == c);
    }

    private int skipLeadingZeros() {
        int start = position;

        while (start + 1 < buf.length() && buf.charAt(start) == '0' && DIGIT.get(buf.charAt(start + 1))) {
            start++;
        }

        return start;
    }

    private CharBuffer next(BitSet bitSet) {
        skipInsignificantChars();

        final int start = skipLeadingZeros();

        while (position < buf.length() && bitSet.get(buf.charAt(position))) {
            position++;
        }

        return buf.subSequence(start, position);
    }
}
