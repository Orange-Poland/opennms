/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2019 The OpenNMS Group, Inc.
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

package org.opennms.core.ipc.rpc.kafka.server;

import java.util.Objects;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * This is a bean that consists of rpcId and expiration time.
 * Delay queue removes this bean when it reaches the delay specified.
 */
public class RpcId implements Delayed {
    private final long expirationTime;

    private final String rpcId;

    public RpcId(String rpcId, long expirationTime) {
        this.rpcId = rpcId;
        this.expirationTime = expirationTime;
    }

    @Override
    public int compareTo(Delayed other) {
        long myDelay = getDelay(TimeUnit.MILLISECONDS);
        long otherDelay = other.getDelay(TimeUnit.MILLISECONDS);
        return Long.compare(myDelay, otherDelay);
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long now = System.currentTimeMillis();
        return unit.convert(expirationTime - now, TimeUnit.MILLISECONDS);
    }

    public String getRpcId() {
        return rpcId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RpcId that = (RpcId) o;
        return expirationTime == that.expirationTime &&
                Objects.equals(rpcId, that.rpcId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expirationTime, rpcId);
    }
}
