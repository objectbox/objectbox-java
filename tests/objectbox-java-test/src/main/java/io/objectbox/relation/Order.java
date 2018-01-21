/*
 * Copyright 2017 ObjectBox Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.objectbox.relation;

import java.io.Serializable;

import javax.annotation.Nullable;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.NameInDb;
import io.objectbox.annotation.apihint.Internal;

/**
 * Entity mapped to table "ORDERS".
 */
@Entity
@NameInDb("ORDERS")
public class Order implements Serializable {

    @Id(assignable = true)
    long id;
    java.util.Date date;
    long customerId;
    String text;

    private Customer customer;

    /** @Depreacted Used to resolve relations */
    @Internal
    transient BoxStore __boxStore;

    @Internal
    transient ToOne<Customer> customer__toOne = new ToOne<>(this, Order_.customer);

    public Order() {
    }

    public Order(Long id) {
        this.id = id;
    }

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
    public Customer getCustomer() {
        customer = customer__toOne.getTarget(this.customerId);
        return customer;
    }

    /** Set the to-one relation including its ID property. */
    public void setCustomer(@Nullable Customer customer) {
        customer__toOne.setTarget(customer);
        this.customer = customer;
    }

}
