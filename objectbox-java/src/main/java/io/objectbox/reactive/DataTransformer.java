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

package io.objectbox.reactive;

import javax.annotation.Nullable;

/**
 * Transforms or processes data before it is given to subscribed {@link DataObserver}s. A transformer is set via
 * {@link SubscriptionBuilder#transform(DataTransformer)}.
 *
 * Note that a transformer is not required to actually "transform" any data.
 * Technically, it's fine to return the same data it received and just do some processing with it.
 *
 * Threading notes: Note that the transformer is always executed asynchronously.
 * It is OK to perform long lasting operations.
 *
 * @param <FROM> Data type this transformer receives
 * @param <TO> Type of transformed data
 */
public interface DataTransformer<FROM, TO> {
    /**
     * Transforms/processes the given data.
     * @param source data to be transformed
     * @return transformed data
     * @throws Exception Transformers may throw any exceptions, which can be reacted on via
     * {@link SubscriptionBuilder#onError(ErrorObserver)}.
     */
    TO transform(FROM source) throws Exception;
}
