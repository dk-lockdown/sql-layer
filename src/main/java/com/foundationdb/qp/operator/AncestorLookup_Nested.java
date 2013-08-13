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

import com.foundationdb.ais.model.Group;
import com.foundationdb.ais.model.UserTable;
import com.foundationdb.qp.row.HKey;
import com.foundationdb.qp.row.Row;
import com.foundationdb.qp.rowtype.HKeyRowType;
import com.foundationdb.qp.rowtype.IndexRowType;
import com.foundationdb.qp.rowtype.RowType;
import com.foundationdb.qp.rowtype.Schema;
import com.foundationdb.qp.rowtype.UserTableRowType;
import com.foundationdb.server.api.dml.ColumnSelector;
import com.foundationdb.server.explain.*;
import com.foundationdb.server.explain.std.LookUpOperatorExplainer;
import com.foundationdb.util.ArgumentValidation;
import com.foundationdb.util.tap.InOutTap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**

 <h1>Overview</h1>

 AncestorLookup_Nested locates ancestors of both group rows and index rows. Ancestors are located 
 using the hkey in a row of the current query's QueryContext.

 One expected usage is to locate the group row corresponding to an
 index row. For example, an index on customer.name yields index rows
 which AncestorLookup_Nested can then use to locate customer
 rows. (The ancestor relationship is reflexive, e.g. customer is
 considered to be an ancestor of customer.)

 Another expected usage is to locate ancestors higher in the group. For
 example, given either an item row or an item index row,
 AncestorLookup_Nested can be used to find the corresponding order and
 customer.

 AncestorLookup_Nested always locates 0-1 row per ancestor type.

 <h1>Arguments</h1>

 <ul>

 <li><b>GroupTable groupTable:</b> The group table containing the
 ancestors of interest.

 <li><b>RowType rowType:</b> Ancestors will be located for input rows
 of this type.

 <li><b>List<RowType> ancestorTypes:</b> Ancestor types to be located.

 <li><b>int inputBindingPosition:</b> Indicates input row's position in the query context. The hkey
 of this row will be used to locate ancestors.

 <li><b>int lookaheadQuantum:</b> Number of cursors to try to keep open by looking
  ahead in bindings stream.

 </ul>

 rowType may be an index row type or a group row type. For a group row
 type, rowType must not be one of the ancestorTypes. For an index row
 type, rowType may be one of the ancestorTypes.

 The groupTable, rowType, and all ancestorTypes must belong to the same
 group.

 Each ancestorType must be an ancestor of the rowType (or, if rowType
 is an index type, then an ancestor of the index's table's type).

 <h1>Behavior</h1>
 
 When this operator's cursor is opened, the row at position inputBindingPosition in the
 query context is accessed. The hkey from this row is obtained. For each ancestor type, the
 hkey is shortened if necessary, and the groupTable is then searched for
 a record with that exact hkey. All the retrieved records are written
 to the output stream in hkey order (ancestors before descendents), as
 is the input row if keepInput is true.

 <h1>Output</h1>

 Nothing else to say.

 <h1>Assumptions</h1>

 None.

 <h1>Performance</h1>

 For each input row, AncestorLookup_Nested does one random access for
 each ancestor type.

 <h1>Memory Requirements</h1>

 AncestorLookup_Nested stores in memory up to (ancestorTypes.size() +
 1) rows.

 */

class AncestorLookup_Nested extends Operator
{
    // Object interface

    @Override
    public String toString()
    {
        return String.format("%s(%s -> %s)", getClass().getSimpleName(), rowType, ancestors);
    }

    // Operator interface

    @Override
    public void findDerivedTypes(Set<RowType> derivedTypes)
    {
    }

    @Override
    protected Cursor cursor(QueryContext context, QueryBindingsCursor bindingsCursor)
    {
        if (lookaheadQuantum <= 1) {
            return new Execution(context, bindingsCursor);
        }
        else {
            // Convert to number of AncestorCursors, which hold multiple underlying
            // cursors, rounding up.
            int ncursors = ancestors.size();
            int quantum = (lookaheadQuantum + ncursors - 1) / ncursors;
            return new LookaheadExecution(context, bindingsCursor, 
                                          context.getStore(ancestors.get(0)),
                                          quantum);
        }
    }

    @Override
    public String describePlan()
    {
        return toString();
    }

    // AncestorLookup_Nested interface

    public AncestorLookup_Nested(Group group,
                                 RowType rowType,
                                 Collection<UserTableRowType> ancestorTypes,
                                 int inputBindingPosition,
                                 int lookaheadQuantum)
    {
        validateArguments(group, rowType, ancestorTypes, inputBindingPosition);
        this.group = group;
        this.rowType = rowType;
        this.inputBindingPosition = inputBindingPosition;
        this.lookaheadQuantum = lookaheadQuantum;
        // Sort ancestor types by depth
        this.ancestors = new ArrayList<>(ancestorTypes.size());
        for (UserTableRowType ancestorType : ancestorTypes) {
            this.ancestors.add(ancestorType.userTable());
        }
        if (this.ancestors.size() > 1) {
            Collections.sort(this.ancestors,
                             new Comparator<UserTable>()
                             {
                                 @Override
                                 public int compare(UserTable x, UserTable y)
                                 {
                                     return x.getDepth() - y.getDepth();
                                 }
                             });
        }
    }

