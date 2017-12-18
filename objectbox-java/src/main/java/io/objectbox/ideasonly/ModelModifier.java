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

package io.objectbox.ideasonly;

public class ModelModifier {
    public class EntityModifier {
        final String schemaName;
        final String name;

        EntityModifier(String schemaName, String name) {
            this.schemaName = schemaName;
            this.name = name;
        }

        public void renameTo(String newName) {
        }

        public void remove() {
        }

        public PropertyModifier property(String name) {
            return  new PropertyModifier(this, name);
        }
    }

    public class PropertyModifier {
        final String name;
        final EntityModifier entityModifier;

        PropertyModifier(EntityModifier entityModifier, String name) {
            this.entityModifier = entityModifier;
            this.name = name;
        }

        public void renameTo(String newName) {
        }

        public void remove() {
        }
    }

    public EntityModifier entity(String name) {
        return new EntityModifier("default", name);
    }
}
