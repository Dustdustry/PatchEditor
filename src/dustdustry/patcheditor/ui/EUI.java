package dustdustry.patcheditor.ui;

import dustdustry.patcheditor.data.*;
import dustdustry.patcheditor.ui.dialog.*;
import dustdustry.patcheditor.ui.editor.*;
import dustdustry.patcheditor.ui.dialog.selector.*;
import arc.*;
import arc.func.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

/**
 * @author minri2
 * Create by 2024/2/15
 */
public class EUI{
    public static ContentSelector selector;
    public static ClassSelector classSelector;
    public static WeaponSelector weaponSelector;
    public static TextureRegionSelector textureRegionSelector;
    public static StringItemSelector stringItemSelector;
    public static SoundSelector soundSelector;
    public static BlockClassSelector blockClassSelector;
    public static ColorSelector colorSelector;

    public static PatchEditor patchEditor;
    public static ContentAssetEditor contentAssetEditor;
    public static EditNoteDialog noteEditor;
    public static EditProgressDialog progressEditor;

    public static FavoritesDialog favorites;
    public static NotesDialog notes;

    public static MissingRegionsDialog missingRegions;

    public static EditorSettings settings;

    public static void init(){
        EStyles.init();

        selector = new ContentSelector();
        classSelector = new ClassSelector();
        weaponSelector = new WeaponSelector();
        textureRegionSelector = new TextureRegionSelector();
        stringItemSelector = new StringItemSelector();
        soundSelector = new SoundSelector();
        blockClassSelector = new BlockClassSelector();
        colorSelector = new ColorSelector();

        patchEditor = new PatchEditor();
        contentAssetEditor = new ContentAssetEditor();
        noteEditor = new EditNoteDialog();
        progressEditor = new EditProgressDialog();

        favorites = new FavoritesDialog();
        notes = new NotesDialog();

        missingRegions = new MissingRegionsDialog();

        settings = new EditorSettings();

        FieldFavorites.init();
        FieldNotes.init();

        ColorPickerExt.init();
    }

    public static void showUsageInfo(Runnable onHide){
        BaseDialog dialog = new BaseDialog("@patch-editor.usageHint.title");

        dialog.setFillParent(false);
        dialog.cont.margin(16f);

        dialog.cont.defaults().expandX().left();

        for(int i = 1;; i++){
            String line = Core.bundle.get("patch-editor.usageHint" + "." + i, null);
            if(line == null) break;
            dialog.cont.add(line).padTop(4f).row();
        }

        dialog.hidden(onHide);
        dialog.addCloseButton();
        dialog.show();
    }

    public static TextField deboundTextField(String text, Cons<String> changed){
        return deboundTextField(text, changed, 0.5f);
    }

    public static TextField deboundTextField(String text, Cons<String> changed, float timeSeconds){
        if(Vars.mobile && !Core.input.useKeyboard()){
            TextField field = new TextField(text);
            field.changed(() -> changed.get(field.getText()));
            return field;
        }

        return new DeboundTextField(text, timeSeconds, changed);
    }

    public static TextArea deboundTextArea(String text, Cons<String> changed){
        return deboundTextArea(text, changed, 0.5f);
    }

    public static TextArea deboundTextArea(String text, Cons<String> changed, float timeSeconds){
        if(Vars.mobile && !Core.input.useKeyboard()){
            TextArea area = new TextArea(text);
            area.changed(() -> changed.get(area.getText()));
            return area;
        }

        return new DeboundTextArea(text, timeSeconds, changed);
    }

    public static void infoToast(String text){
        infoToast(text, 0.7f);
    }

    public static void copiedToast(){
        copiedToast(null);
    }

    public static void copiedToast(String text){
        infoToast(text == null ? "@patch-editor.copied" : Core.bundle.format("patch-editor.copied.with", text));
    }

    public static void infoToast(String text, float duration){
        Table t = new Table(Styles.black3);
        t.touchable = Touchable.disabled;
        t.margin(16).add(text).wrap().width(256f).style(Styles.outlineLabel).labelAlign(Align.left);

        t.update(t::toFront);

        t.pack();

        float y = Core.scene.getHeight() / 2;
        t.actions(
        Actions.moveToAligned(0, y, Align.right),
        Actions.moveToAligned(0, y, Align.left, 0.8f, Interp.pow4Out),
        Actions.delay(duration),
        Actions.parallel(
        Actions.moveToAligned(0, y, Align.right, 0.8f, Interp.pow4Out),
        Actions.fadeOut(0.8f, Interp.fade)
        ),
        Actions.remove()
        );

        t.act(0.1f);
        Core.scene.add(t);
    }

    public static void backButtonClick(Button btn, Runnable backClicked){
        btn.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if(btn.isDisabled()) return;

                Element current = event.targetActor;
                while(current != null && !(current instanceof Button)){
                    current = current.parent;
                }

                if(current == btn){
                    backClicked.run();
                }
            }
        });
    }

    public static class DeboundTextField extends TextField{
        private boolean keeping;
        private final Timekeeper keeper;
        private final Cons<String> cons;

        @Override
        public void act(float delta){
            super.act(delta);

            if(keeping && keeper.get()){
                keeping = false;
                cons.get(getText());
            }
        }

        public DeboundTextField(String text, float seconds, Cons<String> cons){
            super(text);
            this.cons = cons;
            keeper = Timekeeper.ofSeconds(seconds);

            changed(() -> {
                keeping = true;
                keeper.reset();
            });
        }
    }

    public static class DeboundTextArea extends TextArea{
        private boolean keeping;
        private final Timekeeper keeper;
        private final Cons<String> cons;

        @Override
        public void act(float delta){
            super.act(delta);

            if(keeping && keeper.get()){
                keeping = false;
                cons.get(getText());
            }
        }

        public DeboundTextArea(String text, float seconds, Cons<String> cons){
            super(text);
            this.cons = cons;
            keeper = Timekeeper.ofSeconds(seconds);

            changed(() -> {
                keeping = true;
                keeper.reset();
            });
        }
    }
}
