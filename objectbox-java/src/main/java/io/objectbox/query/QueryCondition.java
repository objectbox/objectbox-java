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
package io.objectbox.query;

import java.util.Date;

import javax.annotation.Nullable;

import io.objectbox.Property;
import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.exception.DbException;
import io.objectbox.query.QueryBuilder.StringOrder;

/**
 * Internal interface to model WHERE conditions used in queries. Use the {@link Property} objects in the DAO classes to
 * create new conditions.
 */
@Experimental
@Internal
public interface QueryCondition {

    void applyTo(QueryBuilder queryBuilder, StringOrder stringOrder);

    void setParameterFor(Query query, Object parameter);

    void setParameterFor(Query query, Object parameter1, Object parameter2);

    abstract class AbstractCondition implements QueryCondition {

        public final Object value;
        protected final Object[] values;

        AbstractCondition(Object value) {
            this.value = value;
            this.values = null;
        }

        AbstractCondition(@Nullable Object[] values) {
            this.value = null;
            this.values = values;
        }

    }

    class PropertyCondition extends AbstractCondition {

        public enum Operation {
            EQUALS,
            NOT_EQUALS,
            BETWEEN,
            IN,
            GREATER_THAN,
            LESS_THAN,
            IS_NULL,
            IS_NOT_NULL,
            CONTAINS,
            STARTS_WITH,
            ENDS_WITH
        }

        public final Property property;
        private final Operation operation;

        public PropertyCondition(Property property, Operation operation, @Nullable Object value) {
            super(checkValueForType(property, value));
            this.property = property;
            this.operation = operation;
        }

        public PropertyCondition(Property property, Operation operation, @Nullable Object[] values) {
            super(checkValuesForType(property, operation, values));
            this.property = property;
            this.operation = operation;
        }

        public void applyTo(QueryBuilder queryBuilder, StringOrder stringOrder) {
            if (operation == Operation.EQUALS) {
                if (value instanceof Long) {
                    queryBuilder.equal(property, (Long) value);
                } else if (value instanceof Integer) {
                    queryBuilder.equal(property, (Integer) value);
                } else if (value instanceof String) {
                    queryBuilder.equal(property, (String) value, stringOrder);
                }
            } else if (operation == Operation.NOT_EQUALS) {
                if (value instanceof Long) {
                    queryBuilder.notEqual(property, (Long) value);
                } else if (value instanceof Integer) {
                    queryBuilder.notEqual(property, (Integer) value);
                } else if (value instanceof String) {
                    queryBuilder.notEqual(property, (String) value, stringOrder);
                }
            } else if (operation == Operation.BETWEEN) {
                if (values[0] instanceof Long && values[1] instanceof Long) {
                    queryBuilder.between(property, (Long) values[0], (Long) values[1]);
                } else if (values[0] instanceof Integer && values[1] instanceof Integer) {
                    queryBuilder.between(property, (Integer) values[0], (Integer) values[1]);
                } else if (values[0] instanceof Double && values[1] instanceof Double) {
                    queryBuilder.between(property, (Double) values[0], (Double) values[1]);
                } else if (values[0] instanceof Float && values[1] instanceof Float) {
                    queryBuilder.between(property, (Float) values[0], (Float) values[1]);
                }
            } else if (operation == Operation.IN) {
                // just check the first value and assume all others are of the same type
                // maybe this is too naive and we should properly check values earlier
                if (values[0] instanceof Long) {
                    long[] inValues = new long[values.length];
                    for (int i = 0; i < values.length; i++) {
                        inValues[i] = (long) values[i];
                    }
                    queryBuilder.in(property, inValues);
                } else if (values[0] instanceof Integer) {
                    int[] inValues = new int[values.length];
                    for (int i = 0; i < values.length; i++) {
                        inValues[i] = (int) values[i];
                    }
                    queryBuilder.in(property, inValues);
                }
            } else if (operation == Operation.GREATER_THAN) {
                if (value instanceof Long) {
                    queryBuilder.greater(property, (Long) value);
                } else if (value instanceof Integer) {
                    queryBuilder.greater(property, (Integer) value);
                } else if (value instanceof Double) {
                    queryBuilder.greater(property, (Double) value);
                } else if (value instanceof Float) {
                    queryBuilder.greater(property, (Float) value);
                }
            } else if (operation == Operation.LESS_THAN) {
                if (value instanceof Long) {
                    queryBuilder.less(property, (Long) value);
                } else if (value instanceof Integer) {
                    queryBuilder.less(property, (Integer) value);
                } else if (value instanceof Double) {
                    queryBuilder.less(property, (Double) value);
                } else if (value instanceof Float) {
                    queryBuilder.less(property, (Float) value);
                }
            } else if (operation == Operation.IS_NULL) {
                queryBuilder.isNull(property);
            } else if (operation == Operation.IS_NOT_NULL) {
                queryBuilder.notNull(property);
            } else if (operation == Operation.CONTAINS) {
                // no need for greenDAO compat, so only String was allowed
                queryBuilder.contains(property, (String) value, stringOrder);
            } else if (operation == Operation.STARTS_WITH) {
                // no need for greenDAO compat, so only String was allowed
                queryBuilder.startsWith(property, (String) value, stringOrder);
            } else if (operation == Operation.ENDS_WITH) {
                // no need for greenDAO compat, so only String was allowed
                queryBuilder.endsWith(property, (String) value, stringOrder);
            } else {
                throw new UnsupportedOperationException("This operation is not known.");
            }
        }

