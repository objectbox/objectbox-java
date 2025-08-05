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
 * Cursor for DB entity "Customer".
 */
public final class CustomerCursor extends Cursor<Customer> {

    static final class Factory implements CursorFactory<Customer> {
        public Cursor<Customer> createCursor(Transaction tx, long cursorHandle, BoxStore boxStoreForEntities) {
            return new CustomerCursor(tx, cursorHandle, boxStoreForEntities);
        }
    }

    // Property IDs get verified in Cursor base class
    private final static int __ID_name = Customer_.name.id;

    public CustomerCursor(Transaction tx, long cursor, BoxStore boxStore) {
        super(tx, cursor, Customer_.__INSTANCE, boxStore);
    }

    @Override
    public long getId(Customer entity) {
        return entity.getId();
    }

    /**
     * Puts an object into its box.
     *
     * @return The ID of the object within its box.
     */
    @Override
    public long put(Customer entity) {
        String name = entity.getName();
        int __id1 = name != null ? __ID_name : 0;

        long __assignedId = collect313311(cursor, entity.getId(), PUT_FLAG_FIRST | PUT_FLAG_COMPLETE,
                __id1, name, 0, null,
                0, null, 0, null,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0);

        entity.setId(__assignedId);
        entity.__boxStore = boxStoreForEntities;

        checkApplyToManyToDb(entity.getOrders(), Order.class);
        checkApplyToManyToDb(entity.getOrdersStandalone(), Order.class);

        return __assignedId;
    }

}
