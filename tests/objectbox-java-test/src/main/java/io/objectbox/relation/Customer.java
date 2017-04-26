package io.objectbox.relation;

import java.util.List;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.Relation;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.exception.DbDetachedException;

/**
 * Entity mapped to table "CUSTOMER".
 */
@Entity
public class Customer {

    @Id
    private long id;

    @Index
    private String name;

    @Relation(idProperty = "customerId")
    List<Order> orders;

    /** Used to resolve relations */
    @Internal
    @Generated(1307364262)
    transient BoxStore __boxStore;

    @Generated(60841032)
    public Customer() {
    }

    @Generated(1039711609)
    public Customer(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(954185799)
    public List<Order> getOrders() {
        if (orders == null) {
            final BoxStore boxStore = this.__boxStore;
            if (boxStore == null) {
                throw new DbDetachedException();
            }
            Box<Order> box = boxStore.boxFor(Order.class);
            int targetEntityId = boxStore.getEntityTypeIdOrThrow(Order.class);
            List<Order> ordersNew = box.getBacklinkEntities(targetEntityId, Order_.customerId, id);
            synchronized (this) {
                if (orders == null) {
                    orders = ordersNew;
                }
            }
        }
        return orders;
    }

    /** Resets a to-many relationship, making the next get call to query for a fresh result. */
    @Generated(1446109810)
    public synchronized void resetOrders() {
        orders = null;
    }

}
