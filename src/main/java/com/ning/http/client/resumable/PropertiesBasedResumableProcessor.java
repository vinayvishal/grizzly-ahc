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

package com.ning.http.client.resumable;

import static java.nio.charset.StandardCharsets.*;
import static com.ning.http.util.MiscUtils.closeSilently;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link com.ning.http.client.resumable.ResumableAsyncHandler.ResumableProcessor} which use a properties file
 * to store the download index information.
 */
public class PropertiesBasedResumableProcessor implements ResumableAsyncHandler.ResumableProcessor {
    private final static Logger log = LoggerFactory.getLogger(PropertiesBasedResumableProcessor.class);
    private final static File TMP = new File(System.getProperty("java.io.tmpdir"), "ahc");
    private final static String storeName = "ResumableAsyncHandler.properties";
    private final ConcurrentHashMap<String, Long> properties = new ConcurrentHashMap<>();

    @Override
    public void put(String url, long transferredBytes) {
        properties.put(url, transferredBytes);
    }

    @Override
    public void remove(String uri) {
        if (uri != null) {
            properties.remove(uri);
        }
    }

    @Override
    public void save(Map<String, Long> map) {
        log.debug("Saving current download state {}", properties.toString());
        FileOutputStream os = null;
        try {

            if (!TMP.exists() && !TMP.mkdirs()) {
                throw new IllegalStateException("Unable to create directory: " + TMP.getAbsolutePath());
            }
            File f = new File(TMP, storeName);
            if (!f.exists() && !f.createNewFile()) {
                throw new IllegalStateException("Unable to create temp file: " + f.getAbsolutePath());
            }
            if (!f.canWrite()) {
                throw new IllegalStateException();
            }

            os = new FileOutputStream(f);

            for (Map.Entry<String, Long> e : properties.entrySet()) {
                os.write((append(e)).getBytes(UTF_8));
            }
            os.flush();
        } catch (Throwable e) {
            log.warn(e.getMessage(), e);
        } finally {
            if (os != null)
                closeSilently(os);
        }
    }

    private static String append(Map.Entry<String, Long> e) {
        return new StringBuilder(e.getKey()).append('=').append(e.getValue()).append('\n').toString();
    }

    @Override
    public Map<String, Long> load() {
        Scanner scan = null;
        try {
            scan = new Scanner(new File(TMP, storeName), UTF_8.name());
            scan.useDelimiter("[=\n]");

            String key;
            String value;
            while (scan.hasNext()) {
                key = scan.next().trim();
                value = scan.next().trim();
                properties.put(key, Long.valueOf(value));
            }
            log.debug("Loading previous download state {}", properties.toString());
        } catch (FileNotFoundException ex) {
            log.debug("Missing {}", storeName);
        } catch (Throwable ex) {
            // Survive any exceptions
            log.warn(ex.getMessage(), ex);
        } finally {
            if (scan != null)
                scan.close();
        }
        return properties;
    }
}
