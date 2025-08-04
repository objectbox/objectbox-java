/*
 * Copyright 2017 ObjectBox Ltd.
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

package io.objectbox;

import java.util.concurrent.Callable;

import javax.annotation.Nullable;

/**
 * Callback to be used for {@link BoxStore#runInTxAsync(Runnable, TxCallback)} and
 * {@link BoxStore#callInTxAsync(Callable, TxCallback)}.
 */
public interface TxCallback<T> {
    /**
     * Called when an asynchronous transaction finished.
     *
     * @param result Result of the callable {@link BoxStore#callInTxAsync(Callable, TxCallback)}
     * @param error  non-null if an exception was thrown
     */
    void txFinished(@Nullable T result, @Nullable Throwable error);
}
