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

import com.foundationdb.junit.NamedParameterizedRunner;
import com.foundationdb.junit.NamedParameterizedRunner.TestParameters;
import com.foundationdb.junit.Parameterization;
import com.foundationdb.junit.ParameterizationBuilder;
import com.foundationdb.server.expression.Expression;
import com.foundationdb.server.expression.ExpressionComposer;
import com.foundationdb.server.types.AkType;
import com.foundationdb.server.types.ValueSource;

import java.util.Collection;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(NamedParameterizedRunner.class)
public class PowExpressionTest extends ComposedExpressionTestBase
{
    private static boolean alreadyExc = false;
    
    private Double base;
    private Double exponent;
    private Double expected;
    
    public PowExpressionTest (Double a, Double n, Double exp)
    {
        base = a;
        exponent = n;
        expected = exp;
    }
    
    @TestParameters
    public static Collection<Parameterization> params()
    {
        ParameterizationBuilder pb = new ParameterizationBuilder();
        
        // test regular cases
        param(pb, 2d, 2d, 4d);
        param(pb, 0.5d, 2d, 0.25d);
        
        // test NULLs
        param(pb, null, 2d, null);
        param(pb, 4d, null, null);
        param(pb, null, null, null);
        
        // test infinity
        param(pb, Double.POSITIVE_INFINITY, 2d, Double.POSITIVE_INFINITY);
        param(pb, Double.NEGATIVE_INFINITY, 3d, Double.NEGATIVE_INFINITY);
        param(pb, Double.NEGATIVE_INFINITY, 4d, Double.POSITIVE_INFINITY);
        param(pb, 1d, (double)Double.MAX_EXPONENT, 1d);
        
        // test NaN
        param(pb, Double.NaN, 5d, Double.NaN);
        param(pb, 1d, Double.NaN, Double.NaN);
        param(pb, Double.NaN, Double.NaN, Double.NaN);
        
        // test negative exponent
        param(pb, -5d, -1d, -0.2);
        param(pb, 25d, -0.5d, 0.2);
        
        // test exponents in (0, 1)
        param(pb, 144d, 0.5, 12d);
        param(pb, 32.0, 0.2, 2d);
        
        return pb.asList();
    }
    
    private static void param(ParameterizationBuilder pb, 
                              Double base, Double exponent, Double expected)
    {
        pb.add("POW(" + base + ", " + exponent + ") ", base, exponent, expected);
    }
    
    
    @Test
    public void test()
    {
        Expression left = base == null
                            ? LiteralExpression.forNull()
                            : new LiteralExpression(AkType.DOUBLE, base.doubleValue());
        
        Expression right = exponent == null
                            ? LiteralExpression.forNull()
                            : new LiteralExpression(AkType.DOUBLE, exponent.doubleValue());
        
        ValueSource top = new PowExpression(left, right).evaluation().eval();
        if (expected == null)
            assertTrue("Top should be NULL ", top.isNull());
        else
            assertEquals(expected.doubleValue(), top.getDouble(), 0.00001);
    }
    
    @Override
    protected CompositionTestInfo getTestInfo()
    {
        return new CompositionTestInfo(2, AkType.DOUBLE, true);
    }

    @Override
    protected ExpressionComposer getComposer()
    {
        return PowExpression.POW;
    }

    @Override
    public boolean alreadyExc()
    {
        return alreadyExc;
    }
}
