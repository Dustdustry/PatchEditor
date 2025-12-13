package MinRi2.ContentsEditor.node.modifier;

import MinRi2.ContentsEditor.node.*;
import MinRi2.ContentsEditor.ui.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.actions.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

public abstract class ModifierBuilder<T>{
    protected T value;
    protected Button resetButton;
    protected final ModifyConsumer<T> consumer;

    public ModifierBuilder(ModifyConsumer<T> consumer){
        this.consumer = consumer;
    }

    public abstract void build(Table table);

    protected void addResetButton(Table table, Runnable clicked){
        resetButton = table.button(Icon.undo, Styles.clearNonei, () -> {
            consumer.resetModify();
            clicked.run();
        }).width(32f).pad(4f).growY().expandX().right().visible(consumer::isModified).tooltip("@node-modifier.undo", true).get();
    }

    public static class TextBuilder extends ModifierBuilder<String>{

        public TextBuilder(ModifyConsumer<String> consumer){
            super(consumer);
        }

        @Override
        public void build(Table table){
            value = consumer.getValue();

            TextField field = table.field(value, t -> {
                consumer.onModify(t);
                resetButton.visible = consumer.isModified();
            }).valid(consumer::checkValue).pad(4f).width(100f).get();

            addResetButton(table, () -> field.setText(value = consumer.getValue()));
        }
    };

    public static class BooleanBuilder extends ModifierBuilder<Boolean>{

        public BooleanBuilder(ModifyConsumer<Boolean> consumer){
            super(consumer);
        }

        @Override
        public void build(Table table){
            value = consumer.getValue();

            BorderImage image = new BorderImage();
            image.addAction(Actions.color(value ? Color.green : Color.red, 0.3f));

            Cons<Boolean> setColorUI = bool -> {
                value = bool;
                image.addAction(Actions.color(bool ? Color.green : Color.red, 0.3f));
                resetButton.visible = consumer.isModified();
            };

            table.button(b -> {
                b.add(image).size(32f).pad(8f).expandX().left();
                b.label(() -> value ? "true" : "false").color(EPalettes.value).expandX();
            }, Styles.clearNonei, () -> {
                setColorUI.get(!value);
                consumer.onModify(value);
            }).grow();

            addResetButton(table, () -> setColorUI.get(consumer.getValue()));
        }
    }

    public static class ContentBuilder extends ModifierBuilder<UnlockableContent>{
        protected Table contentTable;

        public ContentBuilder(ModifyConsumer<UnlockableContent> consumer){
            super(consumer);
        }

        @Override
        public void build(Table table){
            value = consumer.getValue();

            contentTable = table.button(b -> {}, Styles.clearNonei, () -> {
                Class<?> type = consumer.getTypeMeta();
                ContentType contentType = PatchJsonIO.getContentType(type);

                EUI.selector.select(contentType, type, c -> c != value, c -> {
                    setValue(c);
                    consumer.onModify(value);
                    return true;
                });
            }).grow().get();

            addResetButton(table, () -> setValue(consumer.getValue()));
            setValue(consumer.getValue());
        }

        private void setValue(UnlockableContent value){
            this.value = value;
            resetButton.visible = consumer.isModified();

            if(contentTable == null) return;

            contentTable.clearChildren();

            TextureRegion icon;
            String displayName;
            if(value == null){
                icon = Icon.none.getRegion();
                displayName = "null";
            }else{
                icon = value.uiIcon;
                displayName = value.localizedName;
            }

            contentTable.image(icon).scaling(Scaling.fit).size(40f).pad(8f).expandX().left();
            contentTable.add(displayName).pad(4f).ellipsis(true).width(64f);
        }
    }

    public static class ColorBuilder extends ModifierBuilder<String>{

        public ColorBuilder(ModifyConsumer<String> consumer){
            super(consumer);
        }

        @Override
        public void build(Table table){
            value = consumer.getValue();

            Color color0 = Color.white.cpy();
            try{
                color0 = Color.valueOf(value);
            }catch(RuntimeException ignored){
            }

            Color color = color0;
            BorderImage image = new BorderImage();
            Cons<Color> setColorUI = (c) -> {
                color.set(c);
                value = color.toString();
                image.addAction(Actions.color(color, 0.3f));
            };

            image.addAction(Actions.color(color, 0.3f));
            table.button(b -> {
                b.add(image).size(32f).pad(8f);
                b.label(() -> "#" + value).color(EPalettes.value);
            }, Styles.clearNonei, () -> {
                Vars.ui.picker.show(color, c -> {
                    setColorUI.get(c);
                    consumer.onModify(value);
                });
            }).height(48f).growX();

            addResetButton(table, () -> setColorUI.get(Color.valueOf(consumer.getValue())));
        }
    }
}