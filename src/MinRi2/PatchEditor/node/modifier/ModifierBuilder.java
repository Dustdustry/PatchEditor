package MinRi2.PatchEditor.node.modifier;

import MinRi2.PatchEditor.node.*;
import MinRi2.PatchEditor.ui.*;
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

// Define ui element fields that need data
public abstract class ModifierBuilder<T>{
    protected T value;
    protected @Nullable Button resetButton;
    protected final ModifyConsumer<T> consumer;

    public ModifierBuilder(ModifyConsumer<T> consumer){
        this.consumer = consumer;
    }

    public abstract void build(Table table);

    // Sync ui here
    protected void setValue(T value){
        this.value = value;
        consumer.onModify(value);
        if(resetButton != null) resetButton.visible = consumer.isModified();
    }

    protected void addResetButton(Table table){
        resetButton = table.button(Icon.undo, Styles.clearNonei, () -> {
            setValue(consumer.getDefaultValue());
        }).visible(consumer.isModified()).width(32f).pad(4f).growY().expandX().right().tooltip("@node-modifier.undo", true).get();
    }

    public static class TextBuilder extends ModifierBuilder<String>{
        protected TextField field;

        public TextBuilder(ModifyConsumer<String> consumer){
            super(consumer);
        }

        @Override
        protected void setValue(String value){
            super.setValue(value);

            field.setText(value);
        }

        @Override
        public void build(Table table){
            value = consumer.getValue();

            field = table.field(value, this::setValue)
            .valid(consumer::checkValue).pad(4f).width(100f).get();

            if(consumer.getTypeMeta() == String.class){
                table.button(Icon.pencil, Styles.clearNonei, () -> {
                    BaseDialog dialog = new BaseDialog("##Edit Text");
                    Table cont = dialog.cont;

                    cont.add(dialog.titleTable).fillX().row();
                    cont.area(value, Styles.areaField, this::setValue)
                    .valid(consumer::checkValue).minSize(400f, 600f);

                    dialog.addCloseButton();
                    dialog.show();
                }).pad(4f).width(32f).growY();
            }

            addResetButton(table);
        }
    }

    public static class BooleanBuilder extends ModifierBuilder<Boolean>{
        protected Image image;

        public BooleanBuilder(ModifyConsumer<Boolean> consumer){
            super(consumer);
        }

        @Override
        protected void setValue(Boolean value){
            super.setValue(value);

            image.clearActions();
            image.addAction(Actions.color(value ? Color.green : Color.red, 0.3f));
        }

        @Override
        public void build(Table table){
            value = consumer.getValue();

            image = new BorderImage();
            image.addAction(Actions.color(value ? Color.green : Color.red, 0.3f));

            table.button(b -> {
                b.add(image).size(32f).pad(8f).expandX().left();
                b.label(() -> value ? "true" : "false").color(EPalettes.value).expandX();
            }, Styles.clearNonei, () -> {
                setValue(!value);
            }).grow();

            addResetButton(table);
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
                    return true;
                });
            }).grow().get();

            addResetButton(table);
            rebuildTable();
        }

        protected void setValue(UnlockableContent value){
            super.setValue(value);
            if(contentTable != null) rebuildTable();
        }

        private void rebuildTable(){
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
        protected Image image;

        public ColorBuilder(ModifyConsumer<String> consumer){
            super(consumer);
        }

        @Override
        protected void setValue(String value){
            super.setValue(value);

            image.addAction(Actions.color(Color.valueOf(value), 0.3f));
        }

        @Override
        public void build(Table table){
            value = consumer.getValue();
            image = new BorderImage();

            Color color0 = Color.white.cpy();
            try{
                color0 = Color.valueOf(value);
            }catch(RuntimeException ignored){
            }

            Color color = color0;
            image.addAction(Actions.color(color, 0.3f));
            table.button(b -> {
                b.left();
                b.add(image).size(32f).pad(8f);
                b.label(() -> "#" + value).ellipsis(true).color(EPalettes.value).minWidth(64f).growX();
            }, Styles.clearNonei, () -> {
                Vars.ui.picker.show(color, c -> setValue(c.toString()));
            }).grow();

            addResetButton(table);
        }
    }

    public static class WeaponNameBuilder extends TextBuilder{
        public WeaponNameBuilder(ModifyConsumer<String> consumer){
            super(consumer);
        }

        @Override
        public void build(Table table){
            value = consumer.getValue();

            field = table.field(value, this::setValue)
            .valid(consumer::checkValue).pad(4f).width(100f).get();

            table.button(Icon.book, Styles.clearNonei, () -> {
                EUI.weaponSelector.select(weapon -> {
                    setValue(weapon.name);
                    return true;
                });
            }).pad(4f).width(48f).growY().tooltip("@weapon-selector.tooltip");

            addResetButton(table);
        }
    }
}