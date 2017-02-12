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
