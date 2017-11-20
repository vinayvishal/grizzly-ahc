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

package com.ning.http.client.generators;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import org.testng.annotations.Test;

import com.ning.http.client.Body;

/**
 * @author Bryan Davis bpd@keynetics.com
 */
public class ByteArrayBodyGeneratorTest {

    private final Random random = new Random();
    private final int chunkSize = 1024 * 8;

    @Test(groups = "standalone")
    public void testSingleRead() throws IOException {
        final int srcArraySize = chunkSize - 1;
        final byte[] srcArray = new byte[srcArraySize];
        random.nextBytes(srcArray);

        final ByteArrayBodyGenerator babGen =
            new ByteArrayBodyGenerator(srcArray);
        final Body body = babGen.createBody();

        final ByteBuffer chunkBuffer = ByteBuffer.allocate(chunkSize);

        // should take 1 read to get through the srcArray
        assertEquals(body.read(chunkBuffer), srcArraySize);
        assertEquals(chunkBuffer.position(), srcArraySize, "bytes read");
        chunkBuffer.clear();

        assertEquals(body.read(chunkBuffer), -1, "body at EOF");
    }

    @Test(groups = "standalone")
    public void testMultipleReads() throws IOException {
        final int srcArraySize = (3 * chunkSize) + 42;
        final byte[] srcArray = new byte[srcArraySize];
        random.nextBytes(srcArray);

        final ByteArrayBodyGenerator babGen =
            new ByteArrayBodyGenerator(srcArray);
        final Body body = babGen.createBody();

        final ByteBuffer chunkBuffer = ByteBuffer.allocate(chunkSize);

        int reads = 0;
        int bytesRead = 0;
        while (body.read(chunkBuffer) != -1) {
          reads += 1;
          bytesRead += chunkBuffer.position();
          chunkBuffer.clear();
        }
        assertEquals(reads, 4, "reads to drain generator");
        assertEquals(bytesRead, srcArraySize, "bytes read");
    }

}
