
package io.objectbox;

// Copied from generated tests (& removed some unused Properties)

/**
 * Properties for entity "TestEntity". Can be used for QueryBuilder and for referencing DB names.
 */
public class TestEntityProperties implements Properties {

    public static final String __NAME_IN_DB = "TestEntity";

    private static int ID;

    public final static Property Id = new Property(ID++, ID, long.class, "id", true, "id");
    public final static Property SimpleBoolean = new Property(ID++, ID, boolean.class, "simpleBoolean", false, "simpleBoolean");
    public final static Property SimpleByte = new Property(ID++, ID, byte.class, "simpleByte", false, "simpleByte");
    public final static Property SimpleShort = new Property(ID++, ID, short.class, "simpleShort", false, "simpleShort");
    public final static Property SimpleInt = new Property(ID++, ID, int.class, "simpleInt", false, "simpleInt");
    public final static Property SimpleLong = new Property(ID++, ID, long.class, "simpleLong", false, "simpleLong");
    public final static Property SimpleFloat = new Property(ID++, ID, float.class, "simpleFloat", false, "simpleFloat");
    public final static Property SimpleDouble = new Property(ID++, ID, double.class, "simpleDouble", false, "simpleDouble");
    public final static Property SimpleString = new Property(ID++, ID, String.class, "simpleString", false, "simpleString");
    public final static Property SimpleByteArray = new Property(ID++, ID, byte[].class, "simpleByteArray", false, "simpleByteArray");

    public final static Property[] __ALL_PROPERTIES = {
            Id,
            SimpleInt,
            SimpleShort,
            SimpleLong,
            SimpleString,
            SimpleFloat,
            SimpleBoolean,
            SimpleByteArray
    };

    public final static Property __ID_PROPERTY = Id;

    @Override
    public Property[] getAllProperties() {
        return __ALL_PROPERTIES;
    }

    @Override
    public Property getIdProperty() {
        return __ID_PROPERTY;
    }

    @Override
    public String getDbName() {
        return __NAME_IN_DB;
    }

}
