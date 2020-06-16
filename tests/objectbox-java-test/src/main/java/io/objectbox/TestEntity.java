/*
 * Copyright 2017 ObjectBox Ltd. All rights reserved.
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

/** In "real" entity would be annotated with @Entity. */
public class TestEntity {

    public static final String STRING_VALUE_THROW_IN_CONSTRUCTOR =
            "Hey constructor, please throw an exception. Thank you!";

    public static final String EXCEPTION_IN_CONSTRUCTOR_MESSAGE =
            "Hello, this is an exception from TestEntity constructor";

    /** In "real" entity would be annotated with @Id. */
    private long id;
    private boolean simpleBoolean;
    private byte simpleByte;
    private short simpleShort;
    private int simpleInt;
    private long simpleLong;
    private float simpleFloat;
    private double simpleDouble;
    /** Not-null value. */
    private String simpleString;
    /** Not-null value. */
    private byte[] simpleByteArray;
    /** In "real" entity would be annotated with @Unsigned. */
    private short simpleShortU;
    /** In "real" entity would be annotated with @Unsigned. */
    private int simpleIntU;
    /** In "real" entity would be annotated with @Unsigned. */
    private long simpleLongU;

    transient boolean noArgsConstructorCalled;

    public TestEntity() {
        noArgsConstructorCalled = true;
    }

    public TestEntity(long id) {
        this.id = id;
    }

    public TestEntity(long id, boolean simpleBoolean, byte simpleByte, short simpleShort, int simpleInt, long simpleLong, float simpleFloat, double simpleDouble, String simpleString, byte[] simpleByteArray, short simpleShortU, int simpleIntU, long simpleLongU) {
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
        this.simpleShortU = simpleShortU;
        this.simpleIntU = simpleIntU;
        this.simpleLongU = simpleLongU;
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

    /** Not-null value. */
    public String getSimpleString() {
        return simpleString;
    }

    /** Not-null value; ensure this value is available before it is saved to the database. */
    public void setSimpleString(String simpleString) {
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

    public short getSimpleShortU() {
        return simpleShortU;
    }

    public TestEntity setSimpleShortU(short simpleShortU) {
        this.simpleShortU = simpleShortU;
        return this;
    }

    public int getSimpleIntU() {
        return simpleIntU;
    }

    public TestEntity setSimpleIntU(int simpleIntU) {
        this.simpleIntU = simpleIntU;
        return this;
    }

    public long getSimpleLongU() {
        return simpleLongU;
    }

    public TestEntity setSimpleLongU(long simpleLongU) {
        this.simpleLongU = simpleLongU;
        return this;
    }
}
