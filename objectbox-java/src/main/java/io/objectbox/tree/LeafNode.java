package io.objectbox.tree;

/**
 * (Potentially internal) value object created in our JNI layer to represent a leaf with all stored data.
 * Note that only one of the value properties is actually set for any node.
 */
public class LeafNode {
    public long id;
    public long branchId;
    public long metaId;

    public long integerValue;
    public double floatingValue;

    /**
     * One of String, byte[], String[]
     */
    public Object objectValue;

    /**
     * See {@link io.objectbox.model.PropertyType} for values.
     * Attention: does not represent the type accurately yet:
     * 1) Strings are Bytes, 2) all integer type are Long, 3) all FPs are Double.
     */
    public short valueType;

    /**
     * All-args constructor used by JNI (don't change, it's actually used).
     */
    public LeafNode(long id, long branchId, long metaId, long integerValue, double floatingValue, Object objectValue,
                    short valueType) {
        this.id = id;
        this.branchId = branchId;
        this.metaId = metaId;
        this.integerValue = integerValue;
        this.floatingValue = floatingValue;
        this.objectValue = objectValue;
        this.valueType = valueType;
    }
}
