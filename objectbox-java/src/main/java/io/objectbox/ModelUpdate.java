package io.objectbox;

public interface ModelUpdate {
    void updateModel(ModelModifier modifier, long oldVersion, long newVersion);

}
