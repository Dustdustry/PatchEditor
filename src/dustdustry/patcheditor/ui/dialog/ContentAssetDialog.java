package dustdustry.patcheditor.ui.dialog;

import arc.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.util.*;
import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import dustdustry.patcheditor.node.*;
import dustdustry.patcheditor.ui.*;
import dustdustry.patcheditor.ui.editor.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.mod.data.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import java.util.regex.*;

import static mindustry.Vars.*;

public class ContentAssetDialog extends BaseDialog{
    public static final Pattern unsafeNamePattern = Pattern.compile("[\\[\\]{}`!@#$%^&8*();:,]");

    protected ContentEditor editor;

    protected ContentAsset asset;
    protected @Nullable Jval jsonData;

    protected boolean nameInvalid;
    protected @Nullable Runnable onDataChanged;

    public ContentAssetDialog(){
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
            t.setValidator(ContentAssetDialog::isSafeContentName);
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
                    jsonData.remove("type");
                }).checked(b -> asset.type == type).size(50f).pad(4f).tooltip("@content." + type.name());
            }
        });

        cont.row();

        cont.add("内容类型：").padRight(10f); // TODO: i18n
        cont.table(t -> {
            t.left();
            t.table(Styles.grayPanel, c -> {
                c.margin(8f);
                c.label(() -> {
                    if(!jsonData.isObject()) return "无法读取类型"; // TODO: i18n
                    Jval typeData = jsonData.get("type");
                    Class<?> resolved = null;
                    if(typeData != null) resolved = PatchJsonIO.resolveType(typeData.asString());
                    if(resolved == null) resolved = asset.type.contentClass;
                    return resolved.getSimpleName();
                });
            }).size(260f, 50f).pad(4f).marginLeft(5f).marginRight(5f);

            t.image(Tex.whiteui, Pal.accent).width(4f).pad(4f).fillY();

            t.button(Icon.edit, Styles.graySquarei, () -> {
                EUI.classSelector.select(asset.type.contentClass, (clazz) -> {
                    jsonData.put("type", PatchJsonIO.getTypeName(clazz));
                    return true;
                });
            }).size(50f).pad(4f).disabled(b -> jsonData == null || !jsonData.isObject());
        }).fillX();

        cont.row();

        cont.label(() -> !nameField.isValid() ? "@asset.content.badname" : "@asset.content.exists").colspan(2).visible(() -> nameInvalid);

        buttons.defaults().height(64f);
        buttons.button("##退出", Icon.exit, this::hide).width(200f).get();

        // TODO: i18n
        buttons.button("##编辑器中打开", Icon.edit, () -> {
            applyJson();
            editor.edit(asset, () -> {
                jsonData = Jval.read(asset.data);
                applyJson();
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
            jsonData = Jval.read(asset.data);
        }catch(Exception e){
            jsonData = Jval.newObject();
            Vars.ui.showException(e);
        }

        show();
    }

    protected void setJson(String json){
        String oldJson = asset.data;
        try{
            jsonData = Jval.read(json);
            applyJson();
        }catch(Exception e){
            asset.data = oldJson;
            ui.showException("@patch.importerror", e);
        }
    }

    protected void applyJson(){
        asset.data = jsonData.toString(Jformat.plain);
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
