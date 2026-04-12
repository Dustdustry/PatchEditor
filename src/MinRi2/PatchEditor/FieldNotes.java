package MinRi2.PatchEditor;

import MinRi2.PatchEditor.node.*;
import arc.files.*;
import arc.struct.*;
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
        return node.getFieldID() != null;
    }

    public static String getNote(String fieldId){
        if(fieldId == null || fieldId.isEmpty()) return null;
        String user = userNotes.get(fieldId);
        return user != null ? user : builtInNotes.get(fieldId);
    }

    public static String getUserNote(String fieldId){
        if(fieldId == null || fieldId.isEmpty()) return null;
        return userNotes.get(fieldId);
    }

    public static String getBuiltInNote(String fieldId){
        if(fieldId == null || fieldId.isEmpty()) return null;
        return builtInNotes.get(fieldId);
    }

    public static boolean isDefaultNote(String fieldId){
        return getUserNote(fieldId) == null;
    }

    public static boolean setUserNote(String fieldId, String note){
        if(fieldId == null || fieldId.isEmpty()) return false;

        String normalized = normalize(note);
        if(normalized == null){
            return removeUserNote(fieldId);
        }

        String current = userNotes.get(fieldId);
        if(normalized.equals(current)) return false;

        userNotes.put(fieldId, normalized);
        saveUserNotes();
        return true;
    }

    public static boolean removeUserNote(String fieldId){
        if(fieldId == null || fieldId.isEmpty()) return false;
        if(!userNotes.containsKey(fieldId)) return false;

        userNotes.remove(fieldId);
        saveUserNotes();
        return true;
    }

    public static boolean clearUserNotes(){
        if(userNotes.isEmpty()) return false;
        userNotes.clear();
        saveUserNotes();
        return true;
    }

    public static String exportUserNotesJson(){
        return buildNotesJson(userNotes, "user");
    }

    public static int importUserNotes(String text, boolean replace){
        OrderedMap<String, String> imported = parseNotesJson(text);

        boolean changed = false;
        int applied = 0;
        if(replace){
            changed = !userNotes.isEmpty();
            userNotes.clear();
        }

        for(String id : imported.keys()){
            String note = imported.get(id);
            if(note == null) continue;

            applied++;
            String current = userNotes.get(id);
            if(!note.equals(current)){
                changed = true;
            }
            userNotes.put(id, note);
        }

        if(changed){
            saveUserNotes();
        }
        return applied;
    }

    private static void saveUserNotes(){
        try{
            if(userNotesFi != null){
                userNotesFi.writeString(buildNotesJson(userNotes, "user"), false);
            }
        }catch(Exception e){
            Log.err("Failed to save user notes", e);
        }
    }

    private static OrderedMap<String, String> parseNotesJson(String text){
        OrderedMap<String, String> result = new OrderedMap<>();
        if(text == null || text.trim().isEmpty()) return result;

        NotesData data = JsonIO.json.fromJson(NotesData.class, text);
        if(data == null || data.notes == null) return result;

        for(String type : data.notes.keys()){
            if(type == null || type.isEmpty()) continue;

            OrderedMap<String, String> fields = data.notes.get(type);
            if(fields == null) continue;

            String normalizedType = PatchJsonIO.getTypeName(type);
            for(String field : fields.keys()){
                String normalizedField = normalizeField(field);
                String note = normalize(fields.get(field));
                if(normalizedField == null || note == null) continue;

                result.put(normalizedType + "#" + normalizedField, note);
            }
        }

        return result;
    }

    private static String buildNotesJson(OrderedMap<String, String> notesMap, String source){
        NotesData data = new NotesData();
        data.meta.version = 1;
        data.meta.source = source;
        data.meta.updatedAt = String.valueOf(System.currentTimeMillis());

        for(String id : notesMap.keys()){
            String note = notesMap.get(id);
            if(note == null) continue;

            int split = id.lastIndexOf('#');
            if(split <= 0 || split + 1 >= id.length()) continue;

            String type = PatchJsonIO.getTypeName(id.substring(0, split));
            String field = normalizeField(id.substring(split + 1));
            if(field == null || type == null || type.isEmpty()) continue;

            OrderedMap<String, String> fields = data.notes.get(type);
            if(fields == null){
                fields = new OrderedMap<>();
                data.notes.put(type, fields);
            }
            fields.put(field, note);
        }

        return JsonIO.json.toJson(data);
    }

    private static String normalize(String note){
        if(note == null) return null;
        String normalized = note.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String normalizeField(String field){
        if(field == null) return null;
        String normalized = field.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public static class NotesData{
        public NotesMeta meta = new NotesMeta();
        public OrderedMap<String, OrderedMap<String, String>> notes = new OrderedMap<>();
    }

    public static class NotesMeta{
        public int version;
        public String source;
        public String updatedAt;
    }
}
