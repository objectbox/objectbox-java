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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.reactive.SubscriptionBuilder;

public class MockQuery<T> {
    private Box box;
    private BoxStore boxStore;
    private final Query query;
    private final FakeQueryPublisher fakeQueryPublisher;

    public MockQuery(boolean hasOrder) {
        // box = mock(Box.class);
        // boxStore = mock(BoxStore.class);
        // when(box.getStore()).thenReturn(boxStore);
         query = mock(Query.class);
        fakeQueryPublisher = new FakeQueryPublisher();
        SubscriptionBuilder subscriptionBuilder = new SubscriptionBuilder(fakeQueryPublisher, null, null);
        when(query.subscribe()).thenReturn(subscriptionBuilder);
    }

    public Box getBox() {
        return box;
    }

    public BoxStore getBoxStore() {
        return boxStore;
    }

    public Query getQuery() {
        return query;
    }

    public FakeQueryPublisher getFakeQueryPublisher() {
        return fakeQueryPublisher;
    }
}
