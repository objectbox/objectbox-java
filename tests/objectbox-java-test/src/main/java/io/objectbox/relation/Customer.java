package io.objectbox.relation;

import java.io.Serializable;
import java.util.List;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.Relation;
import io.objectbox.annotation.apihint.Internal;

/**
 * Entity mapped to table "CUSTOMER".
 */
@Entity
public class Customer implements Serializable {

    @Id
    private long id;

    @Index
    private String name;

    @Relation(idProperty = "customerId")
    List<Order> orders = new ToMany<>(this, Customer_.orders);

    ToMany<Order> ordersStandalone = new ToMany<>(this, Customer_.ordersStandalone);

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

    public List<Order> getOrders() {
        return orders;
    }

    public ToMany<Order> getOrdersStandalone() {
        return ordersStandalone;
    }
}
