package io.objectbox.tree;

import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.model.PropertyType;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;

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

    public boolean isBytes() {
        return node.valueType == PropertyType.ByteVector;
    }

    public boolean isStringArray() {
        return node.valueType == PropertyType.ShortVector;
    }

    // valueInt
    public long getInt() {
        verifyIsInt();
        return node.integerValue;
    }

    public void verifyIsInt() {
        if (!isInt()) throw new IllegalStateException("value is not integer (" + node.valueType + ")");
    }

    // valueDouble
    public double getDouble() {
        verifyIsDouble();
        return node.floatingValue;
    }

    public void verifyIsDouble() {
        if (!isDouble()) throw new IllegalStateException("value is not floating point (" + node.valueType + ")");
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

    public void verifyIsString() {
        if (!isString()) throw new IllegalStateException("value is not string (" + node.valueType + ")");
    }

    // valueStrings
    @Nullable
    public String[] getStringArray() {
        verifyIsStringArray();
        return (String[]) node.objectValue;
    }

    public void verifyIsStringArray() {
        if (!isStringArray()) throw new IllegalStateException("value is not string array");
    }

    @Nullable
    public Long asInt() {
        if (isInt()) return getInt();

        if (isDouble()) {
            return (long) getDouble();
        }
        if (isString()) {
            String value = getString();
            return value != null ? Long.valueOf(value) : null;
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
            return value != null ? Double.valueOf(value) : null;
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