        private static Object checkValueForType(Property property, @Nullable Object value) {
            if (value != null && value.getClass().isArray()) {
                throw new DbException("Illegal value: found array, but simple object required");
            }
            Class<?> type = property.type;
            if (type == Date.class) {
                if (value instanceof Date) {
                    return ((Date) value).getTime();
                } else if (value instanceof Long) {
                    return value;
                } else {
                    throw new DbException("Illegal date value: expected java.util.Date or Long for value " + value);
                }
            } else if (property.type == boolean.class || property.type == Boolean.class) {
                if (value instanceof Boolean) {
                    return ((Boolean) value) ? 1 : 0;
                } else if (value instanceof Number) {
                    int intValue = ((Number) value).intValue();
                    if (intValue != 0 && intValue != 1) {
                        throw new DbException("Illegal boolean value: numbers must be 0 or 1, but was " + value);
                    }
                } else if (value instanceof String) {
                    String stringValue = ((String) value);
                    if ("TRUE".equalsIgnoreCase(stringValue)) {
                        return 1;
                    } else if ("FALSE".equalsIgnoreCase(stringValue)) {
                        return 0;
                    } else {
                        throw new DbException(
                                "Illegal boolean value: Strings must be \"TRUE\" or \"FALSE\" (case insensitive), but was "
                                        + value);
                    }
                }
            }
            return value;
        }

        private static Object[] checkValuesForType(Property property, Operation operation, @Nullable Object[] values) {
            if (values == null) {
                if (operation == Operation.IS_NULL || operation == Operation.IS_NOT_NULL) {
                    return null;
                } else {
                    throw new IllegalArgumentException("This operation requires non-null values.");
                }
            }
            for (int i = 0; i < values.length; i++) {
                values[i] = checkValueForType(property, values[i]);
            }
            return values;
        }

        @Override
        public void setParameterFor(Query query, Object parameter) {
            if (parameter == null) {
                throw new IllegalArgumentException("The new parameter can not be null.");
            }
            if (operation == Operation.BETWEEN) {
                throw new UnsupportedOperationException("The BETWEEN condition requires two parameters.");
            }
            if (operation == Operation.IN) {
                throw new UnsupportedOperationException("The IN condition does not support changing parameters.");
            }
            if (parameter instanceof Long) {
                query.setParameter(property, (Long) parameter);
            } else if (parameter instanceof Integer) {
                query.setParameter(property, (Integer) parameter);
            } else if (parameter instanceof String) {
                query.setParameter(property, (String) parameter);
            } else if (parameter instanceof Double) {
                query.setParameter(property, (Double) parameter);
            } else if (parameter instanceof Float) {
                query.setParameter(property, (Float) parameter);
            } else {
                throw new IllegalArgumentException("Only LONG, INTEGER, DOUBLE, FLOAT or STRING parameters are supported.");
            }
        }

        @Override
        public void setParameterFor(Query query, Object parameter1, Object parameter2) {
            if (parameter1 == null || parameter2 == null) {
                throw new IllegalArgumentException("The new parameters can not be null.");
            }
            if (operation != Operation.BETWEEN) {
                throw new UnsupportedOperationException("Only the BETWEEN condition supports two parameters.");
            }
            if (parameter1 instanceof Long && parameter2 instanceof Long) {
                query.setParameters(property, (Long) parameter1, (Long) parameter2);
            } else if (parameter1 instanceof Integer && parameter2 instanceof Integer) {
                query.setParameters(property, (Integer) parameter1, (Integer) parameter2);
            }  else if (parameter1 instanceof Double && parameter2 instanceof Double) {
                query.setParameters(property, (Double) parameter1, (Double) parameter2);
            }  else if (parameter1 instanceof Float && parameter2 instanceof Float) {
                query.setParameters(property, (Float) parameter1, (Float) parameter2);
            } else {
                throw new IllegalArgumentException("The BETWEEN condition only supports LONG, INTEGER, DOUBLE or FLOAT parameters.");
            }
        }
    }

}
