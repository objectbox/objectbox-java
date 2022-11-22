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