    // For use by this class

    private void validateArguments(Group group,
                                   RowType rowType,
                                   Collection<? extends RowType> ancestorTypes,
                                   int inputBindingPosition)
    {
        ArgumentValidation.notNull("group", group);
        ArgumentValidation.notNull("rowType", rowType);
        ArgumentValidation.notNull("ancestorTypes", ancestorTypes);
        ArgumentValidation.notEmpty("ancestorTypes", ancestorTypes);
        ArgumentValidation.isTrue("inputBindingPosition >= 0", inputBindingPosition >= 0);
        if (rowType instanceof IndexRowType) {
            // Keeping index rows not supported
            RowType tableRowType = ((IndexRowType) rowType).tableType();
            // Each ancestorType must be an ancestor of rowType. ancestorType = tableRowType is OK only if the input
            // is from an index. I.e., this operator can be used for an index lookup.
            for (RowType ancestorType : ancestorTypes) {
                ArgumentValidation.isTrue("ancestorType.ancestorOf(tableRowType)",
                                          ancestorType.ancestorOf(tableRowType));
                ArgumentValidation.isTrue("ancestorType.userTable().getGroup() == tableRowType.userTable().getGroup()",
                                          ancestorType.userTable().getGroup() == tableRowType.userTable().getGroup());
            }
        } else if (rowType instanceof UserTableRowType) {
            // Each ancestorType must be an ancestor of rowType. ancestorType = tableRowType is OK only if the input
            // is from an index. I.e., this operator can be used for an index lookup.
            for (RowType ancestorType : ancestorTypes) {
                ArgumentValidation.isTrue("ancestorType != tableRowType",
                                          ancestorType != rowType);
                ArgumentValidation.isTrue("ancestorType.ancestorOf(tableRowType)",
                                          ancestorType.ancestorOf(rowType));
                ArgumentValidation.isTrue("ancestorType.userTable().getGroup() == tableRowType.userTable().getGroup()",
                                          ancestorType.userTable().getGroup() == rowType.userTable().getGroup());
            }
        } else if (rowType instanceof HKeyRowType) {
        } else {
            ArgumentValidation.isTrue("invalid rowType", false);
        }
    }

    // Class state

    private static final Logger LOG = LoggerFactory.getLogger(AncestorLookup_Nested.class);
    private static final InOutTap TAP_OPEN = OPERATOR_TAP.createSubsidiaryTap("operator: AncestorLookup_Nested open");
    private static final InOutTap TAP_NEXT = OPERATOR_TAP.createSubsidiaryTap("operator: AncestorLookup_Nested next");

    // Object state

    private final Group group;
    private final RowType rowType;
    private final List<UserTable> ancestors;
    private final int inputBindingPosition;
    private final int lookaheadQuantum;

    @Override
    public CompoundExplainer getExplainer(ExplainContext context)
    {
        Attributes atts = new Attributes();
        atts.put(Label.BINDING_POSITION, PrimitiveExplainer.getInstance(inputBindingPosition));
        for (UserTable table : ancestors) {
            atts.put(Label.OUTPUT_TYPE, ((Schema)rowType.schema()).userTableRowType(table).getExplainer(context));
        }
        atts.put(Label.PIPELINE, PrimitiveExplainer.getInstance(lookaheadQuantum));
        return new LookUpOperatorExplainer(getName(), atts, rowType, false, null, context);
    }

    // Inner classes

    private class Execution extends LeafCursor
    {
        // Cursor interface

        @Override
        public void open()
        {
            TAP_OPEN.in();
            try {
                CursorLifecycle.checkIdle(this);
                Row rowFromBindings = bindings.getRow(inputBindingPosition);
                if (LOG_EXECUTION) {
                    LOG.debug("AncestorLookup_Nested: open using {}", rowFromBindings);
                }
                findAncestors(rowFromBindings);
                closed = false;
            } finally {
                TAP_OPEN.out();
            }
        }

        @Override
        public Row next()
        {
            if (TAP_NEXT_ENABLED) {
                TAP_NEXT.in();
            }
            try {
                if (CURSOR_LIFECYCLE_ENABLED) {
                    CursorLifecycle.checkIdleOrActive(this);
                }
                checkQueryCancelation();
                Row row = pending.take();
                if (LOG_EXECUTION) {
                    LOG.debug("AncestorLookup: {}", row == null ? null : row);
                }
                if (row == null) {
                    close();
                }
                return row;
            } finally {
                if (TAP_NEXT_ENABLED) {
                    TAP_NEXT.out();
                }
            }
        }

        @Override
        public void close()
        {
            CursorLifecycle.checkIdleOrActive(this);
            if (!closed) {
                pending.clear();
                ancestorCursor.close();
                closed = true;
            }
        }

        @Override
        public void destroy()
        {
            close();
            ancestorCursor.destroy();
        }

