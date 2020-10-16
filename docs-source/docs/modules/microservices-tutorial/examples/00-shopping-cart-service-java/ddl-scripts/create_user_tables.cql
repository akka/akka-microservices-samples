USE shoppingcartservice;
CREATE TABLE IF NOT EXISTS item_popularity (
  item_id text,
  count counter, // <1>
  PRIMARY KEY (item_id));

