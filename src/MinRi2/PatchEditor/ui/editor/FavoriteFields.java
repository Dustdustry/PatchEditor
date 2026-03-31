package MinRi2.PatchEditor.ui.editor;

import MinRi2.PatchEditor.node.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import arc.util.serialization.JsonValue.*;
import arc.util.serialization.JsonWriter.*;
import arc.util.serialization.Jval.*;
import mindustry.*;

import java.lang.reflect.*;

/**
 * Persistent favorites for "type field", e.g. "mindustry.world.Block#health".
 */
public class FavoriteFields{
    public static final String fileName = "favorites.json";

    private static Fi configFi;
    private static final OrderedMap<String, FavoriteField> favorites = new OrderedMap<>();

    public static void init(){
        Fi dir = Vars.modDirectory.child("config/PatchEditor");
        if(!dir.exists()) dir.mkdirs();
        configFi = dir.child(fileName);

        load();
    }

    public static boolean canFavorite(EditorNode node){
        return resolve(node) != null;
    }

    public static boolean isFavorite(EditorNode node){
        FavoriteField favorite = resolve(node);
        return favorite != null && isFavorite(favorite.id);
    }

    public static boolean isFavorite(String id){
        return id != null && favorites.containsKey(id);
    }

    public static boolean toggle(EditorNode node){
        FavoriteField favorite = resolve(node);
        if(favorite == null) return false;

        if(favorites.containsKey(favorite.id)){
            favorites.remove(favorite.id);
            save();
            return false;
        }else{
            favorites.put(favorite.id, favorite);
            save();
            return true;
        }
    }

    public static boolean remove(String id){
        if(id == null || !favorites.containsKey(id)) return false;

        favorites.remove(id);
        save();
        return true;
    }

    public static void clear(){
        if(favorites.isEmpty()) return;

        favorites.clear();
        save();
    }

    public static Seq<FavoriteField> all(){
        return favorites.values().toSeq();
    }

    public static String exportJson(){
        return buildJson().toJson(OutputType.json);
    }

    public static int importJson(String text, boolean replace){
        OrderedMap<String, FavoriteField> imported = parseJson(text);

        int before = favorites.size;
        if(replace){
            favorites.clear();
        }

        for(FavoriteField favorite : imported.values()){
            favorites.put(favorite.id, favorite);
        }

        int changed = replace ? favorites.size : favorites.size - before;
        if(changed != 0 || replace){
            save();
        }

        return changed;
    }

    private static void load(){
        favorites.clear();

        try{
            favorites.putAll(parseJson(configFi.readString()));
        }catch(Exception e){
            throw new RuntimeException("Failed to read favorites to " + configFi.absolutePath());
        }
    }

    private static void save(){
        try{
            configFi.writeString(exportJson(), false);
        }catch(Exception e){
            throw new RuntimeException("Failed to save favorites to " + configFi.absolutePath());
        }
    }

    private static FavoriteField resolve(EditorNode node){
        if(node == null) return null;
        ObjectNode objNode = node.getObjNode();
        if(objNode == null || objNode.field == null) return null;

        Field field = objNode.field;
        Class<?> owner = field.getDeclaringClass();
        return FavoriteField.from(owner.getName() + "#" + field.getName(), owner.getName(), field.getName());
    }

    private static OrderedMap<String, FavoriteField> parseJson(String text){
        OrderedMap<String, FavoriteField> result = new OrderedMap<>();
        if(text == null || text.trim().isEmpty()) return result;

        JsonValue root = new JsonReader().parse(Jval.read(text).toString(Jformat.plain));
        JsonValue array = root.isArray() ? root : root.get("favorites");
        if(array == null || !array.isArray()) return result;

        for(JsonValue child : array){
            FavoriteField favorite = readFavorite(child);
            if(favorite != null){
                result.put(favorite.id, favorite);
            }
        }

        return result;
    }

    private static FavoriteField readFavorite(JsonValue child){
        if(child == null) return null;
        if(child.isString()){
            return FavoriteField.from(child.asString(), null, null);
        }

        if(!child.isObject()) return null;

        String id = child.getString("id", null);
        String ownerType = child.getString("ownerType", null);
        String fieldName = child.getString("field", null);

        if(id == null && ownerType != null && fieldName != null){
            id = ownerType + "#" + fieldName;
        }

        return FavoriteField.from(id, ownerType, fieldName);
    }

    private static JsonValue buildJson(){
        JsonValue root = new JsonValue(ValueType.object);
        root.addChild("version", new JsonValue(1));

        JsonValue favoriteArray = new JsonValue(ValueType.array);
        root.addChild("favorites", favoriteArray);

        for(FavoriteField favorite : favorites.values()){
            JsonValue favoriteValue = new JsonValue(ValueType.object);
            favoriteValue.addChild("id", new JsonValue(favorite.id));
            favoriteValue.addChild("ownerType", new JsonValue(favorite.ownerType));
            favoriteValue.addChild("field", new JsonValue(favorite.fieldName));
            favoriteArray.addChild(favoriteValue);
        }

        return root;
    }

    public static class FavoriteField{
        public final String id;
        public final String ownerType;
        public final String fieldName;

        private FavoriteField(String id, String ownerType, String fieldName){
            this.id = id;
            this.ownerType = ownerType;
            this.fieldName = fieldName;
        }

        public String ownerSimple(){
            int split = ownerType.lastIndexOf('.');
            return split == -1 ? ownerType : ownerType.substring(split + 1);
        }

        public String displayName(){
            return ownerSimple() + "." + fieldName;
        }

        public static FavoriteField from(String id, String ownerType, String fieldName){
            if(id == null || id.isEmpty()) return null;

            int split = id.lastIndexOf('#');
            if((ownerType == null || ownerType.isEmpty()) && split != -1){
                ownerType = id.substring(0, split);
            }else if(ownerType == null || ownerType.isEmpty()){
                ownerType = "unknown";
            }

            if((fieldName == null || fieldName.isEmpty()) && split != -1 && split + 1 < id.length()){
                fieldName = id.substring(split + 1);
            }else if(fieldName == null || fieldName.isEmpty()){
                fieldName = id;
            }

            if(ownerType.isEmpty() || fieldName.isEmpty()) return null;

            return new FavoriteField(ownerType + "#" + fieldName, ownerType, fieldName);
        }
    }
}
