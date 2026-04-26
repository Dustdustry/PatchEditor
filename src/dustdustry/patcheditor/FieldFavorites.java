package dustdustry.patcheditor;

import dustdustry.patcheditor.node.*;
import arc.files.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import mindustry.*;
import mindustry.io.*;

import java.lang.reflect.*;

public class FieldFavorites{
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
        String key = node.getFieldId();
        return key != null && map.containsKey(key);
    }

    public static void toggle(EditorNode node){
        String key = node.getFieldId();
        if(key == null) return;

        if(map.containsKey(key)){
            map.remove(key);
            save();
        }else{
            map.put(key, resolve(node));
            save();
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

    public static int fieldCount(){
        return map.size;
    }

    public static Seq<String> allId(){
        return map.keys().toSeq();
    }

    public static boolean canFavorite(EditorNode node){
        return node.getFieldId() != null;
    }

    public static void importJson(String text, boolean replace){
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

    private static FavoriteField resolve(EditorNode node){
        String key = node.getFieldId();
        if(key == null) return null;

        Field field = node.getObjNode().field;
        Class<?> owner = field.getDeclaringClass();
        return new FavoriteField(key, owner.getName(), field.getName());
    }

    public static String exportJson(){
        FavoritesData data = new FavoritesData();
        data.version = 2;
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
                if(!field.check()) continue;

                if(data.version == 1){
                    String className = field.ownerType;
                    Class<?> resolve = PatchJsonIO.resolveType(className);
                    if(resolve == null){
                        try{
                            resolve = Class.forName(className, false, ClassLoader.getSystemClassLoader());
                        }catch(ClassNotFoundException ignored){}
                    }
                    if(resolve != null){
                        field.ownerType = PatchJsonIO.getTypeName(resolve);
                        field.id = field.ownerType + "#" + field.field;
                    }
                }

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
        public transient String ownerType;
        public transient String field;

        public FavoriteField(){
        }

        public FavoriteField(String id, String ownerType, String field){
            this.id = id;
            this.ownerType = ownerType;
            this.field = field;
        }

        public boolean check(){
            if(id == null || id.isEmpty()) return false;

            int split = id.lastIndexOf('#');
            if(ownerType == null){
                ownerType = split != -1 ? id.substring(0, split) : "unknown";
            }

            if(field == null){
                field = split != -1 ? id.substring(split + 1) : id;
            }

            return !ownerType.isEmpty() && !field.isEmpty();
        }
    }
}
