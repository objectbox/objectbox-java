/*
 * Copyright 2017-2025 ObjectBox Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.objectbox;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.ExternalPropertyType;
import io.objectbox.annotation.ExternalType;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Unsigned;

/**
 * The annotations in this class have no effect as the Gradle plugin is not configured in this project. However, test
 * code builds a model like if the annotations were processed.
 * <p>
 * There is a matching test in the internal integration test project where this is tested and model builder code can be
 * "stolen" from.
 */
@Entity
public class TestEntity {

    public static final String STRING_VALUE_THROW_IN_CONSTRUCTOR =
            "Hey constructor, please throw an exception. Thank you!";

    public static final String EXCEPTION_IN_CONSTRUCTOR_MESSAGE =
            "Hello, this is an exception from TestEntity constructor";

    @Id
    private long id;
    private boolean simpleBoolean;
    private byte simpleByte;
    private short simpleShort;
    private int simpleInt;
    private long simpleLong;
    private float simpleFloat;
    private double simpleDouble;
    private String simpleString;
    /** Not-null value. */
    private byte[] simpleByteArray;
    /** Not-null value. */
    private String[] simpleStringArray;
    private List<String> simpleStringList;
    @Unsigned
    private short simpleShortU;
    @Unsigned
    private int simpleIntU;
    @Unsigned
    private long simpleLongU;
    private Map<String, Object> stringObjectMap;
    private Object flexProperty;
    private boolean[] booleanArray;
    private short[] shortArray;
    private char[] charArray;
    private int[] intArray;
    private long[] longArray;
    private float[] floatArray;
    private double[] doubleArray;
    private Date date;
    // Just smoke testing this property type (tests do not use Sync).
    // Also use UUID instead of the default MONGO_ID.
    @ExternalType(ExternalPropertyType.UUID)
    private byte[] externalId;
    // Just smoke testing this property type (tests do not use Sync).
    @ExternalType(ExternalPropertyType.JSON_TO_NATIVE)
    private String externalJsonToNative;

    transient boolean noArgsConstructorCalled;

    public TestEntity() {
        noArgsConstructorCalled = true;
    }

    public TestEntity(long id) {
        this.id = id;
    }

