
// tag::itemPopularityTables[]
USE shoppingcart;
CREATE TABLE IF NOT EXISTS item_popularity (
  item_id text,
  count counter,
  PRIMARY KEY (item_id));
// end::itemPopularityTables[]
