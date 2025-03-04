/*
 * Copyright 2025 ObjectBox Ltd. All rights reserved.
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


import org.junit.Test;


import static org.junit.Assert.assertEquals;

public class ExternalTypeTest extends AbstractRelationTest {

    /**
     * There is no way to test external type mapping works here. Instead, verify passing a model with
     * {@link io.objectbox.ModelBuilder.RelationBuilder#externalType(int)} works (see {@link MyObjectBox}) and that
     * there are no side effects for put and get.
     */
    @Test
    public void standaloneToMany_externalType_putGetSmokeTest() {
        Customer putCustomer = new Customer();
        putCustomer.setName("Joe");
        Order order = new Order();
        order.setText("Order from Joe");
        putCustomer.getToManyExternalId().add(order);
        long customerId = customerBox.put(putCustomer);

        Customer readCustomer = customerBox.get(customerId);
        assertEquals(order.getText(), readCustomer.getToManyExternalId().get(0).getText());
    }

}
