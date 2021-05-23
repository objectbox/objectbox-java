package io.objectbox.tree;

import io.objectbox.annotation.ConflictStrategy;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Unique;
import io.objectbox.relation.ToOne;

@Entity
public final class DataBranch {
    @Id
    long id;

    @Unique(onConflict = ConflictStrategy.REPLACE)
    String uid;

    public ToOne<DataBranch> parent;
    public ToOne<MetaBranch> metaBranch;

    public String toString() {
        return "DataBranch(id=" + this.id + ", uid=" + this.uid + ')';
    }


}
