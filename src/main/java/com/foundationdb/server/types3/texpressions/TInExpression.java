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
package com.foundationdb.server.types3.texpressions;

import com.foundationdb.qp.operator.QueryContext;
import com.foundationdb.server.types3.LazyList;
import com.foundationdb.server.types3.TClass;
import com.foundationdb.server.types3.TComparison;
import com.foundationdb.server.types3.TExecutionContext;
import com.foundationdb.server.types3.TInstance;
import com.foundationdb.server.types3.TKeyComparable;
import com.foundationdb.server.types3.TOverloadResult;
import com.foundationdb.server.types3.aksql.aktypes.AkBool;
import com.foundationdb.server.types3.pvalue.PValueSource;
import com.foundationdb.server.types3.pvalue.PValueTarget;

import java.util.ArrayList;
import java.util.List;

public final class TInExpression {

    public static TPreparedExpression prepare(TPreparedExpression lhs, List<? extends TPreparedExpression> rhs,
                                              QueryContext queryContext) {
        return prepare(lhs, rhs, null, null, queryContext);
    }

    public static TPreparedExpression prepare(TPreparedExpression lhs, List<? extends TPreparedExpression> rhs,
                                              TInstance rhsInstance, TKeyComparable comparable,
                                              QueryContext queryContext) {
        List<TPreparedExpression> all = new ArrayList<>(rhs.size() + 1);
        boolean nullable = lhs.resultType().nullability();
        all.add(lhs);
        for (TPreparedExpression r : rhs) {
            all.add(r);
            nullable |= r.resultType().nullability();
        }
        TValidatedScalar overload;        
        if (comparable == null)
            overload = noKey;
        else {
            TInstance lhsInstance = lhs.resultType();
            boolean reverse;
            TClass leftIn = lhsInstance.typeClass();
            TClass rightIn = rhsInstance.typeClass();
            TClass leftCmp = comparable.getLeftTClass();
            TClass rightCmp = comparable.getRightTClass();
            if (leftIn == leftCmp && rightIn == rightCmp) {
                reverse = false;
            }
            else if (rightIn == leftCmp && leftIn == rightCmp) {
                reverse = true;
            }
            else {
                throw new IllegalArgumentException("invalid comparisons: " + lhsInstance + " and " + rhsInstance + " against " + comparable);
            }
            overload = new TValidatedScalar(reverse ?
                                            new InKeyReversedScalar(comparable.getComparison()) :
                                            new InKeyScalar(comparable.getComparison()));
        }
        return new TPreparedFunction(overload, AkBool.INSTANCE.instance(nullable), all, queryContext);
    }
    
    static abstract class InScalarBase extends TScalarBase {
        protected abstract int doCompare(TInstance lhsInstance, PValueSource lhsSource,
                                         TInstance rhsInstance, PValueSource rhsSource);

        @Override
        protected void buildInputSets(TInputSetBuilder builder) {
            builder.vararg(null, 0, 1);
        }

        @Override
        protected void doEvaluate(TExecutionContext context, LazyList<? extends PValueSource> inputs, PValueTarget output) {
            TInstance lhsInstance = context.inputTInstanceAt(0);
            PValueSource lhsSource = inputs.get(0);
            for (int i=1, nInputs = inputs.size(); i < nInputs; ++i) {
                TInstance rhsInstance = context.inputTInstanceAt(i);
                PValueSource rhsSource = inputs.get(i);
                if (0 == doCompare(lhsInstance, lhsSource, rhsInstance, rhsSource)) {
                    output.putBool(true);
                    return;
                }
            }
            output.putBool(false);
        }

        @Override
        public String displayName() {
            return "in";
        }

        @Override
        public TOverloadResult resultType() {
            return TOverloadResult.fixed(AkBool.INSTANCE);
        }

        @Override
        protected boolean nullContaminates(int inputIndex) {
            return (inputIndex == 0);
        }
    }

    private static final TValidatedScalar noKey = new TValidatedScalar(new InScalarBase() {
        @Override
        protected int doCompare(TInstance lhsInstance, PValueSource lhsSource,
                                TInstance rhsInstance, PValueSource rhsSource) {
            return TClass.compare(lhsInstance, lhsSource, rhsInstance, rhsSource);
        }
    });

    static class InKeyScalar extends InScalarBase {
        protected final TComparison comparison;

        InKeyScalar(TComparison comparison) {
            this.comparison = comparison;
        }
        
        @Override
        protected int doCompare(TInstance lhsInstance, PValueSource lhsSource,
                                TInstance rhsInstance, PValueSource rhsSource) {
            return comparison.compare(lhsInstance, lhsSource, rhsInstance, rhsSource);
        }
    }

    static class InKeyReversedScalar extends InKeyScalar {
        InKeyReversedScalar(TComparison comparison) {
            super(comparison);
        }
        
        @Override
        protected int doCompare(TInstance lhsInstance, PValueSource lhsSource,
                                TInstance rhsInstance, PValueSource rhsSource) {
            return comparison.compare(rhsInstance, rhsSource, lhsInstance, lhsSource);
        }
    }
}
