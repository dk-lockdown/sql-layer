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

package com.foundationdb.server.types3.mcompat.mtypes;

import com.foundationdb.server.collation.AkCollator;
import com.foundationdb.server.collation.AkCollatorFactory;
import com.foundationdb.server.types3.PValueIO;
import com.foundationdb.server.types3.TClass;
import com.foundationdb.server.types3.TExecutionContext;
import com.foundationdb.server.types3.TInstance;
import com.foundationdb.server.types3.common.types.StringAttribute;
import com.foundationdb.server.types3.common.types.StringFactory;
import com.foundationdb.server.types3.common.types.TString;
import com.foundationdb.server.types3.mcompat.MBundle;
import com.foundationdb.server.types3.pvalue.PValueSource;
import com.foundationdb.server.types3.pvalue.PValueTarget;
import com.foundationdb.sql.types.TypeId;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class MString extends TString
{

    public static TInstance varcharFor(String string) {
        return string == null
                ? MString.VARCHAR.instance(0, true)
                : MString.VARCHAR.instance(string.length(), false);
    }
    
    public static TInstance varchar() {
        return MString.VARCHAR.instance(true);
    }

    public static final MString CHAR = new MString(TypeId.CHAR_ID, "char");
    public static final MString VARCHAR = new MString(TypeId.VARCHAR_ID, "varchar");
    public static final MString TINYTEXT = new MString(TypeId.LONGVARCHAR_ID, "tinytext", 256);
    
    public static final MString MEDIUMTEXT = new MString(TypeId.LONGVARCHAR_ID, "mediumtext", 65535);
    public static final MString TEXT = new MString(TypeId.LONGVARCHAR_ID, "text", 16777215);
    public static final MString LONGTEXT = new MString(TypeId.LONGVARCHAR_ID, "longtext", Integer.MAX_VALUE); // TODO not big enough!

    @Override
    protected PValueIO getPValueIO() {
        return pvalueIO;
    }

    @Override
    public void selfCast(TExecutionContext context, TInstance sourceInstance, PValueSource source,
                         TInstance targetInstance, PValueTarget target) {
        int maxTargetLen = targetInstance.attribute(StringAttribute.MAX_LENGTH);
        String sourceString = source.getString();
        if (sourceString.length() > maxTargetLen) {
            String truncated = sourceString.substring(0, maxTargetLen);
            context.reportTruncate(sourceString, truncated);
            sourceString = truncated;
        }
        target.putString(sourceString, null);
    }

    private MString(TypeId typeId, String name, int fixedSize) {
        super(typeId, MBundle.INSTANCE, name, -1, fixedSize);
    }
    
    private MString(TypeId typeId, String name)
    {       
        super(typeId, MBundle.INSTANCE, name, -1);
    }

    @Override
    public boolean compatibleForCompare(TClass other) {
        return super.compatibleForCompare(other) ||
            ((this == CHAR) && (other == VARCHAR)) ||
            ((this == VARCHAR) && (other == CHAR));
    }

    // Generally, text fields are rarely used in comparisons other than 'EQUAL'
    // Hence, it is not necessary to widen the width to capture the widest type
    // If the input does not fit into *this* width, it is definitely not going to be equal
    // Might need to flag 'TRUNCATION' as an error, not warning
    public TClass widestComparable()
    {
        return this;
    }
    
    @Override
    public void fromObject(TExecutionContext context, PValueSource in, PValueTarget out)
    {
        if (in.isNull()) {
            out.putNull();
            return;
        }
        int expectedLen = context.outputTInstance().attribute(StringAttribute.MAX_LENGTH);
        int charsetId = context.outputTInstance().attribute(StringAttribute.CHARSET);
        int collatorId = context.outputTInstance().attribute(StringAttribute.COLLATION);

        switch (TInstance.pUnderlying(in.tInstance()))
        {
            case STRING:
                String inStr = in.getString();
                String ret;
                if (inStr.length() > expectedLen)
                {
                    ret = inStr.substring(0, expectedLen);
                    context.reportTruncate(inStr, ret);
                }
                else
                    ret = inStr;
                out.putString(ret, AkCollatorFactory.getAkCollator(collatorId));
                break;
                
            case BYTES:
                byte bytes[] = in.getBytes();
                byte truncated[];

                if (bytes.length > expectedLen)
                {
                    truncated = Arrays.copyOf(bytes, expectedLen);
                    context.reportTruncate("BYTES string of length " + bytes.length,
                                           "BYTES string of length " + expectedLen);
                }
                else
                    truncated = bytes;
                
                try 
                {
                     out.putString(new String(truncated,
                                              StringFactory.Charset.of(charsetId))
                                  , AkCollatorFactory.getAkCollator(collatorId));
                }
                catch (UnsupportedEncodingException e)
                {
                    context.reportBadValue(e.getMessage());
                }
                break;
            default:
                throw new IllegalArgumentException("Unexpected UnderlyingType: " + in.tInstance());
        }
    }

    private static final PValueIO pvalueIO = new PValueIO() {
        @Override
        public void copyCanonical(PValueSource in, TInstance typeInstance, PValueTarget out) {
            out.putString(in.getString(), null);
        }

        @Override
        public void writeCollating(PValueSource inValue, TInstance inInstance, PValueTarget out) {
            final int collatorId = inInstance.attribute(StringAttribute.COLLATION);
            out.putString(AkCollator.getString(inValue), AkCollatorFactory.getAkCollator(collatorId));
        }

        @Override
        public void readCollating(PValueSource in, TInstance typeInstance, PValueTarget out) {
            if (in.canGetRawValue())
                out.putString(in.getString(), null);
            else if (in.hasCacheValue())
                out.putObject(in.getObject());
            else
                throw new AssertionError("no value");
        }
    };
}
