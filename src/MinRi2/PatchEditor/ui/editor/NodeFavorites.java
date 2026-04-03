package MinRi2.PatchEditor.ui.editor;

import MinRi2.PatchEditor.node.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.io.*;

import java.lang.reflect.*;

/**
 * Persistent favorites for "type field", e.g. "mindustry.world.Block#health".
 */
public class NodeFavorites{
    public static final String fileName = "favorites.json";

    private static Fi configFi;
    private static final OrderedMap<String, FavoriteField> map = new OrderedMap<>();

    public static void init(){
        Fi dir = Vars.modDirectory.child("config/PatchEditor");
        if(!dir.exists()) dir.mkdirs();
        configFi = dir.child(fileName);

        load();
    }

    public static boolean isFavorite(EditorNode node){
        String key = getID(node);
        return key != null && map.containsKey(key);
    }

    public static boolean toggle(EditorNode node){
        String key = getID(node);
        if(key == null) return false;

        if(map.containsKey(key)){
            map.remove(key);
            save();
            return false;
        }else{
            map.put(key, resolve(node));
            save();
            return true;
        }
    }

    public static boolean remove(String id){
        if(id == null || !map.containsKey(id)) return false;

        map.remove(id);
        save();
        return true;
    }

    public static void clear(){
        if(map.isEmpty()) return;

        map.clear();
        save();
    }

    public static Seq<FavoriteField> all(){
        return map.values().toSeq();
    }

    public static int importJson(String text, boolean replace){
        OrderedMap<String, FavoriteField> imported = parseJson(text);

        int before = map.size;
        if(replace){
            map.clear();
        }

        for(FavoriteField favorite : imported.values()){
            map.put(favorite.id, favorite);
        }

        int changed = replace ? map.size : map.size - before;
        if(changed != 0 || replace){
            save();
        }

        return changed;
    }

    private static void load(){
        map.clear();

        try{
            map.putAll(parseJson(configFi.readString()));
        }catch(Exception e){
            Log.err("Failed to read favorites to " + configFi.absolutePath(), e);
        }
    }

    private static void save(){
        try{
            configFi.writeString(exportJson(), false);
        }catch(Exception e){
            Log.err("Failed to save favorites to " + configFi.absolutePath(), e);
        }
    }

    public static boolean canFavorite(EditorNode node){
        return getID(node) != null;
    }

    public static String getID(EditorNode node){
        if(node == null) return null;
        ObjectNode objNode = node.getObjNode();
        if(objNode == null || objNode.field == null) return null;
        return  objNode.field.getDeclaringClass().getName() + "#" + objNode.field.getName();
    }

    private static FavoriteField resolve(EditorNode node){
        String key = getID(node);
        if(key == null) return null;

        Field field = node.getObjNode().field;
        Class<?> owner = field.getDeclaringClass();
        return new FavoriteField(key, owner.getName(), field.getName());
    }

    public static String exportJson(){
        FavoritesData data = new FavoritesData();
        data.version = 1;
        data.favorites.addAll(map.values());
        return JsonIO.json.toJson(data);
    }

    private static OrderedMap<String, FavoriteField> parseJson(String text){
        OrderedMap<String, FavoriteField> result = new OrderedMap<>();
        if(text == null || text.trim().isEmpty()) return result;

        FavoritesData data = JsonIO.json.fromJson(FavoritesData.class, text);
        Seq<FavoriteField> favorites = data.favorites;
        if(favorites != null){
            for(FavoriteField field : favorites){
                if(!field.valid()) continue;
                result.put(field.id, field);
            }
        }

        return result;
    }

    public static class FavoritesData{
        public int version;
        public Seq<FavoriteField> favorites = new Seq<>();
    }

    public static class FavoriteField{
        public String id;
        public String ownerType;
        public String field;

        public FavoriteField(){
        }

        public FavoriteField(String id, String ownerType, String field){
            this.id = id;
            this.ownerType = ownerType;
            this.field = field;
        }

        public String ownerSimple(){
            int split = ownerType.lastIndexOf('.');
            return split == -1 ? ownerType : ownerType.substring(split + 1);
        }

        public String displayName(){
            return ownerSimple() + "." + field;
        }

        public boolean valid(){
            if(id == null || id.isEmpty()) return false;

            int split = id.lastIndexOf('#');
            if((ownerType == null || ownerType.isEmpty()) && split != -1){
                ownerType = id.substring(0, split);
            }else if(ownerType == null || ownerType.isEmpty()){
                ownerType = "unknown";
            }

            if((field == null || field.isEmpty()) && split != -1 && split + 1 < id.length()){
                field = id.substring(split + 1);
            }else if(field == null || field.isEmpty()){
                field = id;
            }

            return !ownerType.isEmpty() && !field.isEmpty();
        }
    }
}
