/**
 * Copyright (C) 2011 Akiban Technologies Inc.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package com.akiban.cserver.service.jmx;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import com.akiban.util.ArgumentValidation;

public final class MBeanServerProxy {

    public interface MockMBeanServer extends MBeanServer {
        Set<ObjectName> getRegisteredObjectNames();
    }

    public static MockMBeanServer getMock() {
        return (MockMBeanServer) Proxy.newProxyInstance(
                MockMBeanServer.class.getClassLoader(),
                new Class[] {MockMBeanServer.class},
                new MBeanServerHandler()
        );
    }

    private static class MBeanServerHandler implements InvocationHandler {

        private final Map<ObjectName,Object> objects = new HashMap<ObjectName,Object>();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("registerMBean".equals(method.getName())) {
                ArgumentValidation.arrayLength("args", args, 2);
                ArgumentValidation.notNull("object", args[0]);
                ArgumentValidation.notNull("objectName", args[1]);
                objects.put((ObjectName)args[1], args[0]);
                return new ObjectInstance((ObjectName)args[1], args[0].getClass().getCanonicalName());
            }
            else if ("unregisterMBean".equals(method.getName())) {
                ArgumentValidation.arrayLength("args", args, 1);
                ArgumentValidation.notNull("objectName", args[0]);
                Object prev = objects.remove(args[0]);
                if (prev == null) {
                    throw new InstanceNotFoundException(args[0].toString());
                }
                return null;
            }
            else if ("getRegisteredObjectNames".equals(method.getName())) {
                ArgumentValidation.isNull("args", args);
                return Collections.unmodifiableSet(objects.keySet());
            }
            else {
                throw new UnsupportedOperationException(method.toString());
            }
        }
    }
}
