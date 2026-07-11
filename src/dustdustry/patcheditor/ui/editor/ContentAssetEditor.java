package dustdustry.patcheditor.ui.editor;

import arc.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.JsonWriter.*;
import arc.util.serialization.Jval.*;
import dustdustry.patcheditor.node.*;
import dustdustry.patcheditor.node.PatchExportOptions.*;
import dustdustry.patcheditor.ui.*;
import dustdustry.patcheditor.ui.dialog.*;
import dustdustry.patcheditor.ui.dialog.EditorSettings.*;
import dustdustry.patcheditor.utils.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.mod.data.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import java.util.regex.*;

import static mindustry.Vars.*;

public class ContentAssetEditor extends BaseDialog{
    public static final Pattern unsafeNamePattern = Pattern.compile("[\\[\\]{}`!@#$%^&*();:,]");

    protected ContentEditor editor;

    protected ContentAsset asset;
    protected Class<?> contentClass;

    protected boolean nameInvalid;
    protected @Nullable Runnable onDataChanged;

    public ContentAssetEditor(){
        super("@patch-editor.content-asset");

        editor = new ContentEditor();

        shown(this::rebuild);
        hidden(this::applyJson);
    }

    protected void rebuild(){
        setFillParent(false);
        closeOnBack();

        cont.clearChildren();
        buttons.clearChildren();

        cont.add("@name").padRight(10f);
        TextField nameField = cont.field(asset.name, text -> {
            asset.setPath(text + ".json");
        }).with(t -> {
            t.setFilter((field, c) -> !Character.isWhitespace(c));
            t.setValidator(ContentAssetEditor::isSafeContentName);
            t.changed(() -> nameInvalid = content.byName(t.getText()) != null || !t.isValid());
        }).width(400f).get();
        cont.row();

        cont.add("@asset.content.type").padRight(10f);
        cont.table(t -> {
            t.table(Styles.grayPanel, c -> {
                c.margin(8f);
                c.label(() -> "@content." + asset.type.name());
            }).height(50f).pad(4f).marginLeft(5f).marginRight(5f).width(160f);

            t.image(Tex.whiteui, Pal.accent).size(4f, 50f).pad(4f);

            for(ContentType type : ContentAsset.loadableContent){
                t.button(new TextureRegionDrawable(NodeDisplay.getDisplayIcon(type)), Styles.grayTogglei, iconMed, () -> {
                    asset.type = type;
                    contentClass = type.contentClass;

                    Jval jval = Jval.read(asset.data);
                    jval.remove("type");
                    PatchExportOptions options = EditorSettings.getPatchExportOptions();
                    asset.data = options.format == Format.hjson ? jval.toString(Jformat.hjson)
                    : options.formatJson ? jval.toString(Jformat.formatted)
                    : jval.toString(Jformat.plain);
                }).checked(b -> asset.type == type).size(50f).pad(4f).tooltip("@content." + type.name());
            }
        });

        cont.row();

        cont.add("内容类型：").padRight(10f); // TODO: i18n
        cont.table(t -> {
            t.left();
            t.table(Styles.grayPanel, c -> {
                c.margin(8f);
                c.label(() -> PatchJsonIO.getTypeName(contentClass));
            }).size(260f, 50f).pad(4f).marginLeft(5f).marginRight(5f);

            t.image(Tex.whiteui, Pal.accent).width(4f).pad(4f).fillY();
        }).fillX();

        cont.row();

        cont.label(() -> !nameField.isValid() ? "@asset.content.badname" : "@asset.content.exists").colspan(2).visible(() -> nameInvalid);

        buttons.defaults().height(64f);
        buttons.button("##退出", Icon.exit, this::hide).width(200f).get();

        // TODO: i18n
        buttons.button("##编辑器中打开", Icon.edit, () -> {
            editor.edit(asset, () -> {
                applyJson();
                readContentClass();
            });
        }).width(200f);

        buttons.button("@asset.content.import.file", Icon.fileText, () -> FileChooser.open("json", "json5", "hjson").submit(file -> {
            setJson(file.readString());
        })).width(170f).disabled(b -> nameField.getText().isEmpty() || nameInvalid);

        buttons.button("@asset.content.import.clipboard", Icon.copy, () -> {
            String text = Core.app.getClipboardText();
            if(text == null) return;
            setJson(text);
        }).width(170f).disabled(b -> nameField.getText().isEmpty() || nameInvalid || Core.app.getClipboardText() == null);
    }

    public void show(ContentAsset asset, Runnable onHide){
        this.asset = asset;
        this.onDataChanged = onHide;
        try{
            readContentClass();
        }catch(Exception e){
            Vars.ui.showException(e);
        }
        show();
    }

    protected void setJson(String json){
        String oldJson = asset.data;
        asset.data = json;
        try{
            Jval.read(asset.data);
            readContentClass();
            applyJson();
        }catch(Exception e){
            asset.data = oldJson;
            ui.showException("@patch.importerror", e);
        }
    }

    protected void readContentClass(){
        contentClass = null;
        Jval jval = Jval.read(asset.data);
        contentClass = PatchJsonIO.resolveType(jval.getString("type"));
        if(contentClass == null) contentClass = asset.type.contentClass;
    }

    protected void applyJson(){
        state.data.reloadContent(false);
        state.data.regenerateContentSprites(false);

        if(onDataChanged != null){
            onDataChanged.run();
            onDataChanged = null;
        }
    }

    public static boolean isSafeContentName(String name){
        return Strings.isSafeFilename(name) && !unsafeNamePattern.matcher(name).find();
    }
}
