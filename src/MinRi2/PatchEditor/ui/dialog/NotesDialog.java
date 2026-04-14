package MinRi2.PatchEditor.ui.dialog;

import MinRi2.PatchEditor.*;
import MinRi2.PatchEditor.FieldNotes.*;
import MinRi2.PatchEditor.ui.*;
import arc.*;
import arc.graphics.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

public class NotesDialog extends BaseDialog{
    private String searchText = "";
    private final ObjectSet<String> shownTypes = new ObjectSet<>();

    private ScrollPane pane;
    private final Table notesTable = new Table();

    public NotesDialog(){
        super("@patch-editor.notes");

        resized(this::rebuild);
        shown(this::rebuild);
        hidden(() -> pane = null);

        addCloseButton();
    }

    private void rebuild(){
        cont.top();
        cont.clearChildren();

        float width = Math.min(Core.graphics.getWidth() * 0.7f, 1000f);
        cont.table(table -> {
            table.top();
            table.defaults().pad(8f).growX();

            table.table(Styles.grayPanel, this::setupSearchTable).row();

            if(pane == null) pane = new ScrollPane(notesTable, Styles.noBarPane);
            table.add(pane).scrollX(false).grow().row();

            table.table(Styles.grayPanel, buttons -> {
                buttons.defaults().growX().height(42f);

                buttons.button("@notes.export", Icon.copy, Styles.cleart, () -> {
                    Core.app.setClipboardText(FieldNotes.exportUserNotesJson());
                    EUI.infoToast("@notes.export.succeed");
                });
                buttons.button("@notes.import", Icon.download, Styles.cleart, this::importNotes)
                .disabled(b -> Core.app.getClipboardText() == null || Core.app.getClipboardText().isEmpty());
//                buttons.button("@notes.github.open", Icon.book, Styles.cleart, this::openGithub);
                buttons.button("@notes.clear", Icon.cancel, Styles.cleart, () -> Vars.ui.showConfirm("@confirm", "@notes.clear.confirm", () -> {
                    if(FieldNotes.clearUserNotes()){
                        rebuildNotesTable();
                        EUI.infoToast("@notes.clear.succeed");
                    }
                })).disabled(b -> FieldNotes.userNoteCount() == 0);

                for(Element child : buttons.getChildren()){
                    if(child instanceof Button btn){
                        for(Cell<?> cell : btn.getCells()){
                            cell.pad(8f);
                        }
                    }
                }
            });
        }).width(width).growY();

        rebuildNotesTable();
    }

    private void setupSearchTable(Table table){
        table.image(Icon.zoomSmall).size(Vars.iconMed);

        TextField field = table.add(EUI.deboundTextField(searchText, text -> {
            searchText = text;
            rebuildNotesTable();
        })).padLeft(4f).padRight(4f).growX().get();

        if(Core.app.isDesktop()){
            Core.scene.setKeyboardFocus(field);
        }

        table.button(Icon.cancelSmall, Styles.clearNonei, () -> {
            searchText = "";
            field.setText(searchText);
            rebuildNotesTable();
        }).disabled(b -> searchText.isEmpty()).width(Vars.iconSmall).growY();
    }

