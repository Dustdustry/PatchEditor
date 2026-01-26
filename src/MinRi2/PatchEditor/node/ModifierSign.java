package MinRi2.PatchEditor.node;

public enum ModifierSign{
    /** Used for Array, Seq, ObjectSet as field.
     * '+' means appending an element in original array or override array.
     */
    PLUS("+"),
    /** Used for ObjectMap as the value.
     * '-' means removing the key of map.
     */
    REMOVE("-"),
    /** Used for fields
     * '=' means this field is modifiable or overrideable
     */
    MODIFY("=");

    public final String sign;

    ModifierSign(String sign){
        this.sign = sign;
    }
}
