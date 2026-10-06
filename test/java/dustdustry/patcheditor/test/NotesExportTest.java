package dustdustry.patcheditor.test;

import static org.junit.jupiter.api.Assertions.*;

import arc.struct.*;
import arc.util.*;
import dustdustry.patcheditor.core.*;
import dustdustry.patcheditor.data.*;
import org.junit.jupiter.api.*;

/** Notes are exported as compact json, which used to be a Json tree round trip. */
@DisplayName("Notes export")
class NotesExportTest extends HeadlessTest{

    @Test
    void exportsCompactJson(){
        FieldNotes.NotesData data = new FieldNotes.NotesData();
        data.versionTag = "v1";
        data.updateTime = 42L;
        data.lang = "en";
        data.notes.put("block.duo", OrderedMap.of("health", "the \"health\" field\nsecond line"));

        Reflect.set(FieldNotes.class, "wikiNotesData", data);

        String json = FieldNotes.exportWikiNotesJson();
        assertTrue(json.startsWith("{"), "the export is a json object: " + json);
        assertTrue(json.contains("\"versionTag\":\"v1\""), "names are quoted: " + json);
        assertTrue(json.contains("\"updateTime\":42"), "numbers stay unquoted: " + json);

        JNode parsed = JNode.of(arc.util.serialization.Jval.read(json));
        assertEquals("v1", parsed.getString("versionTag"), "the export parses back");
        assertEquals("the \"health\" field\nsecond line",
        parsed.get("notes").get("block.duo").get("health").getString("value"),
        "values with quotes and newlines survive");
    }

    @Test
    void exportsUserNotes(){
        FieldNotes.setUserNote("block.duo#health", "keep an eye on \"this\" one");

        String json = FieldNotes.exportUserNotesJson();
        assertTrue(json.startsWith("{"), "the export is a json object: " + json);

        FieldNotes.clearUserNotes();
        assertNull(FieldNotes.getUserNote("block.duo#health"), "the note is gone before the import");

        FieldNotes.importUserNotesClipboard(json, true);
        assertEquals("keep an eye on \"this\" one", FieldNotes.getUserNote("block.duo#health"),
        "the exported json imports back");
    }
}
