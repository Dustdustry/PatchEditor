package dustdustry.patcheditor.modifier;

public interface ModifyConsumer<T>{
    T getValue();

    Class<?> getDataType();

    Class<?> getTypeMeta();

    void resetModify();

    void onModify(T value);

    boolean checkValue(T value);
}