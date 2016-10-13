
package io.objectbox;

// Copied from generated tests (& removed some unused Properties)

/**
 * Properties for entity "TestEntity". Can be used for QueryBuilder and for referencing DB names.
 */
public class TestEntityProperties implements Properties {

    public static final String __NAME_IN_DB = "TestEntity";

    public final static Property Id = new Property(0, long.class, "id", true, "id");
    public final static Property SimpleInt = new Property(1, int.class, "simpleInt", false, "simpleInt");
//    public final static Property SimpleInteger = new Property(2, int.class, "simpleInteger", false, "simpleInteger");
//    public final static Property SimpleStringNotNull = new Property(3, String.class, "simpleStringNotNull", false, "simpleStringNotNull");
    public final static Property SimpleString = new Property(4, String.class, "simpleString", false, "simpleString");
//    public final static Property IndexedString = new Property(5, String.class, "indexedString", false, "indexedString");
//    public final static Property IndexedStringAscUnique = new Property(6, String.class, "indexedStringAscUnique", false, "indexedStringAscUnique");
//    public final static Property SimpleDate = new Property(7, java.util.Date.class, "simpleDate", false, "simpleDate");
    public final static Property SimpleBoolean = new Property(8, boolean.class, "simpleBoolean", false, "simpleBoolean");
    public final static Property SimpleByteArray = new Property(9, byte[].class, "simpleByteArray", false, "simpleByteArray");

    public final static Property[] __ALL_PROPERTIES = {
        Id,
        SimpleInt,
//        SimpleInteger,
//        SimpleStringNotNull,
        SimpleString,
//        IndexedString,
//        IndexedStringAscUnique,
//        SimpleDate,
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
