SELECT name, order_date, quan, sku
  FROM customers
 INNER JOIN orders ON customers.cid = orders.cid
 INNER JOIN items ON orders.oid = items.oid
 WHERE NOT (name LIKE '%Smith' OR sku = '1234') 
   AND quan > 1000
