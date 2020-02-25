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

import org.junit.After;
import org.junit.Before;

import java.io.File;

import javax.annotation.Nullable;

import io.objectbox.AbstractObjectBoxTest;
import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.BoxStoreBuilder;
import io.objectbox.DebugFlags;

public abstract class AbstractRelationTest extends AbstractObjectBoxTest {

    protected Box<Order> orderBox;
    protected Box<Customer> customerBox;

    @Override
    protected BoxStore createBoxStore() {
        return MyObjectBox.builder().baseDirectory(boxStoreDir)
                .debugFlags(DebugFlags.LOG_TRANSACTIONS_READ | DebugFlags.LOG_TRANSACTIONS_WRITE)
                .build();
    }

    @After
    public void deleteDbFiles() {
        BoxStore.deleteAllFiles(new File(BoxStoreBuilder.DEFAULT_NAME));
    }

    @Before
    public void initBoxes() {
        deleteDbFiles();
        customerBox = store.boxFor(Customer.class);
        orderBox = store.boxFor(Order.class);
        customerBox.removeAll();
        orderBox.removeAll();
    }

    protected Customer putCustomer() {
        Customer customer = new Customer();
        customer.setName("Joe");
        customerBox.put(customer);
        return customer;
    }

    protected Order putOrder(@Nullable Customer customer, @Nullable String text) {
        Order order = new Order();
        order.setCustomer(customer);
        order.setText(text);
        orderBox.put(order);
        return order;
    }

}
