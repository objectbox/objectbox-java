package io.objectbox.tree;

/**
 * (Potentially internal) value object created in our JNI layer to represent a leaf with all stored data.
 */
public class LeafNode {
    public long id;
    public long branchId;
    public long metaId;

    // Value properties; only one is actually set.
    public long integerValue;
    public double floatingValue;
    /** One of String, byte[], String[] */
    public Object objectValue;

    // Note: If we need the metaId only to figure out the type, we could also provide a value type property instead.
    // E.g. public int valueType;


    /**
     * All-args constructor used by JNI (don't change, it's actually used).
     */
    public LeafNode(long id, long branchId, long metaId, long integerValue, double floatingValue, Object objectValue) {
        this.id = id;
        this.branchId = branchId;
        this.metaId = metaId;
        this.integerValue = integerValue;
        this.floatingValue = floatingValue;
        this.objectValue = objectValue;
    }
}
