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

    @SuppressWarnings("FieldMayBeFinal")
    private ToOne<Customer> customer = new ToOne<>(this, Order_.customer);

    /** Used to resolve relations. */
    transient BoxStore __boxStore;

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

    public ToOne<Customer> getCustomer() {
        return customer;
    }

}
