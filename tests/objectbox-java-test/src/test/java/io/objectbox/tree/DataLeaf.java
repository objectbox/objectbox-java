package io.objectbox.tree;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

import java.util.Arrays;

@Entity
public final class DataLeaf {
    @Id
    long id;
    long valueInt;
    double valueDouble;
    String valueString;
    String[] valueStrings;
    public ToOne<DataBranch> dataBranch;
    public ToOne<MetaLeaf> metaLeaf;

    public DataLeaf(long id, long valueInt, double valueDouble, String valueString, String[] valueStrings) {
        this.id = id;
        this.valueInt = valueInt;
        this.valueDouble = valueDouble;
        this.valueString = valueString;
        this.valueStrings = valueStrings;
    }

    public final long getId() {
        return this.id;
    }

    public final void setId(long id) {
        this.id = id;
    }

    public final long getValueInt() {
        return this.valueInt;
    }

    public final void setValueInt(long valueInt) {
        this.valueInt = valueInt;
    }

    public final double getValueDouble() {
        return this.valueDouble;
    }

    public final void setValueDouble(double valueDouble) {
        this.valueDouble = valueDouble;
    }

    public final String getValueString() {
        return this.valueString;
    }

    public final void setValueString(String valueString) {
        this.valueString = valueString;
    }

    public final String[] getValueStrings() {
        return this.valueStrings;
    }

    public final void setValueStrings(String[] valueStrings) {
        this.valueStrings = valueStrings;
    }

    public ToOne<DataBranch> getDataBranch() {
        return dataBranch;
    }

    public void setDataBranch(ToOne<DataBranch> dataBranch) {
        this.dataBranch = dataBranch;
    }

    public ToOne<MetaLeaf> getMetaLeaf() {
        return metaLeaf;
    }

    public void setMetaLeaf(ToOne<MetaLeaf> metaLeaf) {
        this.metaLeaf = metaLeaf;
    }

    public String toString() {
        return "DataAttribute(id=" + this.id + ", valueInt=" + this.valueInt + ", valueDouble=" + this.valueDouble + ", valueString=" + this.valueString + ", valueStrings=" + Arrays.toString(this.valueStrings) + ')';
    }

    public int hashCode() {
        int result = Long.hashCode(this.id);
        result = result * 31 + Long.hashCode(this.valueInt);
        result = result * 31 + Double.hashCode(this.valueDouble);
        result = result * 31 + this.valueString.hashCode();
        result = result * 31 + Arrays.hashCode(this.valueStrings);
        return result;
    }

}
