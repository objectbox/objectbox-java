package io.objectbox.ideasonly;

public interface ModelUpdate {
    void updateModel(ModelModifier modifier, long oldVersion, long newVersion);

}
