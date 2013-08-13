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

package com.foundationdb.qp.operator;

import com.foundationdb.qp.row.HKey;
import com.foundationdb.qp.row.Row;
import com.foundationdb.server.error.*;
import com.foundationdb.server.types.AkType;
import com.foundationdb.server.types.ValueSource;
import com.foundationdb.server.types.conversion.Converters;
import com.foundationdb.server.types.util.ValueHolder;
import com.foundationdb.server.types3.pvalue.PValue;
import com.foundationdb.server.types3.pvalue.PValueSource;
import com.foundationdb.server.types3.pvalue.PValueTargets;
import com.foundationdb.util.BloomFilter;
import com.foundationdb.util.ShareHolder;
import com.foundationdb.util.SparseArray;

import java.math.BigDecimal;
import java.math.BigInteger;

public class SparseArrayQueryBindings implements QueryBindings
{
    private final SparseArray<Object> bindings = new SparseArray<>();
    private final QueryBindings parent;
    private final int depth;

    public SparseArrayQueryBindings() {
        this.parent = null;
        this.depth = 0;
    }

    public SparseArrayQueryBindings(QueryBindings parent) {
        this.parent = parent;
        this.depth = parent.getDepth() + 1;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(getClass().getSimpleName());
        str.append('(');
        bindings.describeElements(str);
        if (parent != null) {
            str.append(", ");
            str.append(parent);
        }
        str.append(')');
        return str.toString();
    }

    /* QueryBindings interface */

    @Override
    public PValueSource getPValue(int index) {
        if (bindings.isDefined(index)) {
            return (PValueSource)bindings.get(index);
        }
        else if (parent != null) {
            return parent.getPValue(index);
        }
        else {
            throw new BindingNotSetException(index);
        }
    }

    /*
     * (non-Javadoc)
     * @see com.akiban.qp.operator.QueryContext#setPValue(int, com.akiban.server.types3.pvalue.PValueSource)
     * This makes a copy of the PValueSource value, rather than simply
     * storing the reference. The assumption is the PValueSource parameter
     * will be reused by the caller as rows are processed, so the QueryContext
     * needs to keep a copy of the underlying value.
     *
     */
    @Override
    public void setPValue(int index, PValueSource value) {
        PValue holder = null;
        if (bindings.isDefined(index)) {
            holder = (PValue)bindings.get(index);
            if (holder.tInstance() != value.tInstance())
                holder = null;
        }
        if (holder == null) {
            holder = new PValue(value.tInstance());
            bindings.set(index, holder);
        }
        PValueTargets.copyFrom(value, holder);
    }
    
    @Override
    public ValueSource getValue(int index) {
        if (bindings.isDefined(index)) {
            return (ValueSource)bindings.get(index);
        }
        else if (parent != null) {
            return parent.getValue(index);
        }
        else {
            throw new BindingNotSetException(index);
        }
    }

    @Override
    public void setValue(int index, ValueSource value, AkType type)
    {
        ValueHolder holder;
        if (bindings.isDefined(index))
            holder = (ValueHolder)bindings.get(index);
        else {
            holder = new ValueHolder();
            bindings.set(index, holder);
        }
        
        holder.expectType(type);
        try
        {
            Converters.convert(value, holder);
        }
        catch (InvalidDateFormatException e)
        {
            errorCase(e, holder);
        }
        catch (InconvertibleTypesException e)
        {
            errorCase(e, holder);
        }
        catch (InvalidCharToNumException e)
        {
            errorCase(e, holder);
        }
    }
    
    private void errorCase (InvalidOperationException e, ValueHolder holder)
    {
        //warnClient(e);
        switch(holder.getConversionType())
        {
            case DECIMAL:   holder.putDecimal(BigDecimal.ZERO); break;
            case U_BIGINT:  holder.putUBigInt(BigInteger.ZERO); break;
            case LONG:
            case U_INT:
            case INT:        holder.putRaw(holder.getConversionType(), 0L); break;
            case U_DOUBLE:   
            case DOUBLE:     holder.putRaw(holder.getConversionType(), 0.0d);
            case U_FLOAT:
            case FLOAT:      holder.putRaw(holder.getConversionType(), 0.0f); break;
            case TIME:       holder.putTime(0L);
            default:         holder.putNull();

        }
    }

    @Override
    public void setValue(int index, ValueSource value)
    {
        setValue(index, value, value.getConversionType());
    }

    @Override
    public Row getRow(int index) {
        if (bindings.isDefined(index)) {
            return ((ShareHolder<Row>)bindings.get(index)).get();
        }
        else if (parent != null) {
            return parent.getRow(index);
        }
        else {
            throw new BindingNotSetException(index);
        }
    }

    @Override
    public void setRow(int index, Row row)
    {
        ShareHolder<Row> holder = null;
        if (bindings.isDefined(index)) {
            holder = (ShareHolder<Row>)bindings.get(index);
        }
        if (holder == null) {
            holder = new ShareHolder<Row>();
            bindings.set(index, holder);
        }
        holder.hold(row);
    }

    @Override
    public HKey getHKey(int index) {
        if (bindings.isDefined(index)) {
            return (HKey)bindings.get(index);
        }
        else if (parent != null) {
            return parent.getHKey(index);
        }
        else {
            throw new BindingNotSetException(index);
        }
    }

    @Override
    public void setHKey(int index, HKey hKey)
    {
        bindings.set(index, hKey);
    }

    @Override
    public BloomFilter getBloomFilter(int index) {
        if (bindings.isDefined(index)) {
            return (BloomFilter)bindings.get(index);
        }
        else if (parent != null) {
            return parent.getBloomFilter(index);
        }
        else {
            throw new BindingNotSetException(index);
        }
    }

    @Override
    public void setBloomFilter(int index, BloomFilter filter) {
        bindings.set(index, filter);
    }

    @Override
    public void clear() {
        bindings.clear();
    }

    @Override
    public QueryBindings getParent() {
        return parent;
    }

    @Override
    public boolean isAncestor(QueryBindings ancestor) {
        for (QueryBindings descendant = this; descendant != null; descendant = descendant.getParent()) {
            if (descendant == ancestor) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public QueryBindings createBindings() {
        return new SparseArrayQueryBindings(this);
    }
}
