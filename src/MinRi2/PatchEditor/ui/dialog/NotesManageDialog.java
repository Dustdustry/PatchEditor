package MinRi2.PatchEditor.ui.dialog;

import MinRi2.PatchEditor.ui.*;
import arc.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

public class NotesManageDialog extends BaseDialog{
    private static NotesManageDialog dialog;
    private Runnable onChanged;

    private NotesManageDialog(){
        super("@favorites.note.manage");
        shown(this::rebuild);
        hidden(() -> onChanged = null);
        addCloseButton();
    }

    public static void showNotes(){
        show((Runnable)null);
    }

    public static void show(Runnable onChanged){
        if(dialog == null) dialog = new NotesManageDialog();
        dialog.onChanged = onChanged;
        dialog.show();
    }

    private void rebuild(){
        cont.clearChildren();
        cont.defaults().growX().pad(8f);

        cont.table(Styles.grayPanel, top -> {
            top.left().defaults().left().growX();
            top.add("@favorites.note.manage.hint").wrap();
        }).row();

        cont.table(Styles.grayPanel, link -> {
            link.left().defaults().left().growX();
            link.add("@favorites.note.github").padBottom(6f).row();
            link.add(FieldNotes.githubNotesUrl).color(EPalettes.type).wrap();
        }).row();

        buttons.clearChildren();
        buttons.defaults().height(52f).pad(8f).growX();

        buttons.button("@favorites.note.export", Icon.copy, Styles.cleart, this::exportNotes);
        buttons.button("@favorites.note.import", Icon.download, Styles.cleart, this::importNotes)
        .disabled(b -> Core.app.getClipboardText() == null || Core.app.getClipboardText().isEmpty());

        buttons.row();

        buttons.button("@favorites.note.github.open", Icon.book, Styles.cleart, this::openGithub);
        buttons.button("@favorites.note.clearAll", Icon.cancel, Styles.cleart, this::clearAll);
    }

    private void exportNotes(){
        Core.app.setClipboardText(FieldNotes.exportUserNotesJson());
        EUI.infoToast("@favorites.note.export.succeed");
    }

    private void importNotes(){
        String text = Core.app.getClipboardText();
        if(text == null || text.isEmpty()){
            EUI.infoToast("@favorites.note.import.failed");
            return;
        }

        int imported;
        try{
            imported = FieldNotes.importUserNotes(text, false);
        }catch(Exception e){
            Vars.ui.showException("@favorites.note.import.failed", e);
            return;
        }

        if(onChanged != null){
            onChanged.run();
        }
        EUI.infoToast(Core.bundle.format("favorites.note.import.succeed", imported));
    }

    private void openGithub(){
        if(Core.app.openURI(FieldNotes.githubNotesUrl)){
            EUI.infoToast("@favorites.note.github.open.succeed");
        }else{
            EUI.infoToast("@favorites.note.github.open.failed");
        }
    }

    private void clearAll(){
        Vars.ui.showConfirm("@confirm", "@favorites.note.clearAll.confirm", () -> {
            if(FieldNotes.clearUserNotes()){
                if(onChanged != null){
                    onChanged.run();
                }
                EUI.infoToast("@favorites.note.clearAll.succeed");
            }
        });
    }
}
