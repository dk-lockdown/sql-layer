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

package com.akiban.sql.pg;

import com.akiban.qp.operator.ArrayBindings;
import com.akiban.qp.operator.Bindings;
import com.akiban.server.expression.EnvironmentExpressionSetting;
import com.akiban.server.service.dxl.DXLFunctionsHook;
import com.akiban.server.service.dxl.DXLReadWriteLockHook;
import com.akiban.server.service.session.Session;
import com.akiban.sql.server.ServerParameterDecoder;
import com.akiban.util.Tap;

import java.io.IOException;
import java.util.List;

/**
 * An ordinary SQL statement.
 */
public abstract class PostgresBaseStatement implements PostgresStatement
{
    private List<String> columnNames;
    private List<PostgresType> columnTypes;
    private PostgresType[] parameterTypes;
    private List<EnvironmentExpressionSetting> environmentSettings;

    protected PostgresBaseStatement(PostgresType[] parameterTypes,
                                    List<EnvironmentExpressionSetting> environmentSettings) {
        this.parameterTypes = parameterTypes;
        this.environmentSettings = environmentSettings;
    }

    protected PostgresBaseStatement(List<String> columnNames, 
                                    List<PostgresType> columnTypes,
                                    PostgresType[] parameterTypes,
                                    List<EnvironmentExpressionSetting> environmentSettings) {
        this.columnNames = columnNames;
        this.columnTypes = columnTypes;
        this.parameterTypes = parameterTypes;
        this.environmentSettings = environmentSettings;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public List<PostgresType> getColumnTypes() {
        return columnTypes;
    }

    public boolean isColumnBinary(int i) {
        return false;
    }

    public PostgresType[] getParameterTypes() {
        return parameterTypes;
    }

    public List<EnvironmentExpressionSetting> getEnvironmentSettings() {
        return environmentSettings;
    }

    public void sendDescription(PostgresServerSession server, boolean always) 
            throws IOException {
        PostgresMessenger messenger = server.getMessenger();
        List<PostgresType> columnTypes = getColumnTypes();
        if (columnTypes == null) {
            if (!always) return;
            messenger.beginMessage(PostgresMessages.NO_DATA_TYPE.code());
        }
        else {
            messenger.beginMessage(PostgresMessages.ROW_DESCRIPTION_TYPE.code());
            List<String> columnNames = getColumnNames();
            int ncols = columnTypes.size();
            messenger.writeShort(ncols);
            for (int i = 0; i < ncols; i++) {
                PostgresType type = columnTypes.get(i);
                messenger.writeString(columnNames.get(i)); // attname
                messenger.writeInt(0);    // attrelid
                messenger.writeShort(0);  // attnum
                messenger.writeInt(type.getOid()); // atttypid
                messenger.writeShort(type.getLength()); // attlen
                messenger.writeInt(type.getModifier()); // atttypmod
                messenger.writeShort(isColumnBinary(i) ? 1 : 0);
            }
        }
        messenger.sendMessage();
    }

    protected Bindings getBindings() {
        return new ArrayBindings(0);
    }

    protected int getNParameters() {
        if (parameterTypes == null)
            return 0;
        else
            return parameterTypes.length;
    }

    protected Bindings getParameterBindings(Object[] parameters) {
        ServerParameterDecoder decoder = new ServerParameterDecoder();
        ArrayBindings bindings = new ArrayBindings(parameters.length);
        for (int i = 0; i < parameters.length; i++) {
            PostgresType pgType = (parameterTypes == null) ? null : parameterTypes[i];
            bindings.set(i, decoder.decodeParameter(parameters[i], pgType));
        }
        return bindings;
    }

    protected void setEnvironmentBindings(PostgresServerSession session, 
                                          Bindings bindings) {
        if (environmentSettings != null) {
            int position = getNParameters();
            for (EnvironmentExpressionSetting environmentSetting : environmentSettings) {
                bindings.set(position++, session.getEnvironmentValue(environmentSetting));
            }
        }
    }

    protected abstract Tap.InOutTap executeTap();
    protected abstract Tap.InOutTap acquireLockTap();

    protected void lock(Session session, DXLFunctionsHook.DXLFunction operationType)
    {
        acquireLockTap().in();
        executeTap().in();
        DXLReadWriteLockHook.only().hookFunctionIn(session, operationType);
        acquireLockTap().out();
    }

    protected void unlock(Session session, DXLFunctionsHook.DXLFunction operationType)
    {
        DXLReadWriteLockHook.only().hookFunctionFinally(session, operationType, null);
        executeTap().out();
    }
}
