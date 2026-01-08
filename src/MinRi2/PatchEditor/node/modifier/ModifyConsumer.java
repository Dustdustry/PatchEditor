package MinRi2.PatchEditor.node.modifier;

public interface ModifyConsumer<T>{
    T getValue();

    T getDefaultValue();

    Class<?> getDataType();

    Class<?> getTypeMeta();

    void resetModify();

    void onModify(T value);

    boolean isModified();

    boolean checkValue(T value);
}