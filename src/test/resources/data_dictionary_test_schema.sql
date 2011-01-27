--
-- Copyright (C) 2011 Akiban Technologies Inc.
-- This program is free software: you can redistribute it and/or modify
-- it under the terms of the GNU Affero General Public License, version 3,
-- as published by the Free Software Foundation.
--
-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU Affero General Public License for more details.
--
-- You should have received a copy of the GNU Affero General Public License
-- along with this program.  If not, see http://www.gnu.org/licenses.
--

-- akiba_information_schema database

source src/main/resources/akiba_information_schema.sql
use akiba_information_schema;

drop database if exists data_dictionary_test;
create database data_dictionary_test;

-- don't forget the grants to allow access
grant all on data_dictionary_test.* to akiba, akiba@'localhost';

-- -----------------------------------------------------------------------------

use akiba_information_schema;

-- Tables

-- -- Group
insert into tables values('akiba_objects', 'coi', 'GROUP', 0, null, null, null);

-- -- User
insert into tables values('data_dictionary_test', 'customer', 'USER', 1,  'akiba_objects', 'coi', null);
insert into tables values('data_dictionary_test', 'order', 'USER', 2, 'data_dictionary_test', 'customer', 0);
insert into tables values('data_dictionary_test', 'item', 'USER', 3, 'data_dictionary_test', 'order', 0);


-- Columns

-- -- Group

insert into columns values('akiba_objects', 'coi', 'customer$customer_id', 0,
                            'INT', null, null, 0,
                             null, null, null,
                             null, null, null);
insert into columns values('akiba_objects', 'coi', 'customer$customer_name', 1,
                            'VARCHAR', 100, null, 0,
                            null, null, null,
                            null, null, null);
insert into columns values('akiba_objects', 'coi', 'order$order_id', 2,
                            'INT', null, null, 0,
                            null, null, null,
                            null, null, null);
insert into columns values('akiba_objects', 'coi', 'order$customer_id', 3,
                            'INT', null, null, 0,
                            null, null, null,
                            null, null, null);
insert into columns values('akiba_objects', 'coi', 'order$order_date', 4,
                            'INT', null, null, 0,
                            null, null, null,
                            null, null, null);
insert into columns values('akiba_objects', 'coi', 'item$order_id', 5,
                            'INT', null, null, 0,
                            null, null, null,
                            null, null, null);
insert into columns values('akiba_objects', 'coi', 'item$part_id', 6,
                            'INT', null, null, 0,
                            null, null, null,
                            null, null, null);
insert into columns values('akiba_objects', 'coi', 'item$quantity', 7,
                            'INT', null, null, 0,
                            null, null, null,
                            null, null, null);
insert into columns values('akiba_objects', 'coi', 'item$unit_price', 8,
                            'INT', null, null, 0,
                            null, null, null,
                            null, null, null);

-- -- User

insert into columns values('data_dictionary_test', 'customer', 'customer_id', 0,
                            'INT', null, null, 0,
                            'akiba_objects', 'coi', 'customer$customer_id',
                            null, null, null);
insert into columns values('data_dictionary_test', 'customer', 'customer_name', 1,
                            'VARCHAR', 100, null, 0,
                            'akiba_objects', 'coi', 'customer$customer_name',
                            null, null, null);
insert into columns values('data_dictionary_test', 'order', 'order_id', 0,
                            'INT', null, null, 0,
                            'akiba_objects', 'coi', 'order$order_id',
                            null, null, null);
insert into columns values('data_dictionary_test', 'order', 'customer_id', 1,
                            'INT', null, null, 0,
                            'akiba_objects', 'coi', 'order$customer_id',
                            'data_dictionary_test', 'customer', 'customer_id');
insert into columns values('data_dictionary_test', 'order', 'order_date', 2,
                            'INT', null, null, 0,
                            'akiba_objects', 'coi', 'order$order_date',
                            null, null, null);
insert into columns values('data_dictionary_test', 'item', 'order_id', 0,
                            'INT', null, null, 0,
                            'akiba_objects', 'coi', 'item$order_id',
                            'data_dictionary_test', 'order', 'order_id');
insert into columns values('data_dictionary_test', 'item', 'part_id', 1,
                            'INT', null, null, 0,
                            'akiba_objects', 'coi', 'item$part_id',
                            null, null, null);
insert into columns values('data_dictionary_test', 'item', 'quantity', 2,
                            'INT', null, null, 0,
                            'akiba_objects', 'coi', 'item$quantity',
                            null, null, null);
insert into columns values('data_dictionary_test', 'item', 'unit_price', 3,
                            'INT', null, null, 0,
                            'akiba_objects', 'coi', 'item$unit_price',
                            null, null, null);

-- Indexes 
insert into indexes values ('akiba_objects', 'coi', 'coi_customer_PK', 0, 'PRIMARY KEY', 1);
insert into index_columns values ('akiba_objects', 'coi', 'coi_customer_PK', 'customer$customer_id', 0, 1); 

insert into indexes values ('akiba_objects', 'coi', 'coi_order_PK', 0, 'PRIMARY KEY', 1);
insert into index_columns values ('akiba_objects', 'coi', 'coi_order_PK', 'order$order_id', 0, 1);

insert into indexes values ('akiba_objects', 'coi', 'coi_item_PK', 0, 'PRIMARY KEY', 1);
insert into index_columns  values ('akiba_objects', 'coi', 'coi_item_PK', 'order$order_id', 0, 1);
insert into index_columns  values ('akiba_objects', 'coi', 'coi_item_PK', 'item$part_id', 1, 1);

insert into indexes values ('data_dictionary_test', 'customer', 'customer_PK', 0, 'PRIMARY KEY', 1);
insert into index_columns values ('data_dictionary_test', 'customer', 'customer_PK', 'customer_id', 0, 1); 

insert into indexes values ('data_dictionary_test', 'order', 'order_PK', 0, 'PRIMARY KEY', 1);
insert into index_columns values ('data_dictionary_test', 'order', 'order_PK', 'order_id', 0, 1);

insert into indexes values ('data_dictionary_test', 'item', 'item_PK', 0, 'PRIMARY KEY', 1);
insert into index_columns values ('data_dictionary_test', 'item', 'item_PK', 'order_id', 0, 1);
insert into index_columns values ('data_dictionary_test', 'item', 'item_PK', 'part_id', 1, 1);

 
-- Groups

insert into groups values('coi', 'akiba_objects', 'coi');

-- -----------------------------------------------------------------------------

-- USER TABLES

use data_dictionary_test;

create table customer(
    customer_id int not null,
    customer_name varchar(100) not null,
    primary key(customer_id)
) engine=akibandb;

create table `order`(
    order_id int not null,
    customer_id int not null,
    order_date int not null,
    primary key(order_id),
    foreign key(customer_id) references customer
) engine=akibandb;

create table item(
    order_id int not null,
    part_id int not null,
    quantity int not null,
    unit_price int not null,
    primary key(order_id, part_id),
    foreign key(order_id) references `order`
) engine=akibandb;

-- GROUP TABLES

use akiba_objects;

create table coi(
    customer$customer_id int not null,
    customer$customer_name varchar(100) not null,
    order$order_id int not null,
    order$order_date int not null,
    item$part_id int not null,
    item$quantity int not null,
    item$unit_price int not null
) engine=akibandb;

