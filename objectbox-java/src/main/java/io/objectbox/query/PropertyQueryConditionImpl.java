/*
 * Copyright 2020-2025 ObjectBox Ltd.
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

package io.objectbox.query;

import java.util.Date;

import io.objectbox.Property;
import io.objectbox.query.QueryBuilder.StringOrder;

/**
 * {@link Property} based query conditions with implementations split by number and type of values,
 * such as {@link LongCondition LongCondition}, {@link LongLongCondition LongLongCondition},
 * {@link LongArrayCondition LongArrayCondition} and the general {@link NullCondition NullCondition}.
 * <p>
 * Each condition implementation has a set of operation enums, e.g. EQUAL/NOT_EQUAL/..., which represent the actual
 * query condition passed to the native query builder.
 */
public abstract class PropertyQueryConditionImpl<T> extends QueryConditionImpl<T> implements PropertyQueryCondition<T> {
    // Note: Expose for DAOcompat
    public final Property<T> property;
    private String alias;

    PropertyQueryConditionImpl(Property<T> property) {
        this.property = property;
    }

    @Override
    public QueryCondition<T> alias(String name) {
        this.alias = name;
        return this;
    }

    // Note: Expose for DAOcompat
    @Override
    public void apply(QueryBuilder<T> builder) {
        applyCondition(builder);
        if (alias != null && alias.length() != 0) {
            builder.parameterAlias(alias);
        }
    }

    abstract void applyCondition(QueryBuilder<T> builder);

    public static class NullCondition<T> extends PropertyQueryConditionImpl<T> {
        private final Operation op;

        public enum Operation {
            IS_NULL,
            NOT_NULL
        }

        public NullCondition(Property<T> property, Operation op) {
            super(property);
            this.op = op;
        }

        @Override
        void applyCondition(QueryBuilder<T> builder) {
            switch (op) {
                case IS_NULL:
                    builder.isNull(property);
                    break;
                case NOT_NULL:
                    builder.notNull(property);
                    break;
                default:
                    throw new UnsupportedOperationException(op + " is not supported");
            }
        }
    }

    public static class IntArrayCondition<T> extends PropertyQueryConditionImpl<T> {
        private final Operation op;
        private final int[] value;

        public enum Operation {
            IN,
            NOT_IN
        }

        public IntArrayCondition(Property<T> property, Operation op, int[] value) {
            super(property);
            this.op = op;
            this.value = value;
        }

        @Override
        void applyCondition(QueryBuilder<T> builder) {
            switch (op) {
                case IN:
                    builder.in(property, value);
                    break;
                case NOT_IN:
                    builder.notIn(property, value);
                    break;
                default:
                    throw new UnsupportedOperationException(op + " is not supported for int[]");
            }
        }
    }

    public static class LongCondition<T> extends PropertyQueryConditionImpl<T> {
        private final Operation op;
        private final long value;

        public enum Operation {
            EQUAL,
            NOT_EQUAL,
            GREATER,
            GREATER_OR_EQUAL,
            LESS,
            LESS_OR_EQUAL
        }

        public LongCondition(Property<T> property, Operation op, long value) {
            super(property);
            this.op = op;
            this.value = value;
        }

        public LongCondition(Property<T> property, Operation op, boolean value) {
            this(property, op, value ? 1 : 0);
        }

        public LongCondition(Property<T> property, Operation op, Date value) {
            this(property, op, value.getTime());
        }

        @Override
        void applyCondition(QueryBuilder<T> builder) {
            switch (op) {
                case EQUAL:
                    builder.equal(property, value);
                    break;
                case NOT_EQUAL:
                    builder.notEqual(property, value);
                    break;
                case GREATER:
                    builder.greater(property, value);
                    break;
                case GREATER_OR_EQUAL:
                    builder.greaterOrEqual(property, value);
                    break;
                case LESS:
                    builder.less(property, value);
                    break;
                case LESS_OR_EQUAL:
                    builder.lessOrEqual(property, value);
                    break;
                default:
                    throw new UnsupportedOperationException(op + " is not supported for String");
            }
        }
    }

