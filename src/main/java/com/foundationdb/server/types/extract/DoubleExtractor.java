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

package com.foundationdb.server.types.extract;

import com.foundationdb.server.error.InvalidCharToNumException;
import com.foundationdb.server.error.OverflowException;
import com.foundationdb.server.types.AkType;
import com.foundationdb.server.types.ValueSource;
import com.foundationdb.server.types.ValueSourceIsNullException;

public final class DoubleExtractor extends AbstractExtractor {

    public double getDouble(ValueSource source) {
        if (source.isNull())
            throw new ValueSourceIsNullException();

        AkType type = source.getConversionType();
        switch (type) {
        case DECIMAL:   return getDecimalAsDouble(source);
        case DOUBLE:    return source.getDouble();
        case FLOAT:     return source.getFloat();
        case INT:       return source.getInt();
        case LONG:      return source.getLong();
        case VARCHAR:   return getDouble(source.getString());
        case TEXT:      return getDouble(source.getText());
        case U_BIGINT:  return getBigIntAsDouble(source);
        case U_DOUBLE:  return source.getUDouble();
        case U_FLOAT:   return source.getUFloat();
        case U_INT:     return source.getUInt();
        case INTERVAL_MILLIS:  return source.getInterval_Millis();
        case INTERVAL_MONTH:   return source.getInterval_Month();
        case DATE:      return source.getDate();
        case DATETIME:  return source.getDateTime();
        case TIME:      return source.getTime();
        case TIMESTAMP: return source.getTimestamp();
        case YEAR:      return source.getYear();
        default:
            throw unsupportedConversion(type);
        }                
    }

    private double getDecimalAsDouble (ValueSource source )
    {
        double d = source.getDecimal().doubleValue();
        if (Double.isInfinite(d))
            throw new OverflowException();        
        else
            return d;
    }

    private double getBigIntAsDouble (ValueSource source)
    {
        double d = source.getUBigInt().doubleValue();
        if (Double.isInfinite(d))
            throw new OverflowException();        
        else
            return d;
    }

    public double getDouble(String string) {
        try
        {
            return Double.parseDouble(string);
        }
        catch (NumberFormatException e)
        {
            throw new InvalidCharToNumException(e.getMessage());
        }
    }

    public String asString(double value) {
        return Double.toString(value);
    }

    // package-private ctor
    DoubleExtractor() {
        super(AkType.DOUBLE);
    }
}
