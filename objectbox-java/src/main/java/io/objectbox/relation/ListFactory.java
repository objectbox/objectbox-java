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

package io.objectbox.relation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.objectbox.annotation.apihint.Experimental;

@Experimental
public interface ListFactory extends Serializable {
    <T> List<T> createList();

    class ArrayListFactory implements ListFactory {
        private static final long serialVersionUID = 8247662514375611729L;

        @Override
        public <T> List<T> createList() {
            return new ArrayList<>();
        }
    }

    class CopyOnWriteArrayListFactory implements ListFactory {
        private static final long serialVersionUID = 1888039726372206411L;

        @Override
        public <T> List<T> createList() {
            return new CopyOnWriteArrayList<>();
        }
    }
}
