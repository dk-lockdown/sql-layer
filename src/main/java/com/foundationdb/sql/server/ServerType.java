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

package com.foundationdb.sql.server;

import com.foundationdb.server.types.AkType;
import com.foundationdb.server.types3.TInstance;

/** A type according to the server's regime.
 */
public abstract class ServerType
{
    public enum BinaryEncoding {
        NONE, INT_8, INT_16, INT_32, INT_64, FLOAT_32, FLOAT_64, STRING_BYTES,
        BINARY_OCTAL_TEXT, BOOLEAN_C, 
        TIMESTAMP_FLOAT64_SECS_2000_NOTZ, TIMESTAMP_INT64_MICROS_2000_NOTZ,
        DECIMAL_PG_NUMERIC_VAR
    }

    private AkType akType;
    private TInstance instance;

    protected ServerType(AkType akType, TInstance instance) {
        this.akType = akType;
        this.instance = instance;
    }

    public AkType getAkType() {
        return akType;
    }
    
    public TInstance getInstance() {
        return instance;
    }

    public BinaryEncoding getBinaryEncoding() {
        return BinaryEncoding.NONE;
    }

    @Override
    public String toString() {
        return String.valueOf(akType);
    }

}
