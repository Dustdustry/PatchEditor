package dustdustry.patcheditor.node.modifier;

public interface ModifyConsumer<T>{
    T getValue();

    T getDefaultValue();

    Class<?> getDataType();

    Class<?> getTypeMeta();

    void resetModify();

    void onModify(T value);

    boolean checkValue(T value);
}