/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.ning.http.client.async;

import com.ning.http.client.FluentStringsMap;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class FluentStringsMapTest {
    @Test
    public void emptyTest() {
        FluentStringsMap map = new FluentStringsMap();

        assertTrue(map.keySet().isEmpty());
    }

    @Test
    public void normalTest() {
        FluentStringsMap map = new FluentStringsMap();

        map.add("fOO", "bAr");
        map.add("Baz", Arrays.asList("fOo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("fOO", "Baz")));

        assertEquals(map.getFirstValue("fOO"), "bAr");
        assertEquals(map.getJoinedValue("fOO", ", "), "bAr");
        assertEquals(map.get("fOO"), Arrays.asList("bAr"));
        assertNull(map.getFirstValue("foo"));
        assertNull(map.getJoinedValue("foo", ", "));
        assertNull(map.get("foo"));

        assertEquals(map.getFirstValue("Baz"), "fOo");
        assertEquals(map.getJoinedValue("Baz", ", "), "fOo, bar");
        assertEquals(map.get("Baz"), Arrays.asList("fOo", "bar"));
        assertNull(map.getFirstValue("baz"));
        assertNull(map.getJoinedValue("baz", ", "));
        assertNull(map.get("baz"));
    }

    @Test
    public void addNullTest() {
        FluentStringsMap map = new FluentStringsMap();

        map.add("fOO", "bAr");
        map.add(null, Arrays.asList("fOo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("fOO")));

        assertEquals(map.getFirstValue("fOO"), "bAr");
        assertEquals(map.getJoinedValue("fOO", ", "), "bAr");
        assertEquals(map.get("fOO"), Arrays.asList("bAr"));
        assertNull(map.getFirstValue("foo"));
        assertNull(map.getJoinedValue("foo", ", "));
        assertNull(map.get("foo"));

        assertNull(map.getFirstValue(null));
        assertNull(map.getJoinedValue("Baz", ", "));
        assertNull(map.get(null));
    }

    @Test
    public void sameKeyMultipleTimesTest() {
        FluentStringsMap map = new FluentStringsMap();

        map.add("foo", "baz,foo");
        map.add("foo", Arrays.asList("bar"));
        map.add("foo", "bla", "blubb");
        map.add("fOO", "duh");

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "fOO")));

        assertEquals(map.getFirstValue("foo"), "baz,foo");
        assertEquals(map.getJoinedValue("foo", ", "), "baz,foo, bar, bla, blubb");
        assertEquals(map.get("foo"), Arrays.asList("baz,foo", "bar", "bla", "blubb"));
        assertEquals(map.getFirstValue("fOO"), "duh");
        assertEquals(map.getJoinedValue("fOO", ", "), "duh");
        assertEquals(map.get("fOO"), Arrays.asList("duh"));
    }

    @Test
    public void emptyValueTest() {
        FluentStringsMap map = new FluentStringsMap();

        map.add("foo", "");

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo")));
        assertEquals(map.getFirstValue("foo"), "");
        assertEquals(map.getJoinedValue("foo", ", "), "");
        assertEquals(map.get("foo"), Arrays.asList(""));
    }

    @Test
    public void nullValueTest() {
        FluentStringsMap map = new FluentStringsMap();

        map.add("foo", (String) null);

        assertEquals(map.getFirstValue("foo"), null);
        assertEquals(map.getJoinedValue("foo", ", "), null);
        assertEquals(map.get("foo").size(), 1);
    }

    @Test
    public void mapConstructorTest() {
        Map<String, Collection<String>> headerMap = new LinkedHashMap<>();

        headerMap.put("foo", Arrays.asList("baz,foo"));
        headerMap.put("baz", Arrays.asList("bar"));
        headerMap.put("bar", Arrays.asList("bla", "blubb"));

        FluentStringsMap map = new FluentStringsMap(headerMap);

        headerMap.remove("foo");
        headerMap.remove("bar");
        headerMap.remove("baz");

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz", "bar")));
        assertEquals(map.getFirstValue("foo"), "baz,foo");
        assertEquals(map.getJoinedValue("foo", ", "), "baz,foo");
        assertEquals(map.get("foo"), Arrays.asList("baz,foo"));
        assertEquals(map.getFirstValue("baz"), "bar");
        assertEquals(map.getJoinedValue("baz", ", "), "bar");
        assertEquals(map.get("baz"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("bar"), "bla");
        assertEquals(map.getJoinedValue("bar", ", "), "bla, blubb");
        assertEquals(map.get("bar"), Arrays.asList("bla", "blubb"));
    }

    @Test
    public void mapConstructorNullTest() {
        FluentStringsMap map = new FluentStringsMap((Map<String, Collection<String>>) null);

        assertEquals(map.keySet().size(), 0);
    }

    @Test
    public void copyConstructorTest() {
        FluentStringsMap srcHeaders = new FluentStringsMap();

        srcHeaders.add("foo", "baz,foo");
        srcHeaders.add("baz", Arrays.asList("bar"));
        srcHeaders.add("bar", "bla", "blubb");

        FluentStringsMap map = new FluentStringsMap(srcHeaders);

        srcHeaders.delete("foo");
        srcHeaders.delete("bar");
        srcHeaders.delete("baz");
        assertTrue(srcHeaders.keySet().isEmpty());

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz", "bar")));
        assertEquals(map.getFirstValue("foo"), "baz,foo");
        assertEquals(map.getJoinedValue("foo", ", "), "baz,foo");
        assertEquals(map.get("foo"), Arrays.asList("baz,foo"));
        assertEquals(map.getFirstValue("baz"), "bar");
        assertEquals(map.getJoinedValue("baz", ", "), "bar");
        assertEquals(map.get("baz"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("bar"), "bla");
        assertEquals(map.getJoinedValue("bar", ", "), "bla, blubb");
        assertEquals(map.get("bar"), Arrays.asList("bla", "blubb"));
    }

    @Test
    public void copyConstructorNullTest() {
        FluentStringsMap map = new FluentStringsMap((FluentStringsMap) null);

        assertEquals(map.keySet().size(), 0);
    }

    @Test
    public void deleteTest() {
        FluentStringsMap map = new FluentStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.delete("baz");

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertNull(map.getFirstValue("baz"));
        assertNull(map.getJoinedValue("baz", ", "));
        assertNull(map.get("baz"));
    }

    @Test
    public void deleteTestDifferentCase() {
        FluentStringsMap map = new FluentStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.delete("bAz");

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
    }

    @Test
    public void deleteUndefinedKeyTest() {
        FluentStringsMap map = new FluentStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.delete("bar");

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
    }

    @Test
    public void deleteNullTest() {
        FluentStringsMap map = new FluentStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.delete(null);

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
    }

    @Test
    public void deleteAllArrayTest() {
        FluentStringsMap map = new FluentStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.deleteAll("baz", "Boo");

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertNull(map.getFirstValue("baz"));
        assertNull(map.getJoinedValue("baz", ", "));
        assertNull(map.get("baz"));
    }

    @Test
    public void deleteAllArrayDifferentCaseTest() {
        FluentStringsMap map = new FluentStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.deleteAll("Foo", "baz");

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertNull(map.getFirstValue("baz"));
        assertNull(map.getJoinedValue("baz", ", "));
        assertNull(map.get("baz"));
    }

    @Test
    public void deleteAllCollectionTest() {
        FluentStringsMap map = new FluentStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.deleteAll(Arrays.asList("baz", "foo"));

        assertEquals(map.keySet(), Collections.<String>emptyList());
        assertNull(map.getFirstValue("foo"));
        assertNull(map.getJoinedValue("foo", ", "));
        assertNull(map.get("foo"));
        assertNull(map.getFirstValue("baz"));
        assertNull(map.getJoinedValue("baz", ", "));
        assertNull(map.get("baz"));
    }

    @Test
    public void deleteAllCollectionDifferentCaseTest() {
        FluentStringsMap map = new FluentStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.deleteAll(Arrays.asList("bAz", "fOO"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
    }

    @Test
    public void deleteAllNullArrayTest() {
        FluentStringsMap map = new FluentStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.deleteAll((String[]) null);

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
    }

    @Test
    public void deleteAllNullCollectionTest() {
        FluentStringsMap map = new FluentStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.deleteAll((Collection<String>) null);

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
    }

    @Test
    public void replaceArrayTest() {
        FluentStringsMap map = new FluentStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.replaceWith("foo", "blub", "bla");

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "blub");
        assertEquals(map.getJoinedValue("foo", ", "), "blub, bla");
        assertEquals(map.get("foo"), Arrays.asList("blub", "bla"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
    }

    @Test
    public void replaceCollectionTest() {
        FluentStringsMap map = new FluentStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.replaceWith("foo", Arrays.asList("blub", "bla"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "blub");
        assertEquals(map.getJoinedValue("foo", ", "), "blub, bla");
        assertEquals(map.get("foo"), Arrays.asList("blub", "bla"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
    }

    @Test
    public void replaceDifferentCaseTest() {
        FluentStringsMap map = new FluentStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.replaceWith("Foo", Arrays.asList("blub", "bla"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz", "Foo")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
        assertEquals(map.getFirstValue("Foo"), "blub");
        assertEquals(map.getJoinedValue("Foo", ", "), "blub, bla");
        assertEquals(map.get("Foo"), Arrays.asList("blub", "bla"));
    }

    @Test
    public void replaceUndefinedTest() {
        FluentStringsMap map = new FluentStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.replaceWith("bar", Arrays.asList("blub"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz", "bar")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
        assertEquals(map.getFirstValue("bar"), "blub");
        assertEquals(map.getJoinedValue("bar", ", "), "blub");
        assertEquals(map.get("bar"), Arrays.asList("blub"));
    }

    @Test
    public void replaceNullTest() {
        FluentStringsMap map = new FluentStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.replaceWith(null, Arrays.asList("blub"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
    }

    @Test
    public void replaceValueWithNullTest() {
        FluentStringsMap map = new FluentStringsMap();

        map.add("foo", "bar");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.replaceWith("baz", (Collection<String>) null);

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertNull(map.getFirstValue("baz"));
        assertNull(map.getJoinedValue("baz", ", "));
        assertNull(map.get("baz"));
    }

    @Test
    public void replaceAllMapTest1() {
        FluentStringsMap map = new FluentStringsMap();

        map.add("foo", "bar");
        map.add("bar", "foo, bar", "baz");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "bar", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("bar"), "foo, bar");
        assertEquals(map.getJoinedValue("bar", ", "), "foo, bar, baz");
        assertEquals(map.get("bar"), Arrays.asList("foo, bar", "baz"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.replaceAll(new FluentStringsMap().add("bar", "baz").add("Foo", "blub", "bla"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "bar", "baz", "Foo")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("bar"), "baz");
        assertEquals(map.getJoinedValue("bar", ", "), "baz");
        assertEquals(map.get("bar"), Arrays.asList("baz"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
        assertEquals(map.getFirstValue("Foo"), "blub");
        assertEquals(map.getJoinedValue("Foo", ", "), "blub, bla");
        assertEquals(map.get("Foo"), Arrays.asList("blub", "bla"));
    }

    @Test
    public void replaceAllTest2() {
        FluentStringsMap map = new FluentStringsMap();

        map.add("foo", "bar");
        map.add("bar", "foo, bar", "baz");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "bar", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("bar"), "foo, bar");
        assertEquals(map.getJoinedValue("bar", ", "), "foo, bar, baz");
        assertEquals(map.get("bar"), Arrays.asList("foo, bar", "baz"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        LinkedHashMap<String, Collection<String>> newValues = new LinkedHashMap<>();

        newValues.put("bar", Arrays.asList("baz"));
        newValues.put("foo", null);
        map.replaceAll(newValues);

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("bar", "baz")));
        assertNull(map.getFirstValue("foo"));
        assertNull(map.getJoinedValue("foo", ", "));
        assertNull(map.get("foo"));
        assertEquals(map.getFirstValue("bar"), "baz");
        assertEquals(map.getJoinedValue("bar", ", "), "baz");
        assertEquals(map.get("bar"), Arrays.asList("baz"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
    }

    @Test
    public void replaceAllNullTest1() {
        FluentStringsMap map = new FluentStringsMap();

        map.add("foo", "bar");
        map.add("bar", "foo, bar", "baz");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "bar", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("bar"), "foo, bar");
        assertEquals(map.getJoinedValue("bar", ", "), "foo, bar, baz");
        assertEquals(map.get("bar"), Arrays.asList("foo, bar", "baz"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.replaceAll((FluentStringsMap) null);

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "bar", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("bar"), "foo, bar");
        assertEquals(map.getJoinedValue("bar", ", "), "foo, bar, baz");
        assertEquals(map.get("bar"), Arrays.asList("foo, bar", "baz"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
    }

    @Test
    public void replaceAllNullTest2() {
        FluentStringsMap map = new FluentStringsMap();

        map.add("foo", "bar");
        map.add("bar", "foo, bar", "baz");
        map.add("baz", Arrays.asList("foo", "bar"));

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "bar", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("bar"), "foo, bar");
        assertEquals(map.getJoinedValue("bar", ", "), "foo, bar, baz");
        assertEquals(map.get("bar"), Arrays.asList("foo, bar", "baz"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));

        map.replaceAll((Map<String, Collection<String>>) null);

        assertEquals(map.keySet(), new LinkedHashSet<>(Arrays.asList("foo", "bar", "baz")));
        assertEquals(map.getFirstValue("foo"), "bar");
        assertEquals(map.getJoinedValue("foo", ", "), "bar");
        assertEquals(map.get("foo"), Arrays.asList("bar"));
        assertEquals(map.getFirstValue("bar"), "foo, bar");
        assertEquals(map.getJoinedValue("bar", ", "), "foo, bar, baz");
        assertEquals(map.get("bar"), Arrays.asList("foo, bar", "baz"));
        assertEquals(map.getFirstValue("baz"), "foo");
        assertEquals(map.getJoinedValue("baz", ", "), "foo, bar");
        assertEquals(map.get("baz"), Arrays.asList("foo", "bar"));
    }
}
