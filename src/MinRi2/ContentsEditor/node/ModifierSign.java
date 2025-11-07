package MinRi2.ContentsEditor.node;

public enum ModifierSign{
    // used for Array, Seq, ObjectSet as field
    PLUS("+"),
    // used for ObjectMap as the value
    REMOVE("-");

    public final String sign;

    public static final ModifierSign[] all = values();

    ModifierSign(String sign){
        this.sign = sign;
    }
}
