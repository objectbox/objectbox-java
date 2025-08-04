/*
 * Copyright 2017-2025 ObjectBox Ltd.
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

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.NameInDb;

/**
 * Order entity to test relations together with {@link Customer}.
 * <p>
 * The annotations in this class have no effect as the Gradle plugin is not configured in this project. However, test
 * code builds a model like if the annotations were processed.
 * <p>
 * There is a matching test in the internal integration test project where this is tested and model builder code can be
 * "stolen" from.
 */
@Entity
@NameInDb("ORDERS")
public class Order implements Serializable {

    @Id(assignable = true)
    long id;
    java.util.Date date;
    long customerId;
    String text;

    // Note: in a typical project the relation fields are initialized by the ObjectBox byte code transformer
    // https://docs.objectbox.io/relations#initialization-magic
    @SuppressWarnings("FieldMayBeFinal")
    private ToOne<Customer> customer = new ToOne<>(this, Order_.customer);

    // Note: in a typical project the BoxStore field is added by the ObjectBox byte code transformer
    // https://docs.objectbox.io/relations#initialization-magic
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
