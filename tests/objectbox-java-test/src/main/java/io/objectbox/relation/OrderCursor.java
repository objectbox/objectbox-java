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

import io.objectbox.BoxStore;
import io.objectbox.Cursor;
import io.objectbox.Transaction;
import io.objectbox.internal.CursorFactory;

// NOTE: Instead of updating this by hand, copy changes from the internal integration test project after updating its
// Customer class.

/**
 * Cursor for DB entity "ORDERS".
 */
public final class OrderCursor extends Cursor<Order> {
    static final class Factory implements CursorFactory<Order> {
        public Cursor<Order> createCursor(Transaction tx, long cursorHandle, BoxStore boxStoreForEntities) {
            return new OrderCursor(tx, cursorHandle, boxStoreForEntities);
        }
    }

    private static final Order_.OrderIdGetter ID_GETTER = Order_.__ID_GETTER;

    // TODO private Query<Order> customer_OrdersQuery;

    // Property IDs get verified in Cursor base class
    private final static int __ID_date = Order_.date.id;
    private final static int __ID_customerId = Order_.customerId.id;
    private final static int __ID_text = Order_.text.id;

    public OrderCursor(Transaction tx, long cursor, BoxStore boxStore) {
        super(tx, cursor, Order_.__INSTANCE, boxStore);
    }

    @Override
    public long getId(Order entity) {
        return ID_GETTER.getId(entity);
    }

    /**
     * Puts an object into its box.
     *
     * @return The ID of the object within its box.
     */
    @Override
    public long put(Order entity) {
        ToOne<Customer> customer = entity.getCustomer();
        if(customer != null && customer.internalRequiresPutTarget()) {
            Cursor<Customer> targetCursor = getRelationTargetCursor(Customer.class);
            try {
                customer.internalPutTarget(targetCursor);
            } finally {
                targetCursor.close();
            }
        }
        String text = entity.getText();
        int __id3 = text != null ? __ID_text : 0;
        java.util.Date date = entity.date;
        int __id1 = date != null ? __ID_date : 0;

        long __assignedId = collect313311(cursor, entity.id, PUT_FLAG_FIRST | PUT_FLAG_COMPLETE,
                __id3, text, 0, null,
                0, null, 0, null,
                __ID_customerId, entity.customerId, __id1, __id1 != 0 ? date.getTime() : 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0);

        entity.setId(__assignedId);
        entity.__boxStore = boxStoreForEntities;
        return __assignedId;
    }

}
