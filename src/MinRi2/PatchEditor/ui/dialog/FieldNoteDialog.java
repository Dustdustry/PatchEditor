package MinRi2.PatchEditor.ui.dialog;

import MinRi2.PatchEditor.*;
import MinRi2.PatchEditor.node.*;
import MinRi2.PatchEditor.ui.*;
import arc.*;
import arc.scene.ui.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

public class FieldNoteDialog extends BaseDialog{
    private static FieldNoteDialog dialog;

    private String fieldId;
    private Runnable onChanged;

    private final TextArea noteArea = new TextArea("");
    private final Label statusLabel = new Label("");
    private final Label effectiveLabel = new Label("");
    private final Label builtInLabel = new Label("");

    private FieldNoteDialog(){
        super("@favorites.note.edit");

        shown(this::rebuild);
        hidden(() -> {
            fieldId = null;
            onChanged = null;
        });
        addCloseButton();
    }

    public static void show(EditorNode node){
        show(node.getFieldID(), null);
    }

    public static void show(String fieldId, Runnable onChanged){
        if(fieldId == null || fieldId.isEmpty()) return;
        if(dialog == null) dialog = new FieldNoteDialog();
        dialog.fieldId = fieldId;
        dialog.onChanged = onChanged;
        dialog.show();
    }

    private void rebuild(){
        cont.clearChildren();
        cont.defaults().growX().pad(8f);

        String user = FieldNotes.getUserNote(fieldId);

        cont.table(Styles.grayPanel, top -> {
            top.left().defaults().left();
            top.add("@favorites.note.field").padRight(6f);
            top.add(fieldId).growX().ellipsis(true);
        }).row();

        cont.table(Styles.grayPanel, status -> {
            status.left().defaults().left();
            status.add("@favorites.note.status").padRight(6f);
            status.add(FieldNotes.isDefaultNote(fieldId) ? "@favorites.note.status.default" : "@favorites.note.status.custom")
            .color(EPalettes.type).growX();
        }).row();

        noteArea.setMaxLength(280);
        noteArea.setText(user == null ? "" : user);
        noteArea.changed(this::refreshPreview);

        cont.table(Styles.grayPanel, editor -> {
            editor.left().defaults().left().growX();
            editor.add("@favorites.note.user").pad(8f).row();
            editor.pane(Styles.noBarPane, noteArea).scrollX(false).minHeight(120f).maxHeight(220f).growX().pad(8f).row();
        }).row();

        cont.table(Styles.grayPanel, info -> {
            info.left().defaults().left().growX();
            statusLabel.setWrap(true);
            effectiveLabel.setWrap(true);
            builtInLabel.setWrap(true);

            info.add(statusLabel).pad(8f).row();
            info.add(effectiveLabel).padLeft(8f).padRight(8f).padBottom(8f).row();
            info.add(builtInLabel).padLeft(8f).padRight(8f).padBottom(8f).color(EPalettes.type).row();
        }).row();

        buttons.clearChildren();
        buttons.defaults().pad(8f).height(52f).growX();

        buttons.button("@favorites.note.clear", Icon.cancel, Styles.cleart, this::clearUserNote);
        buttons.button("@ok", Icon.ok, Styles.cleart, this::saveUserNote);

        refreshPreview();
    }

    private void refreshPreview(){
        String builtIn = FieldNotes.getBuiltInNote(fieldId);
        String userText = trimToNull(noteArea.getText());
        String effective = userText != null ? userText : builtIn;

        String status = userText == null ?
        Core.bundle.get("favorites.note.status.default") :
        Core.bundle.get("favorites.note.status.custom");
        statusLabel.setText(Core.bundle.format("favorites.note.preview.status", status));

        String preview = effective == null ? Core.bundle.get("favorites.note.preview.empty") : effective;
        effectiveLabel.setText(Core.bundle.format("favorites.note.preview.effective", preview));

        String builtInText = builtIn == null ? Core.bundle.get("favorites.note.preview.empty") : builtIn;
        builtInLabel.setText(Core.bundle.format("favorites.note.preview.builtin", builtInText));
    }

    private void saveUserNote(){
        boolean changed = FieldNotes.setUserNote(fieldId, noteArea.getText());
        if(changed){
            EUI.infoToast("@favorites.note.save.succeed");
            notifyChanged();
        }
        hide();
    }

    private void clearUserNote(){
        boolean changed = FieldNotes.removeUserNote(fieldId);
        if(changed){
            EUI.infoToast("@favorites.note.clear.succeed");
            notifyChanged();
        }
        hide();
    }

    private String trimToNull(String text){
        if(text == null) return null;
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void notifyChanged(){
        if(onChanged != null){
            onChanged.run();
        }
    }
}
