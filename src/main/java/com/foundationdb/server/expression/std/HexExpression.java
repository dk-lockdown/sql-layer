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

package com.foundationdb.server.expression.std;

import com.foundationdb.server.error.WrongExpressionArityException;
import com.foundationdb.server.expression.Expression;
import com.foundationdb.server.expression.ExpressionComposer;
import com.foundationdb.server.expression.ExpressionEvaluation;
import com.foundationdb.server.expression.ExpressionType;
import com.foundationdb.server.expression.TypesList;
import com.foundationdb.server.service.functions.Scalar;
import com.foundationdb.server.types.AkType;
import com.foundationdb.server.types.NullValueSource;
import com.foundationdb.server.types.ValueSource;
import com.foundationdb.sql.StandardException;
import java.nio.charset.Charset;

public class HexExpression extends AbstractUnaryExpression
{
    @Scalar("hex")
    public static final ExpressionComposer COMPOSER = new UnaryComposer()
    {
        @Override
        protected Expression compose(Expression argument, ExpressionType argType, ExpressionType resultType)
        {
            return new HexExpression(argument);
        }

        @Override
        public ExpressionType composeType(TypesList argumentTypes) throws StandardException
        {
            if (argumentTypes.size() != 1)
                throw new WrongExpressionArityException(1, argumentTypes.size());
            if (ArithExpression.isNumeric(argumentTypes.get(0).getType()))
                argumentTypes.setType(0, AkType.LONG);
            else
                argumentTypes.setType(0, AkType.VARCHAR);
            return ExpressionTypes.varchar(argumentTypes.get(0).getPrecision() * 2);
        }
        
    };
    
    private static class InnerEvaluation extends AbstractUnaryExpressionEvaluation
    {
        private  Charset charset = Charset.defaultCharset(); // may change when actual charset
                                                          // becomes availabe to expressions
        
        public InnerEvaluation(ExpressionEvaluation eval)
        {
            super(eval);
        }

        @Override
        public ValueSource eval()
        {
            ValueSource source = operand();
            if (source.isNull())
                return NullValueSource.only();
            
            if (source.getConversionType() == AkType.LONG)
                valueHolder().putString(Long.toHexString(source.getLong()).toUpperCase());
            else
            {
                StringBuilder builder = new StringBuilder();
                for (byte ch : source.getString().getBytes(charset))
                    builder.append(String.format("%02X", ch));
                valueHolder().putString(builder.toString());
            }
            
            return valueHolder();
        }
        
    }

    @Override
    public String name()
    {
        return "HEX";
    }

    @Override
    public ExpressionEvaluation evaluation()
    {
        return new InnerEvaluation(operandEvaluation());
    }
    
    protected HexExpression (Expression arg)
    {
        super(AkType.VARCHAR, arg);
    }
}
