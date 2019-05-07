/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2019-2019 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2019 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.graph.rest.impl;

import static org.opennms.netmgt.graph.rest.impl.GraphRestServiceImpl.parseContentType;

import javax.ws.rs.core.MediaType;

import org.junit.Assert;
import org.junit.Test;

public class GraphRestServiceImplTest {

    @Test
    public void verifyParseSupportedContentType() {
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, parseContentType("application/json"));
        Assert.assertEquals(MediaType.APPLICATION_XML_TYPE, parseContentType("application/xml"));
    }

    @Test
    public void verifyParseMultiValueSupportedContentType() {
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, parseContentType("text/html,application/json"));
    }

    @Test
    public void verifyParseNotSupportedContentType() {
        Assert.assertNull(parseContentType("text/html"));
        Assert.assertNull(parseContentType(""));
        Assert.assertNull(parseContentType(null));
    }

    @Test
    public void verifyUseJsonIfWildcard() {
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, parseContentType("*"));
        Assert.assertEquals(MediaType.APPLICATION_JSON_TYPE, parseContentType("*/*"));
    }
}