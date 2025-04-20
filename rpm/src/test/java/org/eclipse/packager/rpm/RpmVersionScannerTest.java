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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RpmVersionScannerTest {
    @Test
    void testNext() {
        final RpmVersionScanner scanner = new RpmVersionScanner("1.0a^pre0");
        assertThat(scanner.hasNextDigit()).isTrue();
        assertThat(scanner.next()).asString().isEqualTo("1");
        assertThat(scanner.hasNextDigit()).isTrue();
        assertThat(scanner.next()).asString().isEqualTo("0");
        assertThat(scanner.hasNextAlpha()).isTrue();
        assertThat(scanner.next()).asString().isEqualTo("a");
        assertThat(scanner.hasNextCarat()).isTrue();
        assertThat(scanner.next()).asString().isEqualTo("^");
        assertThat(scanner.hasNextAlpha()).isTrue();
        assertThat(scanner.hasNextDigit()).isFalse();
        assertThat(scanner.hasNextCarat()).isFalse();
        assertThat(scanner.next()).asString().isEqualTo("pre");
        assertThat(scanner.next()).asString().isEqualTo("0");
        assertThat(scanner.hasNext()).isFalse();
        assertThat(scanner.hasNextAlpha()).isFalse();
        assertThat(scanner.hasNextDigit()).isFalse();
        assertThat(scanner.hasNextCarat()).isFalse();
        assertThat(scanner.hasNextTilde()).isFalse();
        assertThatThrownBy(scanner::next).isExactlyInstanceOf(NoSuchElementException.class).hasMessage(null);
    }

    @Test
    void testTokenize() {
        final RpmVersionScanner scanner = new RpmVersionScanner("2.0.01~Final");
        final Spliterator<CharSequence> spliterator = Spliterators.spliteratorUnknownSize(scanner, Spliterator.ORDERED);
        final List<String> tokens = StreamSupport.stream(spliterator, false).map(CharSequence::toString).collect(Collectors.toList());
        assertThat(tokens).containsExactly("2" , "0", "1", "~", "Final");
    }
}
