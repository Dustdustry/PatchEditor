package MinRi2.PatchEditor.node;

public enum ModifierSign{
    // used for Array, Seq, ObjectSet as field
    PLUS("+"),
    // used for ObjectMap as the value
    REMOVE("-"),
    // mark as modifiable and overrideable
    MODIFY("=");

    public final String sign;

    ModifierSign(String sign){
        this.sign = sign;
    }
}
