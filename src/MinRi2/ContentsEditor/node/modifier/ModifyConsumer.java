package MinRi2.ContentsEditor.node.modifier;

public interface ModifyConsumer<T>{
    /**
     * 获取当前或默认的数据
     */
    T getValue();

    /**
     * 获取数据的类型
     */
    Class<?> getDataType();

    void resetModify();

    /**
     * 用户修改数据
     */
    void onModify(T value);

    boolean isModified();

    /**
     * 检查输入数据是否合法
     */
    boolean checkValue(T value);
}