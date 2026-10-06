package dustdustry.patcheditor.test;

import static org.junit.jupiter.api.Assertions.*;

import arc.struct.*;
import arc.util.*;
import dustdustry.patcheditor.data.*;
import dustdustry.patcheditor.data.FieldFavorites.FavoriteField;
import org.junit.jupiter.api.*;

/**
 * Favorites are user data written by hand-rolled json, and the format already changed once (v1 stored a
 * class name, v2 an id that carries the type). Both the round trip and the old format are pinned here:
 * the notes export once shipped json that could not be read back.
 */
@DisplayName("Favorites export")
class FavoritesExportTest extends HeadlessTest{

    @Test
    void roundTripsThroughJson(){
        OrderedMap<String, FavoriteField> favorites = map();
        favorites.put("Block#health", new FavoriteField("Block#health", "Block", "health"));

        String json = FieldFavorites.exportJson();
        assertTrue(json.contains("Block#health"), "the field id is written: " + json);

        favorites.clear();
        OrderedMap<String, FavoriteField> restored = parse(json);
        assertEquals(1, restored.size, "the export reads back: " + json);
        assertEquals("Block", restored.get("Block#health").ownerType, "the id carries the type name");
        assertEquals("health", restored.get("Block#health").field, "and the field name");
    }

    @Test
    void readsTheOldFormat(){
        //v1 ids used the java class name; ownerType and field are transient and never written to disk
        String legacy = "{\"version\":1,\"favorites\":[{\"id\":\"mindustry.world.Block#health\"}]}";

        OrderedMap<String, FavoriteField> restored = parse(legacy);
        assertEquals(1, restored.size, "a v1 file still imports");
        assertEquals("Block", restored.get("Block#health").ownerType, "its class name is converted to the type name");
        assertEquals("health", restored.get("Block#health").field, "and the field name is kept");
    }

    @SuppressWarnings("unchecked")
    private static OrderedMap<String, FavoriteField> map(){
        return Reflect.get(FieldFavorites.class, "map");
    }

    @SuppressWarnings("unchecked")
    private static OrderedMap<String, FavoriteField> parse(String json){
        return Reflect.invoke(FieldFavorites.class, null, "parseJson", new Object[]{json}, String.class);
    }
}
