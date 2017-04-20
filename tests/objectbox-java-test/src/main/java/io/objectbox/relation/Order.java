package io.objectbox.relation;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Generated;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Relation;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.exception.DbDetachedException;

/**
 * Entity mapped to table "ORDERS".
 */
@Entity(nameInDb = "ORDERS")
public class Order {

    @Id(assignable = true)
    private long id;
    private java.util.Date date;
    private long customerId;
    private String text;

    @Relation
    private Customer customer;


    @Internal
    @Generated(hash = 1698848862)
    private transient Long customer__resolvedKey;

    /** Used to resolve relations */
    @Internal
    @Generated(hash = 1307364262)
    transient BoxStore __boxStore;

    @Generated(hash = 1105174599)
    public Order() {
    }

    public Order(Long id) {
        this.id = id;
    }

    @Generated(hash = 10986505)
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
    @Generated(hash = 910495430)
    public Customer getCustomer() {
        long __key = this.customerId;
        if (customer__resolvedKey == null || customer__resolvedKey != __key) {
            final BoxStore boxStore = this.__boxStore;
            if (boxStore == null) {
                throw new DbDetachedException();
            }
            Box<Customer> box = boxStore.boxFor(Customer.class);
            Customer customerNew = box.get(__key);
            synchronized (this) {
                customer = customerNew;
                customer__resolvedKey = __key;
            }
        }
        return customer;
    }

    /** Set the to-one relation including its ID property. */
    @Generated(hash = 1322376583)
    public void setCustomer(Customer customer) {
        synchronized (this) {
            this.customer = customer;
            customerId = customer == null ? 0 : customer.getId();
            customer__resolvedKey = customerId;
        }
    }

}
