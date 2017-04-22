package io.objectbox.relation;

import io.objectbox.BoxStore;
import io.objectbox.Cursor;
import io.objectbox.EntityInfo;
import io.objectbox.Transaction;

// THIS CODE IS ADAPTED from generated resources of the test-entity-annotations project

/**
 * Cursor for DB entity "ORDERS".
 */
public final class OrderCursor extends Cursor<Order> {

    private static EntityInfo PROPERTIES = new Order_();

    // TODO private Query<Order> customer_OrdersQuery;

    // Property IDs get verified in Cursor base class
    private final static int __ID_date = Order_.date.id;
    private final static int __ID_customerId = Order_.customerId.id;
    private final static int __ID_text = Order_.text.id;

    public OrderCursor(Transaction tx, long cursor, BoxStore boxStore) {
        super(tx, cursor, PROPERTIES, boxStore);
    }

    @Override
    public final long getId(Order entity) {
        return entity.getId();
    }

    /**
     * Puts an object into its box.
     *
     * @return The ID of the object within its box.
     */
    @Override
    public final long put(Order entity) {
        String text = entity.getText();
        int __id3 = text != null ? __ID_text : 0;
        java.util.Date date = entity.getDate();
        int __id1 = date != null ? __ID_date : 0;

        long __assignedId = collect313311(cursor, entity.getId(), PUT_FLAG_FIRST | PUT_FLAG_COMPLETE,
                __id3, text, 0, null,
                0, null, 0, null,
                __ID_customerId, entity.getCustomerId(), __id1, __id1 != 0 ? date.getTime() : 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0);

        entity.setId(__assignedId);
        entity.__boxStore = boxStoreForEntities;
        return __assignedId;
    }

    // TODO @Override
    protected final void attachEntity(Order entity) {
        // TODO super.attachEntity(entity);
        entity.__boxStore = boxStoreForEntities;
    }

    // TODO do we need this? @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }

    /** Internal query to resolve the "orders" to-many relationship of Customer. */
    /* TODO
    public List<Order> _queryCustomer_Orders(long customerId) {
        synchronized (this) {
            if (customer_OrdersQuery == null) {
                QueryBuilder<Order> queryBuilder = queryBuilder();
                queryBuilder.where(Properties.customerId.eq(null));
                customer_OrdersQuery = queryBuilder.build();
            }
        }
        Query<Order> query = customer_OrdersQuery.forCurrentThread();
        query.setParameter(0, customerId);
        return query.list();
    }
    */

}