    public TestEntity(long id,
                      boolean simpleBoolean,
                      byte simpleByte,
                      short simpleShort,
                      int simpleInt,
                      long simpleLong,
                      float simpleFloat,
                      double simpleDouble,
                      String simpleString,
                      byte[] simpleByteArray,
                      String[] simpleStringArray,
                      List<String> simpleStringList,
                      short simpleShortU,
                      int simpleIntU,
                      long simpleLongU,
                      Map<String, Object> stringObjectMap,
                      Object flexProperty,
                      boolean[] booleanArray,
                      short[] shortArray,
                      char[] charArray,
                      int[] intArray,
                      long[] longArray,
                      float[] floatArray,
                      double[] doubleArray,
                      Date date,
                      byte[] externalId,
                      String externalJsonToNative
    ) {
        this.id = id;
        this.simpleBoolean = simpleBoolean;
        this.simpleByte = simpleByte;
        this.simpleShort = simpleShort;
        this.simpleInt = simpleInt;
        this.simpleLong = simpleLong;
        this.simpleFloat = simpleFloat;
        this.simpleDouble = simpleDouble;
        this.simpleString = simpleString;
        this.simpleByteArray = simpleByteArray;
        this.simpleStringArray = simpleStringArray;
        this.simpleStringList = simpleStringList;
        this.simpleShortU = simpleShortU;
        this.simpleIntU = simpleIntU;
        this.simpleLongU = simpleLongU;
        this.stringObjectMap = stringObjectMap;
        this.flexProperty = flexProperty;
        this.booleanArray = booleanArray;
        this.shortArray = shortArray;
        this.charArray = charArray;
        this.intArray = intArray;
        this.longArray = longArray;
        this.floatArray = floatArray;
        this.doubleArray = doubleArray;
        this.date = date;
        this.externalId = externalId;
        this.externalJsonToNative = externalJsonToNative;
        if (STRING_VALUE_THROW_IN_CONSTRUCTOR.equals(simpleString)) {
            throw new RuntimeException(EXCEPTION_IN_CONSTRUCTOR_MESSAGE);
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean getSimpleBoolean() {
        return simpleBoolean;
    }

    public void setSimpleBoolean(boolean simpleBoolean) {
        this.simpleBoolean = simpleBoolean;
    }

    public byte getSimpleByte() {
        return simpleByte;
    }

    public void setSimpleByte(byte simpleByte) {
        this.simpleByte = simpleByte;
    }

    public short getSimpleShort() {
        return simpleShort;
    }

    public void setSimpleShort(short simpleShort) {
        this.simpleShort = simpleShort;
    }

    public int getSimpleInt() {
        return simpleInt;
    }

    public void setSimpleInt(int simpleInt) {
        this.simpleInt = simpleInt;
    }

    public long getSimpleLong() {
        return simpleLong;
    }

    public void setSimpleLong(long simpleLong) {
        this.simpleLong = simpleLong;
    }

    public float getSimpleFloat() {
        return simpleFloat;
    }

    public void setSimpleFloat(float simpleFloat) {
        this.simpleFloat = simpleFloat;
    }

    public double getSimpleDouble() {
        return simpleDouble;
    }

    public void setSimpleDouble(double simpleDouble) {
        this.simpleDouble = simpleDouble;
    }

    @Nullable
    public String getSimpleString() {
        return simpleString;
    }

    public void setSimpleString(@Nullable String simpleString) {
        this.simpleString = simpleString;
    }

    /** Not-null value. */
    public byte[] getSimpleByteArray() {
        return simpleByteArray;
    }

    /** Not-null value; ensure this value is available before it is saved to the database. */
    public void setSimpleByteArray(byte[] simpleByteArray) {
        this.simpleByteArray = simpleByteArray;
    }

    /** Not-null value. */
    public String[] getSimpleStringArray() {
        return simpleStringArray;
    }

    /** Not-null value; ensure this value is available before it is saved to the database. */
    public void setSimpleStringArray(String[] simpleStringArray) {
        this.simpleStringArray = simpleStringArray;
    }

    public List<String> getSimpleStringList() {
        return simpleStringList;
    }

    public void setSimpleStringList(List<String> simpleStringList) {
        this.simpleStringList = simpleStringList;
    }

    public short getSimpleShortU() {
        return simpleShortU;
    }

    public void setSimpleShortU(short simpleShortU) {
        this.simpleShortU = simpleShortU;
    }

    public int getSimpleIntU() {
        return simpleIntU;
    }

    public void setSimpleIntU(int simpleIntU) {
        this.simpleIntU = simpleIntU;
    }

    public long getSimpleLongU() {
        return simpleLongU;
    }

    public void setSimpleLongU(long simpleLongU) {
        this.simpleLongU = simpleLongU;
    }

    public Map<String, Object> getStringObjectMap() {
        return stringObjectMap;
    }

    public void setStringObjectMap(Map<String, Object> stringObjectMap) {
        this.stringObjectMap = stringObjectMap;
    }

    @Nullable
    public Object getFlexProperty() {
        return flexProperty;
    }

    public void setFlexProperty(@Nullable Object flexProperty) {
        this.flexProperty = flexProperty;
    }

    @Nullable
    public boolean[] getBooleanArray() {
        return booleanArray;
    }

    public void setBooleanArray(@Nullable boolean[] booleanArray) {
        this.booleanArray = booleanArray;
    }

    @Nullable
    public short[] getShortArray() {
        return shortArray;
    }

    public void setShortArray(@Nullable short[] shortArray) {
        this.shortArray = shortArray;
    }

    @Nullable
    public char[] getCharArray() {
        return charArray;
    }

    public void setCharArray(@Nullable char[] charArray) {
        this.charArray = charArray;
    }

    @Nullable
    public int[] getIntArray() {
        return intArray;
    }

    public void setIntArray(@Nullable int[] intArray) {
        this.intArray = intArray;
    }

    @Nullable
    public long[] getLongArray() {
        return longArray;
    }

    public void setLongArray(@Nullable long[] longArray) {
        this.longArray = longArray;
    }

    @Nullable
    public float[] getFloatArray() {
        return floatArray;
    }

    public void setFloatArray(@Nullable float[] floatArray) {
        this.floatArray = floatArray;
    }

    @Nullable
    public double[] getDoubleArray() {
        return doubleArray;
    }

    public void setDoubleArray(@Nullable double[] doubleArray) {
        this.doubleArray = doubleArray;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Nullable
    public byte[] getExternalId() {
        return externalId;
    }

    public void setExternalId(@Nullable byte[] externalId) {
        this.externalId = externalId;
    }

    @Nullable
    public String getExternalJsonToNative() {
        return externalJsonToNative;
    }

    public void setExternalJsonToNative(@Nullable String externalJsonToNative) {
        this.externalJsonToNative = externalJsonToNative;
    }

    @Override
    public String toString() {
        return "TestEntity{" +
                "id=" + id +
                ", simpleBoolean=" + simpleBoolean +
                ", simpleByte=" + simpleByte +
                ", simpleShort=" + simpleShort +
                ", simpleInt=" + simpleInt +
                ", simpleLong=" + simpleLong +
                ", simpleFloat=" + simpleFloat +
                ", simpleDouble=" + simpleDouble +
                ", simpleString='" + simpleString + '\'' +
                ", simpleByteArray=" + Arrays.toString(simpleByteArray) +
                ", simpleStringArray=" + Arrays.toString(simpleStringArray) +
                ", simpleStringList=" + simpleStringList +
                ", simpleShortU=" + simpleShortU +
                ", simpleIntU=" + simpleIntU +
                ", simpleLongU=" + simpleLongU +
                ", stringObjectMap=" + stringObjectMap +
                ", flexProperty=" + flexProperty +
                ", booleanArray=" + Arrays.toString(booleanArray) +
                ", shortArray=" + Arrays.toString(shortArray) +
                ", charArray=" + Arrays.toString(charArray) +
                ", intArray=" + Arrays.toString(intArray) +
                ", longArray=" + Arrays.toString(longArray) +
                ", floatArray=" + Arrays.toString(floatArray) +
                ", doubleArray=" + Arrays.toString(doubleArray) +
                ", date=" + date +
                ", externalId=" + Arrays.toString(externalId) +
                ", externalJsonToString='" + externalJsonToNative + '\'' +
                ", noArgsConstructorCalled=" + noArgsConstructorCalled +
                '}';
    }
}
