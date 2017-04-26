package io.objectbox.relation;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.NameInDb;
import io.objectbox.annotation.Relation;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.exception.DbDetachedException;

/**
 * Entity mapped to table "ORDERS".
 */
@Entity
@NameInDb("ORDERS")
public class Order {

    @Id(assignable = true)
    long id;
    java.util.Date date;
    long customerId;
    String text;

    @Relation
    private Customer customer;

    /** @Depreacted Used to resolve relations */
    @Internal
    @Generated(975972993)
    transient BoxStore __boxStore;
    @Internal
    @Generated(1031210392)
    transient ToOne<Customer> customer__toOne = new ToOne<>(this, Order_.customer);

    @Generated(1105174599)
    public Order() {
    }

    public Order(Long id) {
        this.id = id;
    }

    @Generated(10986505)
    public Order(long id, java.util.Date date, long customerId, String text) {
        this.id = id;
        this.date = date;
        this.customerId = customerId;
        this.text = text;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public java.util.Date getDate() {
        return date;
    }

    public void setDate(java.util.Date date) {
        this.date = date;
    }

    public long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(long customerId) {
        this.customerId = customerId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Customer peekCustomer() {
        return customer;
    }

    /** To-one relationship, resolved on first access. */
    @Generated(910495430)
    public Customer getCustomer() {
        customer = customer__toOne.getTarget(this.customerId);
        return customer;
    }

    /** Set the to-one relation including its ID property. */
    @Generated(1322376583)
    public void setCustomer(Customer customer) {
        customer__toOne.setTarget(customer);
        this.customer = customer;
    }

}
