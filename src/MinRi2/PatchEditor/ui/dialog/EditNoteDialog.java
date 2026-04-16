package MinRi2.PatchEditor.ui.dialog;

import MinRi2.PatchEditor.*;
import MinRi2.PatchEditor.ui.*;
import arc.*;
import arc.graphics.*;
import mindustry.graphics.*;
import mindustry.ui.dialogs.*;

public class EditNoteDialog extends BaseDialog{
    private String fieldId;
    private String note;

    private Runnable hidden;

    public EditNoteDialog(){
        super("@patch-editor.note.edit");

        shown(this::rebuild);
        hidden(() -> {
            if(note != null){
                String userNote = note.trim();
                if(!userNote.isEmpty()){
                    FieldNotes.setUserNote(fieldId, userNote);
                }else{
                    FieldNotes.removeUserNote(fieldId);
                }
            }

            fieldId = null;
            if(hidden != null){
                Core.app.post(hidden);
                hidden = null;
            }
        });
        addCloseButton();
    }

    public void show(String fieldId){
        show(fieldId, null);
    }

    public void show(String fieldId, Runnable hidden){
        if(fieldId == null || fieldId.isEmpty()) return;
        this.fieldId = fieldId;
        this.note = FieldNotes.getNote(fieldId);
        this.hidden = hidden;
        show();
    }

    private void rebuild(){
        if(fieldId == null) return;

        cont.clearChildren();

        float width = Math.min(Core.graphics.getWidth() * 0.7f, 600f);

        cont.add(titleTable).fillX().row();
        cont.table(t -> {
            t.top();
            t.defaults().top().right().pad(6f);

            t.add("@patch-editor.note.field");
            t.label(() -> fieldId).expandX().left();
            t.row();

            String builtIn = FieldNotes.getBuiltInNote(fieldId);
            String user = FieldNotes.getUserNote(fieldId);
            t.add("@patch-editor.note.builtIn");
            t.add(builtIn == null ? "@patch-editor.note.none" : builtIn).wrap().color(builtIn == null ? Pal.lightishGray : EPalettes.grayFront)
            .growX().left();
            t.row();

            t.add("@patch-editor.note.custom");
            t.area(user, text -> note = text).minHeight(180f).growX().get().setMessageText("@patch-editor.note.none");
        }).width(width);
    }
}
