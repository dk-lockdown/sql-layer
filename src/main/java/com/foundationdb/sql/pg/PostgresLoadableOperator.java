/**
 * Copyright (C) 2009-2013 Akiban Technologies, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.foundationdb.sql.pg;

import com.foundationdb.sql.server.ServerCallContextStack;
import com.foundationdb.sql.server.ServerCallInvocation;

import com.foundationdb.qp.loadableplan.LoadableOperator;
import com.foundationdb.qp.operator.QueryBindings;
import com.foundationdb.util.tap.InOutTap;
import com.foundationdb.util.tap.Tap;

import java.io.IOException;
import java.util.List;

public class PostgresLoadableOperator extends PostgresOperatorStatement
{
    private static final InOutTap EXECUTE_TAP = Tap.createTimer("PostgresLoadableOperator: execute shared");
    private static final InOutTap ACQUIRE_LOCK_TAP = Tap.createTimer("PostgresLoadableOperator: acquire shared lock");

    private ServerCallInvocation invocation;

    protected PostgresLoadableOperator(LoadableOperator loadableOperator, 
                                       ServerCallInvocation invocation,
                                       List<String> columnNames, List<PostgresType> columnTypes, 
                                       PostgresType[] parameterTypes,
                                       boolean usesPValues)
    {
        super(null);
        super.init(loadableOperator.plan(), null, columnNames, columnTypes, parameterTypes, null, usesPValues);
        this.invocation = invocation;
    }
    
    @Override
    protected InOutTap executeTap()
    {
        return EXECUTE_TAP;
    }

    @Override
    protected InOutTap acquireLockTap()
    {
        return ACQUIRE_LOCK_TAP;
    }

    @Override
    public int execute(PostgresQueryContext context, QueryBindings bindings, int maxrows) throws IOException {
        bindings = PostgresLoadablePlan.setParameters(bindings, invocation, usesPValues());
        ServerCallContextStack.push(context, invocation);
        try {
            return super.execute(context, bindings, maxrows);
        }
        finally {
            ServerCallContextStack.pop(context, invocation);
        }
    }

}
