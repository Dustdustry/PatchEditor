package MinRi2.PatchEditor.ui;

import MinRi2.PatchEditor.ui.editor.*;
import MinRi2.PatchEditor.ui.selector.*;
import arc.*;
import arc.func.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.scene.utils.*;
import arc.util.*;
import mindustry.*;
import mindustry.editor.*;
import mindustry.ui.*;

/**
 * @author minri2
 * Create by 2024/2/15
 */
public class EUI{
    public static PatchManager manager;
    public static ContentSelector selector;
    public static ClassSelector classSelector;
    public static WeaponSelector weaponSelector;
    public static TextureRegionSelector textureRegionSelector;
    public static StringItemSelector stringItemSelector;
    public static SoundSelector soundSelector;

    public static void init(){
        EStyles.init();

        manager = new PatchManager();
        selector = new ContentSelector();
        classSelector = new ClassSelector();
        weaponSelector = new WeaponSelector();
        textureRegionSelector = new TextureRegionSelector();
        stringItemSelector = new StringItemSelector();
        soundSelector = new SoundSelector();

        addUI();
    }

    private static void addUI(){
        MapInfoDialog infoDialog = Reflect.get(Vars.ui.editor, "infoDialog");
        infoDialog.shown(() -> Core.app.post(() -> {
            ScrollPane pane = (ScrollPane)infoDialog.cont.getChildren().get(0);
            Table table = Reflect.get(pane, "widget");

            Table buttonTable = (Table)table.getChildren().peek();

            buttonTable.row();

            buttonTable.button(b -> {
                b.add("[accent][PE]").pad(8f).left();
                b.add("@patch-editor").expandX();
            }, Styles.cleari, manager::show)
            .colspan(buttonTable.getColumns()).width(Float.NEGATIVE_INFINITY).growX();

            buttonTable.row();
        }));
    }

    public static TextField deboundTextField(String text, Cons<String> changed){
        return deboundTextField(text, changed, 0.5f);
    }

    public static TextField deboundTextField(String text, Cons<String> changed, float timeSeconds){
        if(Vars.mobile && !Core.input.useKeyboard()){
            return Elem.newField(text, changed);
        }

        return new DeboundTextField(text, timeSeconds, changed);
    }

    public static void infoToast(String text){
        infoToast(text, 0.7f);
    }

    public static void infoToast(String text, float duration){
        Table t = new Table(Styles.black3);
        t.touchable = Touchable.disabled;
        t.margin(16).add(text).style(Styles.outlineLabel).labelAlign(Align.center);

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

        public DeboundTextField(String text, float seconds, Cons<String> deboundCons){
            setText(text);
            keeper = new Timekeeper(seconds);

            changed(() -> {
                keeping = true;
                keeper.reset();
            });

            addAction(Actions.forever(Actions.run(() -> {
                if(keeping && keeper.get()){
                    keeping = false;
                    deboundCons.get(getText());
                }
            })));
        }
    }
}
