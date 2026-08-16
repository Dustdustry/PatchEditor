package dustdustry.patcheditor.ui.dialog;

import arc.func.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import dustdustry.patcheditor.modifier.*;
import dustdustry.patcheditor.ui.*;
import mindustry.entities.part.DrawPart.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

public class EditProgressDialog extends BaseDialog{
    private static final String defaultScript = "build(warmup).curve(smooth)";

    protected ProgressElem preview = new ProgressElem(PartProgress.warmup);

    protected ProgressBuilder progress;
    private Cons<ProgressBuilder> cons;

    protected boolean error;
    protected boolean userEdited;
    protected Label errorLabel;
    protected TextField scriptField;

    public EditProgressDialog(){
        super("@patch-editor.editProgress");

        shown(() -> {
            cont.clearChildren();
            userEdited = false;
            error = false;
            setup();
            evalScript(defaultScript);
        });
        addCloseButton();
    }

    protected void setup(){
        cont.add(preview).size(340f);
        cont.row();

        cont.defaults().pad(8f).fillX();
        errorLabel = cont.add("").color(EPalettes.remove).wrap().get();
        cont.row();

        cont.table(Styles.grayPanel, scriptPanel -> {
            scriptPanel.defaults().pad(8f).expandX().left();
            scriptPanel.add("@patch-editor.editProgress.script").color(EPalettes.type);
            scriptPanel.row();
            scriptPanel.add(scriptField = EUI.deboundTextArea(defaultScript, script -> {
                userEdited = true;
                evalScript(script);
            })).minHeight(180f).growX();
            scriptPanel.row();
            Cell<?> hintCell = scriptPanel.add("@patch-editor.editProgress.defaultHint").color(EPalettes.warn).growX().wrap();
            TableUtils.collapseCell(hintCell, () -> !userEdited);
        });

        buttons.clearChildren();
        buttons.button("@cancel", Icon.exit, this::hide);
        buttons.button("@patch-editor.editProgress.tutorial", Icon.book, this::showTutorial);
        buttons.button("@ok", Icon.ok, () -> {
            hide();
            if(cons != null){
                cons.get(progress);
                cons = null;
            }
        }).disabled(b -> error);
    }

    protected void evalScript(String script){
        errorLabel.setText("");
        try{
            ProgressBuilder builder = EditorScripts.buildProgress(script);
            error = false;
            progress = builder;
            preview.setProgress(progress.apply());
        }catch(Exception e){
            errorLabel.setText(e.getMessage());
            error = true;
        }
    }

    public Dialog show(Cons<ProgressBuilder> cons){
        this.cons = cons;
        return super.show();
    }

    protected void showTutorial(){
        new ProgressTutorialDialog().show();
    }
}
