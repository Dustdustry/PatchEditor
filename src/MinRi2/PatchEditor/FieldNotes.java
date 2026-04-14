package MinRi2.PatchEditor;

import MinRi2.PatchEditor.node.*;
import arc.files.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import mindustry.*;
import mindustry.io.*;

/**
 * Field notes with fixed priority:
 * user notes > built-in notes.
 */
public class FieldNotes{
    public static final String builtInNotesPath = "notes/default.json";
    public static final String userNotesFileName = "field-notes.user.json";
    public static final String githubNotesUrl = "https://github.com/minri2/PatchEditor/tree/main/notes";

    private static Fi userNotesFi;

    private static final OrderedMap<String, String> builtInNotes = new OrderedMap<>();
    private static final OrderedMap<String, String> userNotes = new OrderedMap<>();

    public static void init(){
        Fi dir = Vars.modDirectory.child("config/PatchEditor");
        if(!dir.exists()) dir.mkdirs();

        userNotesFi = dir.child(userNotesFileName);
        Fi builtIn = Vars.tree.get(builtInNotesPath);

        try{
            if(userNotesFi.exists()){
                userNotes.putAll(parseNotesJson(userNotesFi.readString()));
            }
        }catch(Exception e){
            Log.err("Failed to load user notes", e);
        }

        try{
            if(builtIn.exists()){
                builtInNotes.putAll(parseNotesJson(builtIn.readString()));
            }
        }catch(Exception e){
            Log.err("Failed to load built-in notes", e);
        }
    }

    public static boolean canNote(EditorNode node){
        return node.getFieldId() != null;
    }

    public static String getNote(String fieldId){
        if(fieldId == null || fieldId.isEmpty()) return null;
        String user = userNotes.get(fieldId);
        return user != null ? user : builtInNotes.get(fieldId);
    }

    public static String getBuiltInNote(String fieldId){
        if(fieldId == null || fieldId.isEmpty()) return null;
        return builtInNotes.get(fieldId);
    }

    public static String getUserNote(String fieldId){
        if(fieldId == null || fieldId.isEmpty()) return null;
        return userNotes.get(fieldId);
    }

    public static void setUserNote(String fieldId, String note){
        if(fieldId == null || fieldId.isEmpty()) return;
        userNotes.put(fieldId, note);
        saveUserNotes();
    }

    public static void removeUserNote(String fieldId){
        if(fieldId == null || fieldId.isEmpty()) return;
        if(!userNotes.containsKey(fieldId)) return;

        userNotes.remove(fieldId);
        saveUserNotes();
    }

    public static boolean clearUserNotes(){
        if(userNotes.isEmpty()) return false;
        userNotes.clear();
        saveUserNotes();
        return true;
    }

    public static String exportUserNotesJson(){
        return notesToJson(userNotes);
    }

    public static int importUserNotes(String text, boolean replace){
        OrderedMap<String, String> imported = parseNotesJson(text);

        if(replace) userNotes.clear();
        for(Entry<String, String> entry : imported){
            userNotes.put(entry.key, entry.value);
        }

        saveUserNotes();
        return 0;
    }

    private static void saveUserNotes(){
        try{
            if(userNotesFi != null){
                userNotesFi.writeString(notesToJson(userNotes), false);
            }
        }catch(Exception e){
            Log.err("Failed to save user notes", e);
        }
    }

    private static OrderedMap<String, String> parseNotesJson(String text){
        OrderedMap<String, String> result = new OrderedMap<>();
        if(text == null || text.trim().isEmpty()) return result;

        NotesData data = JsonIO.json.fromJson(NotesData.class, text);
        if(data == null) return result;

        for(Entry<String, OrderedMap<String, String>> entry : data.notes){
            String typeName = entry.key;
            OrderedMap<String, String> notes = entry.value;

            for(Entry<String, String> noteEntry : notes){
                String field = noteEntry.key;
                String note = noteEntry.value;
                result.put(typeName + "#" + field, note);
            }
        }

        return result;
    }

    private static String notesToJson(OrderedMap<String, String> notesMap){
        NotesData data = new NotesData();
        data.meta.version = 1;
        data.meta.updatedAt = String.valueOf(System.currentTimeMillis());

        for(Entry<String, String> entry : notesMap){
            String id = entry.key;
            String note = entry.value;

            int split = id.lastIndexOf('#');
            if(split == -1) continue;

            String type = id.substring(0, split);
            String field = id.substring(split + 1);
            OrderedMap<String, String> fields = data.notes.get(type, OrderedMap::new);
            fields.put(field, note);
        }

        return JsonIO.json.toJson(data);
    }

    public static class NotesData{
        public NotesMeta meta = new NotesMeta();
        public OrderedMap<String, OrderedMap<String, String>> notes = new OrderedMap<>();
    }

    public static class NotesMeta{
        public int version;
        public String updatedAt;
    }
}