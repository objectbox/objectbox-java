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

package io.objectbox.rx;

import io.objectbox.BoxStore;
import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.DataSubscription;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Cancellable;

/**
 * Static methods to Rx-ify ObjectBox queries.
 */
public abstract class RxBoxStore {
    /**
     * Using the returned Observable, you can be notified about data changes.
     * Once a transaction is committed, you will get info on classes with changed Objects.
     */
    public static <T> Observable<Class> observable(final BoxStore boxStore) {
        return Observable.create(new ObservableOnSubscribe<Class>() {
            @Override
            public void subscribe(final ObservableEmitter<Class> emitter) throws Exception {
                final DataSubscription dataSubscription = boxStore.subscribe().observer(new DataObserver<Class>() {
                    @Override
                    public void onData(Class data) {
                        if (!emitter.isDisposed()) {
                            emitter.onNext(data);
                        }
                    }
                });
                emitter.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        dataSubscription.cancel();
                    }
                });
            }
        });
    }

}
