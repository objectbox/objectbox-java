package io.objectbox.tree;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@Entity
public final class MetaBranch {
    @Id
    long id;

    String name;
    String description;

    public ToOne<MetaBranch> parent;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ToOne<MetaBranch> getParent() {
        return parent;
    }

    public void setParent(ToOne<MetaBranch> parent) {
        this.parent = parent;
    }
}
