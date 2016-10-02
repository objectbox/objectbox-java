/*
 * Copyright (C) 2011-2016 Markus Junginger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import io.objectbox.exception.DbException;

/**
 * Internal interface to model WHERE conditions used in queries. Use the {@link Property} objects in the DAO classes to
 * create new conditions.
 */
public interface WhereCondition {

    abstract class AbstractCondition implements WhereCondition {

        public final Object value;

        public AbstractCondition(Object value) {
            this.value = value;
        }

    }

    class PropertyCondition extends AbstractCondition {

        private static Object checkValueForType(Property property, Object value) {
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

        public final Property property;

        public PropertyCondition(Property property, Object value) {
            super(checkValueForType(property, value));
            this.property = property;
        }

    }

}
