package dustdustry.patcheditor;

public class VersionAdapter{
    public static boolean hasAmoType;

    public static void init(){
        try{
            Class.forName("mindustry.type.AmmoType");
            hasAmoType = true;
        }catch(Exception e){
            hasAmoType = false;
        }
    }
}
