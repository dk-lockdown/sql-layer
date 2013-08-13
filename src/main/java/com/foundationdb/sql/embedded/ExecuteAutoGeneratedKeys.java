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

package com.foundationdb.sql.embedded;

import com.foundationdb.ais.model.Column;
import com.foundationdb.ais.model.UserTable;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

abstract class ExecuteAutoGeneratedKeys
{
    public abstract List<Column> getTargetColumns(UserTable targetTable);

    static ExecuteAutoGeneratedKeys of(int autoGeneratedKeys) {
        switch (autoGeneratedKeys) {
        case Statement.NO_GENERATED_KEYS:
            return null;
        case Statement.RETURN_GENERATED_KEYS:
            return new ExecuteAutoGeneratedKeys() {
                    @Override
                    public List<Column> getTargetColumns(UserTable targetTable) {
                        Column identityColumn = targetTable.getIdentityColumn();
                        if (identityColumn == null)
                            return Collections.emptyList();
                        else
                            return Collections.singletonList(identityColumn);
                    }
                };
        default:
            throw new IllegalArgumentException("Invalid autoGeneratedKeys: " + autoGeneratedKeys);
        }
    }

    static ExecuteAutoGeneratedKeys of(final int[] columnIndexes) {
        return new ExecuteAutoGeneratedKeys() {
                @Override
                public List<Column> getTargetColumns(UserTable targetTable) {
                    List<Column> result = new ArrayList<>();
                    for (int i = 0; i < columnIndexes.length; i++) {
                        int columnIndex = columnIndexes[i];
                        if ((columnIndex < 1) || (columnIndex > targetTable.getColumns().size())) {
                            throw JDBCException.wrapped("Invalid column index: " + columnIndex);
                        }
                        result.add(targetTable.getColumns().get(columnIndex - 1));
                    }
                    return result;
                }
            };
    }

    static ExecuteAutoGeneratedKeys of(final String[] columnNames) {
        return new ExecuteAutoGeneratedKeys() {
                @Override
                public List<Column> getTargetColumns(UserTable targetTable) {
                    List<Column> result = new ArrayList<>();
                    for (int i = 0; i < columnNames.length; i++) {
                        String columnName = columnNames[i];
                        Column column = targetTable.getColumn(columnName);
                        if (column == null) {
                            throw JDBCException.wrapped("Invalid column name: " + columnName);
                        }
                        result.add(column);
                    }
                    return result;
                }
            };
    }

}
