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

package com.foundationdb.direct;

public class COIDirectClasses {

    //
    // Interfaces and classes are adapted from source code generated by {@link
    // com.akiban.direct.ClassBuilder}.
    // ------------------------------------------------------------------------------------------
    //
    interface Iface extends DirectObject {
        interface Address {
            public int getAid();

            public void setAid(int aid);

            public int getCid();

            public void setCid(int cid);

            public String getState();

            public void setState(String state);

            public String getCity();

            public void setCity(String city);

            public Customer getCustomer();

            public void save();
        }

        interface Customer extends DirectObject {
            public int getCid();

            public void setCid(int cid);

            public String getName();

            public void setName(String name);

            public Address getAddress(int aid);

            public DirectIterable<Address> getAddresses();

            public Order getOrder(int oid);

            public DirectIterable<Order> getOrders();

            public void save();
        }

        interface Item extends DirectObject {
            public int getIid();

            public void setIid(int iid);

            public int getOid();

            public void setOid(int oid);

            public String getSku();

            public void setSku(String sku);

            public Order getOrder();

            public void save();
        }

        interface Order extends DirectObject {
            public int getOid();

            public void setOid(int oid);

            public int getCid();

            public void setCid(int cid);

            public java.sql.Date getOdate();

            public void setOdate(java.sql.Date odate);

            public Customer getCustomer();

            public Item getItem(int iid);

            public DirectIterable<Item> getItems();

            public void save();
        }
    }

    static class Test$Customer extends com.akiban.direct.AbstractDirectObject implements Iface.Customer {
        static {
            __init("test", "customers", "cid:cid:int:0:-1,name:name:String:-1:-1");
        }

        public int getCid() {
            return __getINT(0);
        }

        public void setCid(int cid) {
            __setINT(0, cid);
        }

        public String getName() {
            return __getVARCHAR(1);
        }

        public void setName(String name) {
            __setVARCHAR(1, name);
        }

        public Iface.Address getAddress(int aid) {
            return (new com.akiban.direct.DirectIterableImpl<Iface.Address>(Iface.Address.class, "addresses", this))
                    .where("cid", Integer.valueOf(getCid())).where("aid", Integer.valueOf(aid)).single();
        }

        public com.akiban.direct.DirectIterable<Iface.Address> getAddresses() {
            return (new com.akiban.direct.DirectIterableImpl<Iface.Address>(Iface.Address.class, "addresses", this)).where("cid",
                    Integer.valueOf(getCid()));
        }

        public Iface.Order getOrder(int oid) {
            return (new com.akiban.direct.DirectIterableImpl<Iface.Order>(Iface.Order.class, "orders", this))
                    .where("cid", Integer.valueOf(getCid())).where("oid", Integer.valueOf(oid)).single();
        }

        public com.akiban.direct.DirectIterable<Iface.Order> getOrders() {
            return (new com.akiban.direct.DirectIterableImpl<Iface.Order>(Iface.Order.class, "orders", this)).where("cid",
                    Integer.valueOf(getCid()));
        }

    }

    static class Test$Order extends com.akiban.direct.AbstractDirectObject implements Iface.Order {
        static {
            __init("test", "orders", "oid:oid:int:0:-1,cid:cid:int:-1:0,odate:odate:Date:-1:-1");
        }

        public int getOid() {
            return __getINT(0);
        }

        public void setOid(int oid) {
            __setINT(0, oid);
        }

        public int getCid() {
            return __getINT(1);
        }

        public void setCid(int cid) {
            __setINT(1, cid);
        }

        public java.sql.Date getOdate() {
            return __getDATE(2);
        }

        public void setOdate(java.sql.Date odate) {
            __setDATE(2, odate);
        }

        public Iface.Item getItem(int iid) {
            return (new com.akiban.direct.DirectIterableImpl<Iface.Item>(Iface.Item.class, "items", this))
                    .where("oid", Integer.valueOf(getOid())).where("iid", Integer.valueOf(iid)).single();
        }

        public com.akiban.direct.DirectIterable<Iface.Item> getItems() {
            return (new com.akiban.direct.DirectIterableImpl<Iface.Item>(Iface.Item.class, "items", this)).where("oid",
                    Integer.valueOf(getOid()));
        }

        public Iface.Customer getCustomer() {
            return (new com.akiban.direct.DirectIterableImpl<Iface.Customer>(Iface.Customer.class, "customers", this))
                    .where("cid", Integer.valueOf(getCid())).single();
        }

    }

    static class Test$Item extends com.akiban.direct.AbstractDirectObject implements Iface.Item {
        static {
            __init("test", "items", "iid:iid:int:0:-1,oid:oid:int:-1:0,sku:sku:String:-1:-1");
        }

        public int getIid() {
            return __getINT(0);
        }

        public void setIid(int iid) {
            __setINT(0, iid);
        }

        public int getOid() {
            return __getINT(1);
        }

        public void setOid(int oid) {
            __setINT(1, oid);
        }

        public String getSku() {
            return __getVARCHAR(2);
        }

        public void setSku(String sku) {
            __setVARCHAR(2, sku);
        }

        public Iface.Order getOrder() {
            return (new com.akiban.direct.DirectIterableImpl<Iface.Order>(Iface.Order.class, "orders", this)).where(
                    "oid", Integer.valueOf(getOid())).single();
        }

    }

    static class Test$Address extends com.akiban.direct.AbstractDirectObject implements Iface.Address {
        static {
            __init("test", "addresses",
                    "aid:aid:int:0:-1,cid:cid:int:-1:0,state:state:String:-1:-1,city:city:String:-1:-1");
        }

        public int getAid() {
            return __getINT(0);
        }

        public void setAid(int aid) {
            __setINT(0, aid);
        }

        public int getCid() {
            return __getINT(1);
        }

        public void setCid(int cid) {
            __setINT(1, cid);
        }

        public String getState() {
            return __getVARCHAR(2);
        }

        public void setState(String state) {
            __setVARCHAR(2, state);
        }

        public String getCity() {
            return __getVARCHAR(3);
        }

        public void setCity(String city) {
            __setVARCHAR(3, city);
        }

        public Iface.Customer getCustomer() {
            return (new com.akiban.direct.DirectIterableImpl<Iface.Customer>(Iface.Customer.class, "customers", this))
                    .where("cid", Integer.valueOf(getCid())).single();
        }

    }
    
    static void registerDirect() {
        Direct.registerDirectObjectClass(Iface.Address.class, Test$Address.class);
        Direct.registerDirectObjectClass(Iface.Customer.class, Test$Customer.class);
        Direct.registerDirectObjectClass(Iface.Order.class, Test$Order.class);
        Direct.registerDirectObjectClass(Iface.Item.class, Test$Item.class);
    }

}
