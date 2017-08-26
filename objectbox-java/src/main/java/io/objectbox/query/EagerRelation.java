package io.objectbox.query;

import io.objectbox.relation.RelationInfo;

class EagerRelation {
    public final int limit;
    public final RelationInfo relationInfo;

    EagerRelation(int limit, RelationInfo relationInfo) {
        this.limit = limit;
        this.relationInfo = relationInfo;
    }
}
