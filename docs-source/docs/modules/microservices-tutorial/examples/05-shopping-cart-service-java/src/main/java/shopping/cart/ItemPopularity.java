package shopping.cart;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;

@Entity
@Table(name = "item_popularity")
public class ItemPopularity {

  // primary key
  @Id private final String itemId;

  // optimistic locking
  @Version private final long version;

  private final long count;

  public ItemPopularity() {
    this.version = 0;
    this.itemId = "";
    this.count = 0;
  }

  public ItemPopularity(String itemId, long version, long count) {
    this.itemId = itemId;
    this.version = version;
    this.count = count;
  }

  public String getItemId() {
    return itemId;
  }

  public long getCount() {
    return count;
  }

  public long getVersion() {
    return version;
  }

  public ItemPopularity changeCount(long delta) {
    return new ItemPopularity(itemId, version, count + delta);
  }

  @Transient
  @JsonIgnore
  boolean isNew() {
    return itemId.equals("") || version <= 0;
  }
}