    private void rebuildNotesTable(){
        final Table table = notesTable;
        table.clear();
        table.top().defaults().pad(4f);

        Seq<String> notes = FieldNotes.allId().select(fieldId ->
        Strings.matches(searchText, fieldId)
        || (FieldNotes.getNote(fieldId) != null && Strings.matches(searchText, FieldNotes.getNote(fieldId))));

        if(notes.isEmpty()){
            table.add("@notes.empty").pad(16f).color(Color.lightGray);
            return;
        }

        OrderedMap<String, Seq<String>> mapped = new OrderedMap<>();
        for(String fieldId : notes){
            String ownerType = fieldId.split("#")[0];
            mapped.get(ownerType, Seq::new).add(fieldId);
        }

        boolean first = true;
        int shownCount = 0;
        shownTypes.clear();
        for(Entry<String, Seq<String>> entry : mapped){
            String type = entry.key;
            Seq<String> fields = entry.value;

            if(shownCount < 100 && fields.size < 30){
                shownTypes.add(type);
                shownCount += fields.size;
            }

            table.table(top -> {
                top.image().color(Pal.darkerGray).width(32f).padRight(8f);
                top.add(type).labelAlign(Align.left).color(EPalettes.type).minWidth(64f);
                top.add("(" + fields.size + ")").color(EPalettes.grayFront).padLeft(4f);
                top.image().color(Pal.darkerGray).padLeft(8f).padRight(16f).growX();
                top.button(Icon.eyeSmall, Styles.clearTogglei, () -> {
                    if(!shownTypes.add(type)){
                        shownTypes.remove(type);
                    }
                }).size(Vars.iconMed).checked(shownTypes.contains(type));
            }).padTop(first ? 0f : 16f).growX();

            table.row();

            table.collapser(cont -> {
                for(String fieldId : fields){
                    cont.table(t -> setupNoteFieldTable(t, fieldId)).padBottom(4f).growX();
                    cont.row();
                }
            }, () -> shownTypes.contains(type)).growX();

            table.row();

            first = false;
        }
    }

    private void setupNoteFieldTable(Table table, String fieldId){
        table.background(Tex.whiteui).setColor(EPalettes.gray);

        table.table(info -> {
            info.left().defaults().left().growX();
            info.add(fieldId).pad(8f);

            String note = FieldNotes.getNote(fieldId);
            if(note != null){
                info.row();
                info.add(Core.bundle.format("notes.value", note))
                .padLeft(8f).padRight(8f).padBottom(8f).wrap().growX();
            }

            info.row();
            String source = Core.bundle.get(FieldNotes.getUserNote(fieldId) != null ? "notes.source.user" : "notes.source.builtin");
            info.add(Core.bundle.format("notes.source", source))
            .padLeft(8f).padRight(8f).padBottom(8f).color(Pal.lightishGray).growX();
        }).growX();

        table.table(buttons -> {
            buttons.defaults().size(Vars.iconSmall).pad(4f);
            buttons.button(Icon.editSmall, Styles.clearNonei, () -> {
                EUI.noteEditor.show(fieldId, () -> {
                    table.clear();
                    setupNoteFieldTable(table, fieldId);
                });
            }).tooltip("@patch-editor.note.edit");
            buttons.button(Icon.copySmall, Styles.clearNonei, () -> {
                Core.app.setClipboardText(fieldId);
                EUI.infoToast("@favorites.copy-id.succeed");
            }).tooltip("@favorites.copy-id");
            buttons.button(Icon.cancelSmall, Styles.clearNonei, () -> {
                FieldNotes.removeUserNote(fieldId);

                table.clear();
                setupNoteFieldTable(table, fieldId);
            }).tooltip("@notes.clear-user").disabled(b -> FieldNotes.getUserNote(fieldId) == null);
        }).pad(4f);

        table.image().width(4f).color(Color.darkGray).growY().right();
        table.row();
        Cell<?> horizontalLine = table.image().height(4f).color(Color.darkGray).growX();
        horizontalLine.colspan(table.getColumns());
    }

    private void importNotes(){
        String text = Core.app.getClipboardText();
        if(text == null || text.isEmpty()){
            EUI.infoToast("@notes.import.failed");
            return;
        }

        try{
            FieldNotes.importUserNotes(text, false);
        }catch(Exception e){
            Vars.ui.showException("@notes.import.failed", e);
            return;
        }

        rebuildNotesTable();
        EUI.infoToast("@notes.import.succeed");
    }

    private void openGithub(){
        if(Core.app.openURI(FieldNotes.githubNotesUrl)){
            EUI.infoToast("@notes.github.open.succeed");
        }else{
            EUI.infoToast("@notes.github.open.failed");
        }
    }
}
