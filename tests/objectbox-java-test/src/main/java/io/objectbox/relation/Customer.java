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
import java.util.List;

import io.objectbox.BoxStore;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
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

    List<Order> orders = new ToMany<>(this, Customer_.orders);

    ToMany<Order> ordersStandalone = new ToMany<>(this, Customer_.ordersStandalone);

    /** Used to resolve relations */
    @Internal
    transient BoxStore __boxStore;

    public Customer() {
    }

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
