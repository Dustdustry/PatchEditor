package MinRi2.PatchEditor.ui.dialog;

import MinRi2.PatchEditor.*;
import MinRi2.PatchEditor.ui.*;
import arc.*;
import arc.graphics.*;
import arc.scene.ui.layout.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.dialogs.*;

public class FieldNoteDialog extends BaseDialog{
    private String fieldId;
    private String note;

    public FieldNoteDialog(){
        super("@patch-editor.note.edit");

        shown(this::rebuild);
        hidden(() -> {
            if(note != null){
                String userNote = note.trim();
                if(!userNote.isEmpty()){
                    FieldNotes.setUserNote(fieldId, note);
                }else{
                    FieldNotes.removeUserNote(fieldId);
                }
            }

            fieldId = null;
        });
        addCloseButton();
    }

    public void show(String fieldId){
        if(fieldId == null || fieldId.isEmpty()) return;
        this.fieldId = fieldId;
        this.note = FieldNotes.getNote(fieldId);
        show();
    }

    private void rebuild(){
        if(fieldId == null) return;

        cont.clearChildren();

        float width = Math.min(Math.max(400f, Core.graphics.getWidth() * 0.7f), 600f);

        cont.add(titleTable).fillX().row();
        cont.table(t -> {
            t.top();
            t.defaults().top().right().pad(6f);

            t.add("@patch-editor.note.field").color(EPalettes.type);
            t.label(() -> fieldId).expandX().left();
            t.row();

            String builtIn = FieldNotes.getBuiltInNote(fieldId);
            t.add("@patch-editor.note.builtIn").color(EPalettes.type);
            t.add(builtIn == null ? "@patch-editor.note.none" : builtIn).wrap().color(builtIn == null ? Pal.lightishGray : Color.white)
            .growX().left();
            t.row();

            t.add("@patch-editor.note.custom").color(EPalettes.type);
            t.area(note, text -> note = text).minHeight(180f).growX().get().setMessageText("@patch-editor.note.none");
        }).width(width);
    }
}
