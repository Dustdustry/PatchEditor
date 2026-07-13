package dustdustry.patcheditor;

import dustdustry.patcheditor.node.*;
import arc.files.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import mindustry.*;
import mindustry.io.*;

public class FieldNotes{
    public static final String userNotesFileName = "field-notes.user.json";
    public static final String wikiNotesFileName = "field-notes.wiki.json";

    private static Fi userNotesFi;
    private static Fi wikiNotesFi;

    private static NotesData wikiNotesData = new NotesData();
    private static OrderedMap<String, String> wikiNotesCache;
    private static final OrderedMap<String, String> userNotes = new OrderedMap<>();

    public static void init(){
        Fi dir = Vars.modDirectory.child("config/PatchEditor");
        if(!dir.exists()) dir.mkdirs();

        userNotesFi = dir.child(userNotesFileName);
        wikiNotesFi = dir.child(wikiNotesFileName);

        try{
            if(wikiNotesFi.exists()){
                wikiNotesData = JsonIO.json.fromJson(NotesData.class, wikiNotesFi.readString());
                if(wikiNotesData == null) wikiNotesData = new NotesData();
                if(wikiNotesData.notes == null) wikiNotesData.notes = new OrderedMap<>();
            }
        }catch(Exception e){
            Log.err("Failed to load wiki notes", e);
        }

        try{
            if(userNotesFi.exists()){
                userNotes.putAll(parseNotesJson(userNotesFi.readString()));
            }
        }catch(Exception e){
            Log.err("Failed to load user notes", e);
        }
    }

    private static void invalidateWikiCache(){
        wikiNotesCache = null;
    }

    private static OrderedMap<String, String> wikiNotes(){
        if(wikiNotesCache == null){
            wikiNotesCache = new OrderedMap<>();
            for(Entry<String, OrderedMap<String, String>> entry : wikiNotesData.notes){
                String typeName = entry.key;
                for(Entry<String, String> noteEntry : entry.value){
                    wikiNotesCache.put(typeName + "#" + noteEntry.key, noteEntry.value);
                }
            }
        }
        return wikiNotesCache;
    }

    public static boolean canNote(EditorNode node){
        return node.getFieldId() != null;
    }

    public static String getNote(String fieldId){
        if(fieldId == null || fieldId.isEmpty()) return null;
        String user = userNotes.get(fieldId);
        return user != null ? user : wikiNotes().get(fieldId);
    }

    public static String getWikiNote(String fieldId){
        if(fieldId == null || fieldId.isEmpty()) return null;
        return wikiNotes().get(fieldId);
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

    public static int userNoteCount(){
        return userNotes.size;
    }

    public static int wikiNoteCount(){
        return wikiNotes().size;
    }

    public static String getWikiMetaVersion(){
        return wikiNotesData.versionTag;
    }

    public static long getWikiMetaUpdateTime(){
        return wikiNotesData.updateTime;
    }

    public static Seq<String> allId(){
        ObjectSet<String> set = new ObjectSet<>();
        for(String id : wikiNotes().keys()){
            set.add(id);
        }
        for(String id : userNotes.keys()){
            set.add(id);
        }
        return set.toSeq();
    }

    public static String exportUserNotesJson(){
        return notesToJson(userNotes);
    }

    public static String exportWikiNotesJson(){
        return JsonIO.json.toJson(wikiNotesData);
    }

    public static void importUserNotesClipboard(String text, boolean replace){
        OrderedMap<String, String> imported = parseNotesJson(text);
        if(replace) userNotes.clear();
        for(Entry<String, String> entry : imported){
            userNotes.put(entry.key, entry.value);
        }
        saveUserNotes();
    }

    /** Parse an index.json file returning the index data. */
    public static IndexData parseNotesIndex(String text){
        if(text == null || text.trim().isEmpty()) return null;
        return JsonIO.json.fromJson(IndexData.class, text);
    }

    /** Merge parsed notes from raw JSON text into wikiNotes (additive). */
    public static void mergeWikiNotes(String text){
        NotesData incoming = JsonIO.json.fromJson(NotesData.class, text);
        if(incoming == null || incoming.notes == null) return;

        for(Entry<String, OrderedMap<String, String>> entry : incoming.notes){
            String typeName = entry.key;
            OrderedMap<String, String> existingFields = wikiNotesData.notes.get(typeName);
            if(existingFields == null){
                wikiNotesData.notes.put(typeName, entry.value);
            }else{
                for(Entry<String, String> noteEntry : entry.value){
                    existingFields.put(noteEntry.key, noteEntry.value);
                }
            }
        }
        invalidateWikiCache();
    }

    /** Replace all wiki notes with parsed notes from raw JSON text. */
    public static void replaceWikiNotes(String text){
        NotesData incoming = JsonIO.json.fromJson(NotesData.class, text);
        if(incoming == null) return;

        wikiNotesData.clear();
        if(incoming.notes != null) wikiNotesData.notes.putAll(incoming.notes);
        invalidateWikiCache();
    }

    public static void clearWikiNotes(){
        wikiNotesData.clear();
        invalidateWikiCache();
        saveWikiNotes();
    }

    public static void setWikiMeta(NoteDataIndexed meta){
        wikiNotesData.versionTag = meta.versionTag;
        wikiNotesData.updateTime = meta.updateTime;
        wikiNotesData.lang = meta.lang;
    }

    public static void saveWikiNotes(){
        try{
            if(wikiNotesFi != null){
                wikiNotesFi.writeString(JsonIO.json.toJson(wikiNotesData), false);
            }
        }catch(Exception e){
            Log.err("Failed to save wiki notes", e);
        }
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
        if(data == null || data.notes == null) return result;

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
        data.versionTag = "unknown";

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
        public String versionTag;
        public long updateTime;
        public String lang;
        public OrderedMap<String, OrderedMap<String, String>> notes = new OrderedMap<>();

        public void clear(){
            notes.clear();
            versionTag = null;
            updateTime = 0;
            lang = null;
        }
    }

    public static class IndexData{
        public String currentVersionTag;
        public Seq<NoteDataIndexed> notes = new Seq<>();
    }

    public static class NoteDataIndexed{
        public String versionTag;
        public long updateTime;
        public String lang;
        public int noteCount;
        public String fileName;
        public String fileUrl;
    }
}
