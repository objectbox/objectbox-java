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

package io.objectbox.query;

import java.util.List;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.reactive.SubscriptionBuilder;


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockQuery<T> {
    private Box<T> box;
    private BoxStore boxStore;
    private final Query<T> query;
    private final FakeQueryPublisher<T> fakeQueryPublisher;

    public MockQuery(boolean hasOrder) {
        // box = mock(Box.class);
        // boxStore = mock(BoxStore.class);
        // when(box.getStore()).thenReturn(boxStore);

        //noinspection unchecked It's a unit test, casting is fine.
        query = (Query<T>) mock(Query.class);
        fakeQueryPublisher = new FakeQueryPublisher<>();
        //noinspection ConstantConditions ExecutorService only used for transforms.
        SubscriptionBuilder<List<T>> subscriptionBuilder = new SubscriptionBuilder<>(
                fakeQueryPublisher, null, null);
        when(query.subscribe()).thenReturn(subscriptionBuilder);
    }

    public Box<T> getBox() {
        return box;
    }

    public BoxStore getBoxStore() {
        return boxStore;
    }

    public Query<T> getQuery() {
        return query;
    }

    public FakeQueryPublisher<T> getFakeQueryPublisher() {
        return fakeQueryPublisher;
    }
}
