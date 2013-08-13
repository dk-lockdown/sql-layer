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

import com.foundationdb.server.error.ExternalRoutineInvocationException;
import com.foundationdb.server.error.SQLParserInternalException;
import com.foundationdb.server.types.AkType;
import com.foundationdb.server.types3.TInstance;
import com.foundationdb.sql.StandardException;
import com.foundationdb.sql.optimizer.TypesTranslation;
import com.foundationdb.sql.types.DataTypeDescriptor;
import com.foundationdb.sql.types.TypeId;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PostgresDynamicResultSetOutputter extends PostgresOutputter<ResultSet>
{
    private int ncols;
    private PostgresType[] columnTypes;
    private String[] columnNames;

    public PostgresDynamicResultSetOutputter(PostgresQueryContext context,
                                             PostgresJavaRoutine statement) {
        super(context, statement);
    }

    public void setMetaData(ResultSetMetaData metaData) throws SQLException {
        ncols = metaData.getColumnCount();
        columnTypes = new PostgresType[ncols];
        columnNames = new String[ncols];
        for (int i = 0; i < ncols; i++) {
            columnTypes[i] = typeFromSQL(metaData, i+1);
            columnNames[i] = metaData.getColumnName(i+1);
        }
    }

    public void sendDescription() throws IOException {
        messenger.beginMessage(PostgresMessages.ROW_DESCRIPTION_TYPE.code());
        messenger.writeShort(ncols);
        for (int i = 0; i < columnTypes.length; i++) {
            PostgresType type = columnTypes[i];
            messenger.writeString(columnNames[i]); // attname
            messenger.writeInt(0);    // attrelid
            messenger.writeShort(0);  // attnum
            messenger.writeInt(type.getOid()); // atttypid
            messenger.writeShort(type.getLength()); // attlen
            messenger.writeInt(type.getModifier()); // atttypmod
            messenger.writeShort(0);
        }
        messenger.sendMessage();
    }

    @Override
    public void output(ResultSet resultSet, boolean usePVals) throws IOException {
        messenger.beginMessage(PostgresMessages.DATA_ROW_TYPE.code());
        messenger.writeShort(ncols);
        for (int i = 0; i < ncols; i++) {
            Object column;
            try {
                column = resultSet.getObject(i+1);
            }
            catch (SQLException ex) {
                throw new ExternalRoutineInvocationException(((PostgresJavaRoutine)statement).getInvocation().getRoutineName(), ex);
            }
            PostgresType type = columnTypes[i];
            boolean binary = false;
            ByteArrayOutputStream bytes;
            if (usePVals) bytes = encoder.encodePObject(column, type, binary);
            else bytes = encoder.encodeObject(column, type, binary);
            if (bytes == null) {
                messenger.writeInt(-1);
            }
            else {
                messenger.writeInt(bytes.size());
                messenger.writeByteStream(bytes);
            }
        }
        messenger.sendMessage();
    }

    protected static PostgresType typeFromSQL(ResultSetMetaData metaData, int columnIndex) throws SQLException {
        TypeId typeId = TypeId.getBuiltInTypeId(metaData.getColumnType(columnIndex));
        if (typeId == null) {
            try {
                typeId = TypeId.getUserDefinedTypeId(metaData.getColumnTypeName(columnIndex),
                                                     false);
            }
            catch (StandardException ex) {
                throw new SQLParserInternalException(ex);
            }
        }
        DataTypeDescriptor sqlType;
        if (typeId.isDecimalTypeId() || typeId.isNumericTypeId()) {
            sqlType = new DataTypeDescriptor(typeId,
                                             metaData.getPrecision(columnIndex),
                                             metaData.getScale(columnIndex),
                                             metaData.isNullable(columnIndex) != ResultSetMetaData.columnNoNulls,
                                             metaData.getColumnDisplaySize(columnIndex));
        }
        else {
            sqlType = new DataTypeDescriptor(typeId,
                                             metaData.isNullable(columnIndex) != ResultSetMetaData.columnNoNulls,
                                             metaData.getColumnDisplaySize(columnIndex));
        }
        AkType akType = TypesTranslation.sqlTypeToAkType(sqlType);
        TInstance tInstance = TypesTranslation.toTInstance(sqlType);
        return PostgresType.fromDerby(sqlType, akType, tInstance);
    }

}
