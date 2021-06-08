package io.objectbox.tree;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Unsigned;
import io.objectbox.relation.ToOne;

@Entity
public final class MetaLeaf {
    @Id
    long id;

    String name;
    String description;

    @Unsigned
    private int flags;

    @Unsigned
    private short valueType;

    String[] valueEnum;
    String valueUnit;
    String valueMin;
    String valueMax;

    public ToOne<MetaBranch> branch;

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

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public short getValueType() {
        return valueType;
    }

    public void setValueType(short valueType) {
        this.valueType = valueType;
    }

    public String[] getValueEnum() {
        return valueEnum;
    }

    public void setValueEnum(String[] valueEnum) {
        this.valueEnum = valueEnum;
    }

    public String getValueUnit() {
        return valueUnit;
    }

    public void setValueUnit(String valueUnit) {
        this.valueUnit = valueUnit;
    }

    public String getValueMin() {
        return valueMin;
    }

    public void setValueMin(String valueMin) {
        this.valueMin = valueMin;
    }

    public String getValueMax() {
        return valueMax;
    }

    public void setValueMax(String valueMax) {
        this.valueMax = valueMax;
    }

    public ToOne<MetaBranch> getBranch() {
        return branch;
    }

    public void setBranch(ToOne<MetaBranch> branch) {
        this.branch = branch;
    }
}
