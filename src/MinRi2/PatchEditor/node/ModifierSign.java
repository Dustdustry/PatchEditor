package MinRi2.PatchEditor.node;

public enum ModifierSign{
    /**
     * JsonTree: used as field of ArrayLike.
     * '+' means appending an element in original array or override array.
     * ObjectTree: mark as appendable for ArrayLike.
     */
    PLUS("+"),
    /** JsonTree: used as the value.
     * ObjectTree: mark as removable for keys in consumes or MapLike.
     */
    REMOVE("-"),
    /** ObjectTree: mark as modifiable or overrideable for fields. */
    MODIFY("=");

    public final String sign;

    ModifierSign(String sign){
        this.sign = sign;
    }
}
