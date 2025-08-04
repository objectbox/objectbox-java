/*
 * Copyright 2020 ObjectBox Ltd.
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

package io.objectbox.converter;

import javax.annotation.Nullable;

/**
 * Used as a converter if a property is annotated with {@link io.objectbox.annotation.DefaultValue @DefaultValue("")}.
 */
public class NullToEmptyStringConverter implements PropertyConverter<String, String> {

    @Override
    public String convertToDatabaseValue(String entityProperty) {
        return entityProperty;
    }

    @Override
    public String convertToEntityProperty(@Nullable String databaseValue) {
        if (databaseValue == null) {
            return "";
        }
        return databaseValue;
    }
}
