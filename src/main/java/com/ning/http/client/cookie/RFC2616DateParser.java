/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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

package com.ning.http.client.cookie;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A parser for <a href="http://tools.ietf.org/html/rfc2616#section-3.3">RFC2616
 * Date format</a>.
 * 
 * @author slandelle
 */
@SuppressWarnings("serial")
public class RFC2616DateParser extends SimpleDateFormat {

    private final SimpleDateFormat format1 = new RFC2616DateParserObsolete1();
    private final SimpleDateFormat format2 = new RFC2616DateParserObsolete2();

    private static final ThreadLocal<RFC2616DateParser> DATE_FORMAT_HOLDER = new ThreadLocal<RFC2616DateParser>() {
        @Override
        protected RFC2616DateParser initialValue() {
            return new RFC2616DateParser();
        }
    };

    public static RFC2616DateParser get() {
        return DATE_FORMAT_HOLDER.get();
    }

    /**
     * Standard date format<p>
     * Sun, 06 Nov 1994 08:49:37 GMT -> E, d MMM yyyy HH:mm:ss z
     */
    private RFC2616DateParser() {
        super("E, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
        setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    @Override
    public Date parse(String text, ParsePosition pos) {
        Date date = super.parse(text, pos);
        if (date == null) {
            date = format1.parse(text, pos);
        }
        if (date == null) {
            date = format2.parse(text, pos);
        }
        return date;
    }

    /**
     * First obsolete format<p>
     * Sunday, 06-Nov-94 08:49:37 GMT -> E, d-MMM-y HH:mm:ss z
     */
    private static final class RFC2616DateParserObsolete1 extends SimpleDateFormat {
        private static final long serialVersionUID = -3178072504225114298L;

        RFC2616DateParserObsolete1() {
            super("E, dd-MMM-yy HH:mm:ss z", Locale.ENGLISH);
            setTimeZone(TimeZone.getTimeZone("GMT"));
        }
    }

    /**
     * Second obsolete format
     * <p>
     * Sun Nov 6 08:49:37 1994 -> EEE, MMM d HH:mm:ss yyyy
     */
    private static final class RFC2616DateParserObsolete2 extends SimpleDateFormat {
        private static final long serialVersionUID = 3010674519968303714L;

        RFC2616DateParserObsolete2() {
            super("E MMM d HH:mm:ss yyyy", Locale.ENGLISH);
            setTimeZone(TimeZone.getTimeZone("GMT"));
        }
    }
}