    public static class LongLongCondition<T> extends PropertyQueryConditionImpl<T> {
        private final Operation op;
        private final long leftValue;
        private final long rightValue;

        public enum Operation {
            BETWEEN
        }

        public LongLongCondition(Property<T> property, Operation op, long leftValue, long rightValue) {
            super(property);
            this.op = op;
            this.leftValue = leftValue;
            this.rightValue = rightValue;
        }

        public LongLongCondition(Property<T> property, Operation op, Date leftValue, Date rightValue) {
            this(property, op, leftValue.getTime(), rightValue.getTime());
        }

        @Override
        void applyCondition(QueryBuilder<T> builder) {
            if (op == Operation.BETWEEN) {
                builder.between(property, leftValue, rightValue);
            } else {
                throw new UnsupportedOperationException(op + " is not supported with two long values");
            }
        }
    }

    public static class LongArrayCondition<T> extends PropertyQueryConditionImpl<T> {
        private final Operation op;
        private final long[] value;

        public enum Operation {
            IN,
            NOT_IN
        }

        public LongArrayCondition(Property<T> property, Operation op, long[] value) {
            super(property);
            this.op = op;
            this.value = value;
        }

        public LongArrayCondition(Property<T> property, Operation op, Date[] value) {
            super(property);
            this.op = op;
            this.value = new long[value.length];
            for (int i = 0; i < value.length; i++) {
                this.value[i] = value[i].getTime();
            }
        }

        @Override
        void applyCondition(QueryBuilder<T> builder) {
            switch (op) {
                case IN:
                    builder.in(property, value);
                    break;
                case NOT_IN:
                    builder.notIn(property, value);
                    break;
                default:
                    throw new UnsupportedOperationException(op + " is not supported for long[]");
            }
        }
    }

    public static class DoubleCondition<T> extends PropertyQueryConditionImpl<T> {
        private final Operation op;
        private final double value;

        public enum Operation {
            GREATER,
            GREATER_OR_EQUAL,
            LESS,
            LESS_OR_EQUAL
        }

        public DoubleCondition(Property<T> property, Operation op, double value) {
            super(property);
            this.op = op;
            this.value = value;
        }

        @Override
        void applyCondition(QueryBuilder<T> builder) {
            switch (op) {
                case GREATER:
                    builder.greater(property, value);
                    break;
                case GREATER_OR_EQUAL:
                    builder.greaterOrEqual(property, value);
                    break;
                case LESS:
                    builder.less(property, value);
                    break;
                case LESS_OR_EQUAL:
                    builder.lessOrEqual(property, value);
                    break;
                default:
                    throw new UnsupportedOperationException(op + " is not supported for double");
            }
        }
    }

    public static class DoubleDoubleCondition<T> extends PropertyQueryConditionImpl<T> {
        private final Operation op;
        private final double leftValue;
        private final double rightValue;

        public enum Operation {
            BETWEEN
        }

        public DoubleDoubleCondition(Property<T> property, Operation op, double leftValue, double rightValue) {
            super(property);
            this.op = op;
            this.leftValue = leftValue;
            this.rightValue = rightValue;
        }

        @Override
        void applyCondition(QueryBuilder<T> builder) {
            if (op == Operation.BETWEEN) {
                builder.between(property, leftValue, rightValue);
            } else {
                throw new UnsupportedOperationException(op + " is not supported with two double values");
            }
        }
    }

    public static class StringCondition<T> extends PropertyQueryConditionImpl<T> {
        private final Operation op;
        private final String value;
        private final StringOrder order;

        public enum Operation {
            EQUAL,
            NOT_EQUAL,
            GREATER,
            GREATER_OR_EQUAL,
            LESS,
            LESS_OR_EQUAL,
            CONTAINS,
            CONTAINS_ELEMENT,
            STARTS_WITH,
            ENDS_WITH
        }

        public StringCondition(Property<T> property, Operation op, String value, StringOrder order) {
            super(property);
            this.op = op;
            this.value = value;
            this.order = order;
        }

