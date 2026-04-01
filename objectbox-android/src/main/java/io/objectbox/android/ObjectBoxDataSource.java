/*
 * Copyright 2018 ObjectBox Ltd. <https://objectbox.io>
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

package io.objectbox.android;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import androidx.paging.PositionalDataSource;
import io.objectbox.query.Query;
import io.objectbox.reactive.DataObserver;

/**
 * A {@link PositionalDataSource} that loads entities based on an ObjectBox {@link Query} using
 * offset and limit to implement paging support. The data source is invalidated if the query results
 * change.
 */
public class ObjectBoxDataSource<T> extends PositionalDataSource<T> {

    private final Query<T> query;
    @SuppressWarnings("FieldCanBeLocal")
    private final DataObserver<List<T>> observer;

    public static class Factory<Item> extends DataSource.Factory<Integer, Item> {

        private final Query<Item> query;

        public Factory(Query<Item> query) {
            this.query = query;
        }

        @NonNull
        @Override
        public DataSource<Integer, Item> create() {
            return new ObjectBoxDataSource<>(query);
        }
    }

    public ObjectBoxDataSource(Query<T> query) {
        this.query = query;
        this.observer = data -> {
            // if data changes invalidate this data source and create a new one
            invalidate();
        };
        // observer will be automatically removed once GC'ed
        query.subscribe().onlyChanges().weak().observer(observer);
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams params, @NonNull LoadInitialCallback<T> callback) {
        // note: limiting to int should be fine for Android apps
        int totalCount = (int) query.count();
        if (totalCount == 0) {
            callback.onResult(Collections.emptyList(), 0, 0);
            return;
        }

        int position = computeInitialLoadPosition(params, totalCount);
        int loadSize = computeInitialLoadSize(params, position, totalCount);

        List<T> list = loadRange(position, loadSize);
        if (list.size() == loadSize) {
            callback.onResult(list, position, totalCount);
        } else {
            invalidate(); // size doesn't match request - DB modified between count and load
        }
    }

    @Override
    public void loadRange(@NonNull LoadRangeParams params, @NonNull LoadRangeCallback<T> callback) {
        callback.onResult(loadRange(params.startPosition, params.loadSize));
    }

    private List<T> loadRange(int startPosition, int loadCount) {
        // note: find interprets loadCount 0 as no limit
        return query.find(startPosition, loadCount);
    }

}