        @Override
        public boolean isIdle()
        {
            return closed;
        }

        @Override
        public boolean isActive()
        {
            return !closed;
        }

        @Override
        public boolean isDestroyed()
        {
            return ancestorCursor.isDestroyed();
        }

        // Execution interface

        Execution(QueryContext context, QueryBindingsCursor bindingsCursor)
        {
            super(context, bindingsCursor);
            this.pending = new PendingRows(ancestors.size() + 1);
            this.ancestorCursor = adapter().newGroupCursor(group);
        }

        // For use by this class

        private void findAncestors(Row row)
        {
            assert pending.isEmpty();
            for (int i = 0; i < ancestors.size(); i++) {
                Row ancestorRow = readAncestorRow(row.ancestorHKey(ancestors.get(i)));
                if (ancestorRow != null) {
                    pending.add(ancestorRow);
                }
            }
        }

        private Row readAncestorRow(HKey hKey)
        {
            Row row;
            ancestorCursor.close();
            ancestorCursor.rebind(hKey, false);
            ancestorCursor.open();
            row = ancestorCursor.next();
            // Retrieved row might not actually be what we were looking for -- not all ancestors are present,
            // (there are orphan rows).
            if (row != null && !hKey.equals(row.hKey())) {
                row = null;
            }
            return row;
        }

        // Object state

        private final GroupCursor ancestorCursor;
        private final PendingRows pending;
        private boolean closed = true;
    }

    private class AncestorCursor implements BindingsAwareCursor
    {
        // BindingsAwareCursor interface

        @Override
        public void open() {
            Row rowFromBindings = bindings.getRow(inputBindingPosition);
            assert rowFromBindings.rowType() == rowType : rowFromBindings;
            for (int i = 0; i < hKeys.length; i++) {
                hKeys[i] = rowFromBindings.ancestorHKey(ancestors.get(i));
                cursors[i].rebind(hKeys[i], false);
                cursors[i].open();
            }
            cursorIndex = 0;
        }

        @Override
        public Row next() {
            Row row = null;
            while ((row == null) && (cursorIndex < cursors.length)) {
                row = cursors[cursorIndex].next();
                cursors[cursorIndex].close();
                if (row != null && !hKeys[cursorIndex].equals(row.hKey())) {
                    row = null;
                }
                cursorIndex++;
            }
            return row;
        }

        @Override
        public void jump(Row row, ColumnSelector columnSelector) {
            throw new UnsupportedOperationException(getClass().getName());
        }

        @Override
        public void close() {
            for (GroupCursor cursor : cursors) {
                cursor.close();
            }
        }

        @Override
        public void destroy() {
            for (GroupCursor cursor : cursors) {
                cursor.destroy();
            }
        }

        @Override
        public boolean isIdle() {
            return ((cursorIndex >= cursors.length) || cursors[cursorIndex].isIdle());
        }

        @Override
        public boolean isActive() {
            return ((cursorIndex < cursors.length) && cursors[cursorIndex].isActive());
        }

        @Override
        public boolean isDestroyed() {
            return cursors[0].isDestroyed();
        }
        
        @Override
        public void rebind(QueryBindings bindings) {
            this.bindings = bindings;
        }

        // AncestorCursor interface
        public AncestorCursor(StoreAdapter adapter) {
            hKeys = new HKey[ancestors.size()];
            cursors = new GroupCursor[hKeys.length];
            for (int i = 0; i < cursors.length; i++) {
                cursors[i] = adapter.newGroupCursor(group);
            }
        }

        // Object state

        private final HKey[] hKeys;
        private final GroupCursor[] cursors;
        private QueryBindings bindings;
        private int cursorIndex;
    }

    private class LookaheadExecution extends LookaheadLeafCursor<AncestorCursor>
    {
        // Cursor interface

        @Override
        public void open() {
            TAP_OPEN.in();
            try {
                super.open();
            } finally {
                TAP_OPEN.out();
            }
        }

        @Override
        public Row next() {
            if (TAP_NEXT_ENABLED) {
                TAP_NEXT.in();
            }
            try {
                Row row = super.next();
                if (LOG_EXECUTION) {
                    LOG.debug("AncestorLookup_Nested: yield {}", row);
                }
                return row;
            } finally {
                if (TAP_NEXT_ENABLED) {
                    TAP_NEXT.out();
                }
            }
        }

        // LookaheadLeafCursor interface

        @Override
        protected AncestorCursor newCursor(QueryContext context, StoreAdapter adapter) {
            return new AncestorCursor(adapter);
        }

        @Override
        protected AncestorCursor openACursor(QueryBindings bindings, boolean lookahead) {
            if (LOG_EXECUTION) {
                LOG.debug("AncestorLookup_Nested: open{} using {}", lookahead ? " lookahead" : "", bindings.getRow(inputBindingPosition));
            }
            return super.openACursor(bindings, lookahead);
        }
        
        // LookaheadExecution interface

        LookaheadExecution(QueryContext context, QueryBindingsCursor bindingsCursor, 
                           StoreAdapter adapter, int quantum) {
            super(context, bindingsCursor, adapter, quantum);
        }
    }
}