        public StringCondition(Property<T> property, Operation op, String value) {
            this(property, op, value, StringOrder.CASE_SENSITIVE);
        }

        @Override
        void applyCondition(QueryBuilder<T> builder) {
            switch (op) {
                case EQUAL:
                    builder.equal(property, value, order);
                    break;
                case NOT_EQUAL:
                    builder.notEqual(property, value, order);
                    break;
                case GREATER:
                    builder.greater(property, value, order);
                    break;
                case GREATER_OR_EQUAL:
                    builder.greaterOrEqual(property, value, order);
                    break;
                case LESS:
                    builder.less(property, value, order);
                    break;
                case LESS_OR_EQUAL:
                    builder.lessOrEqual(property, value, order);
                    break;
                case CONTAINS:
                    builder.contains(property, value, order);
                    break;
                case CONTAINS_ELEMENT:
                    builder.containsElement(property, value, order);
                    break;
                case STARTS_WITH:
                    builder.startsWith(property, value, order);
                    break;
                case ENDS_WITH:
                    builder.endsWith(property, value, order);
                    break;
                default:
                    throw new UnsupportedOperationException(op + " is not supported for String");
            }
        }
    }

    public static class StringStringCondition<T> extends PropertyQueryConditionImpl<T> {
        private final Operation op;
        private final String leftValue;
        private final String rightValue;
        private final StringOrder order;

        public enum Operation {
            EQUAL_KEY_VALUE,
            GREATER_KEY_VALUE,
            GREATER_EQUALS_KEY_VALUE,
            LESS_KEY_VALUE,
            LESS_EQUALS_KEY_VALUE
        }

        public StringStringCondition(Property<T> property, Operation op, String leftValue, String rightValue, StringOrder order) {
            super(property);
            this.op = op;
            this.leftValue = leftValue;
            this.rightValue = rightValue;
            this.order = order;
        }

        @Override
        void applyCondition(QueryBuilder<T> builder) {
            if (op == Operation.EQUAL_KEY_VALUE) {
                builder.equalKeyValue(property, leftValue, rightValue, order);
            } else if (op == Operation.GREATER_KEY_VALUE) {
                builder.greaterKeyValue(property, leftValue, rightValue, order);
            } else if (op == Operation.GREATER_EQUALS_KEY_VALUE) {
                builder.greaterOrEqualKeyValue(property, leftValue, rightValue, order);
            } else if (op == Operation.LESS_KEY_VALUE) {
                builder.lessKeyValue(property, leftValue, rightValue, order);
            } else if (op == Operation.LESS_EQUALS_KEY_VALUE) {
                builder.lessOrEqualKeyValue(property, leftValue, rightValue, order);
            } else {
                throw new UnsupportedOperationException(op + " is not supported with two String values");
            }
        }
    }

    public static class StringLongCondition<T> extends PropertyQueryConditionImpl<T> {
        private final Operation op;
        private final String leftValue;
        private final long rightValue;

        public enum Operation {
            EQUAL_KEY_VALUE,
            GREATER_KEY_VALUE,
            GREATER_EQUALS_KEY_VALUE,
            LESS_KEY_VALUE,
            LESS_EQUALS_KEY_VALUE
        }

        public StringLongCondition(Property<T> property, Operation op, String leftValue, long rightValue) {
            super(property);
            this.op = op;
            this.leftValue = leftValue;
            this.rightValue = rightValue;
        }

        @Override
        void applyCondition(QueryBuilder<T> builder) {
            if (op == Operation.EQUAL_KEY_VALUE) {
                builder.equalKeyValue(property, leftValue, rightValue);
            } else if (op == Operation.GREATER_KEY_VALUE) {
                builder.greaterKeyValue(property, leftValue, rightValue);
            } else if (op == Operation.GREATER_EQUALS_KEY_VALUE) {
                builder.greaterOrEqualKeyValue(property, leftValue, rightValue);
            } else if (op == Operation.LESS_KEY_VALUE) {
                builder.lessKeyValue(property, leftValue, rightValue);
            } else if (op == Operation.LESS_EQUALS_KEY_VALUE) {
                builder.lessOrEqualKeyValue(property, leftValue, rightValue);
            } else {
                throw new UnsupportedOperationException(op + " is not supported with two String values");
            }
        }
    }

