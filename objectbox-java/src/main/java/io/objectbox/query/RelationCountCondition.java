/*
 * Copyright 2022 ObjectBox Ltd.
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

import io.objectbox.relation.RelationInfo;

public class RelationCountCondition<T> extends QueryConditionImpl<T> {

    private final RelationInfo<T, ?> relationInfo;
    private final int relationCount;


    public RelationCountCondition(RelationInfo<T, ?> relationInfo, int relationCount) {
        this.relationInfo = relationInfo;
        this.relationCount = relationCount;
    }

    @Override
    void apply(QueryBuilder<T> builder) {
        builder.relationCount(relationInfo, relationCount);
    }
}
