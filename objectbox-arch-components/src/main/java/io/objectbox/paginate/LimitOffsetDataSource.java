package io.objectbox.paginate;


import android.arch.paging.PositionalDataSource;
import android.support.annotation.NonNull;
import io.objectbox.Box;
import io.objectbox.query.Query;
import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.DataSubscription;

import java.util.Collections;
import java.util.List;

/**
 * Basic implementation of #PositionalDataSource that uses offset and limit to paginate data
 *
 * @param <T> Type of items returned by DataSource
 * @author Ekalips
 */
class LimitOffsetDataSource<T> extends PositionalDataSource<T> {

    private Query<T> query;
    private DataSubscription dataSubscription;

    /**
     * @param box Used to extract basic query (box.query().build() as data source
     * @see #LimitOffsetDataSource(Query)
     */
    public LimitOffsetDataSource(Box<T> box) {
        this(box.query().build());
    }

    /**
     * Constructs an #LimitOffsetDataSource with #Query as data source and subscribes to query changes
     *
     * @param query Query that is used as data source
     */
    public LimitOffsetDataSource(Query<T> query) {
        this.query = query;

        // We need `onlyChanges()` because this DataSource will get recreated on each invalidate() call, so we basically not interested in initial data response
        // (otherwise it'll get recreated each time it constructs
        dataSubscription = query.subscribe().onlyChanges().weak().observer(new DataObserver<List<T>>() {
            @Override
            public void onData(List<T> data) {
                invalidate();
            }
        });
    }

    /**
     * Count number of items query can return
     */
    private int countItems() {
        return (int) query.count();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        dataSubscription.cancel();
    }


    /**
     * Return the rows from startPos to startPos + loadCount
     */
    @NonNull
    private List<T> loadRange(int startPos, int loadCount) {
        return query.find(startPos, loadCount);
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams params, @NonNull LoadInitialCallback<T> callback) {
        int totalCount = countItems();
        if (totalCount == 0) {
            callback.onResult(Collections.<T>emptyList(), 0, 0);
            return;
        }

        // bound the size requested, based on known count
        int firstLoadPosition = PositionalDataSource.computeInitialLoadPosition(params, totalCount);
        int firstLoadSize = PositionalDataSource.computeInitialLoadSize(params, firstLoadPosition, totalCount);
        List<T> list = loadRange(firstLoadPosition, firstLoadSize);
        if (list.size() == firstLoadSize) {
            callback.onResult(list, firstLoadPosition, totalCount);
        } else {
            // This means that data was changed during load process
            invalidate();
        }
    }

    @Override
    public void loadRange(@NonNull LoadRangeParams params, @NonNull LoadRangeCallback<T> callback) {
        List<T> list = loadRange(params.startPosition, params.loadSize);
        callback.onResult(list);
    }
}