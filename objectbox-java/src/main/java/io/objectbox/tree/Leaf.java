package io.objectbox.tree;

import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.model.PropertyType;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;

/**
 * A data leaf represents a data value in a {@link Tree} as a child of a {@link Branch}.
 * Each data value has a specific type, e.g. an int or a String.
 */
@Experimental
public class Leaf {

    private final LeafNode node;

    public Leaf(LeafNode node) {
        this.node = node;
    }

    public long getId() {
        return node.id;
    }

    public long getParentBranchId() {
        return node.branchId;
    }

    public long getMetaId() {
        return node.metaId;
    }

    /** See {@link PropertyType} for possible types (not all are used here). */
    public short getValueType() {
        return node.valueType;
    }

    public boolean isInt() {
        return node.valueType == PropertyType.Long;
    }

    public boolean isDouble() {
        return node.valueType == PropertyType.Double;
    }

    public boolean isString() {
        return node.valueType == PropertyType.ByteVector;
    }

    public boolean isStringArray() {
        return node.valueType == PropertyType.ShortVector;
    }

    private void verifyIsInt() {
        if (!isInt()) throw new IllegalStateException("value is not integer (" + node.valueType + ")");
    }

    private void verifyIsDouble() {
        if (!isDouble()) throw new IllegalStateException("value is not floating point (" + node.valueType + ")");
    }

    private void verifyIsString() {
        if (!isString()) throw new IllegalStateException("value is not string (" + node.valueType + ")");
    }

    private void verifyIsStringArray() {
        if (!isStringArray()) throw new IllegalStateException("value is not string array");
    }

    // valueInt
    public long getInt() {
        verifyIsInt();
        return node.integerValue;
    }

    // valueDouble
    public double getDouble() {
        verifyIsDouble();
        return node.floatingValue;
    }

    // valueString
    @Nullable
    public String getString() {
        verifyIsString();
        if (node.objectValue instanceof String) {
            return (String) node.objectValue;
        } else {
            byte[] bytes = (byte[]) node.objectValue;
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    // valueStrings
    @Nullable
    public String[] getStringArray() {
        verifyIsStringArray();
        return (String[]) node.objectValue;
    }

    @Nullable
    public Long asInt() {
        if (isInt()) return getInt();

        if (isDouble()) {
            return (long) getDouble();
        }
        if (isString()) {
            String value = getString();
            try {
                return value != null ? Long.valueOf(value) : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    @Nullable
    public Double asDouble() {
        if (isDouble()) return getDouble();

        if (isInt()) {
            return (double) getInt();
        }
        if (isString()) {
            String value = getString();
            try {
                return value != null ? Double.valueOf(value) : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    @Nullable
    public String asString() {
        if (isString()) return getString();

        if (isInt()) {
            return String.valueOf(getInt());
        }
        if (isDouble()) {
            return String.valueOf(getDouble());
        }
        if (isStringArray()) {
            String[] value = getStringArray();
            if (value == null) return null;
            return String.join(", ", value);
        }

        return null;
    }

    @Nullable
    public String[] asStringArray() {
        if (isStringArray()) return getStringArray();

        String value = asString();
        return value != null ? new String[]{value} : null;
    }

    public void setInt(long value) {
        verifyIsInt();
        node.integerValue = value;
    }

    public void setDouble(double value) {
        verifyIsDouble();
        node.floatingValue = value;
    }

    public void setString(@Nullable String value) {
        verifyIsString();
        node.objectValue = value;
    }

    public void setStringArray(@Nullable String[] value) {
        verifyIsStringArray();
        node.objectValue = value;
    }

}