    public static class StringDoubleCondition<T> extends PropertyQueryConditionImpl<T> {
        private final Operation op;
        private final String leftValue;
        private final double rightValue;

        public enum Operation {
            EQUAL_KEY_VALUE,
            GREATER_KEY_VALUE,
            GREATER_EQUALS_KEY_VALUE,
            LESS_KEY_VALUE,
            LESS_EQUALS_KEY_VALUE
        }

        public StringDoubleCondition(Property<T> property, Operation op, String leftValue, double rightValue) {
            super(property);
            this.op = op;
            this.leftValue = leftValue;
            this.rightValue = rightValue;
        }

        @Override
        void applyCondition(QueryBuilder<T> builder) {
            if (op == Operation.EQUAL_KEY_VALUE) {
                builder.equalKeyValue(property, leftValue, rightValue);
            } else if (op == Operation.GREATER_KEY_VALUE) {
                builder.greaterKeyValue(property, leftValue, rightValue);
            } else if (op == Operation.GREATER_EQUALS_KEY_VALUE) {
                builder.greaterOrEqualKeyValue(property, leftValue, rightValue);
            } else if (op == Operation.LESS_KEY_VALUE) {
                builder.lessKeyValue(property, leftValue, rightValue);
            } else if (op == Operation.LESS_EQUALS_KEY_VALUE) {
                builder.lessOrEqualKeyValue(property, leftValue, rightValue);
            } else {
                throw new UnsupportedOperationException(op + " is not supported with two String values");
            }
        }
    }

    public static class StringArrayCondition<T> extends PropertyQueryConditionImpl<T> {
        private final Operation op;
        private final String[] value;
        private final StringOrder order;

        public enum Operation {
            IN
        }

        public StringArrayCondition(Property<T> property, Operation op, String[] value, StringOrder order) {
            super(property);
            this.op = op;
            this.value = value;
            this.order = order;
        }

        public StringArrayCondition(Property<T> property, Operation op, String[] value) {
            this(property, op, value, StringOrder.CASE_SENSITIVE);
        }

        @Override
        void applyCondition(QueryBuilder<T> builder) {
            if (op == Operation.IN) {
                builder.in(property, value, order);
            } else {
                throw new UnsupportedOperationException(op + " is not supported for String[]");
            }
        }
    }

    public static class ByteArrayCondition<T> extends PropertyQueryConditionImpl<T> {
        private final Operation op;
        private final byte[] value;

        public enum Operation {
            EQUAL,
            GREATER,
            GREATER_OR_EQUAL,
            LESS,
            LESS_OR_EQUAL
        }

        public ByteArrayCondition(Property<T> property, Operation op, byte[] value) {
            super(property);
            this.op = op;
            this.value = value;
        }

        @Override
        void applyCondition(QueryBuilder<T> builder) {
            switch (op) {
                case EQUAL:
                    builder.equal(property, value);
                    break;
                case GREATER:
                    builder.greater(property, value);
                    break;
                case GREATER_OR_EQUAL:
                    builder.greaterOrEqual(property, value);
                    break;
                case LESS:
                    builder.less(property, value);
                    break;
                case LESS_OR_EQUAL:
                    builder.lessOrEqual(property, value);
                    break;
                default:
                    throw new UnsupportedOperationException(op + " is not supported for byte[]");
            }
        }
    }

    /**
     * Conditions for properties with an {@link io.objectbox.annotation.HnswIndex}.
     */
    public static class NearestNeighborCondition<T> extends PropertyQueryConditionImpl<T> {

        private final float[] queryVector;
        private final int maxResultCount;

        public NearestNeighborCondition(Property<T> property, float[] queryVector, int maxResultCount) {
            super(property);
            this.queryVector = queryVector;
            this.maxResultCount = maxResultCount;
        }

        @Override
        void applyCondition(QueryBuilder<T> builder) {
            builder.nearestNeighbors(property, queryVector, maxResultCount);
        }
    }
}
