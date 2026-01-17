package MinRi2.PatchEditor.node.modifier;

import MinRi2.PatchEditor.node.*;
import MinRi2.PatchEditor.ui.*;
import MinRi2.PatchEditor.ui.editor.*;
import arc.*;
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

    public void buildTable(Table table){
        value = consumer.getValue();
        build(table);
        updateUI();
    }

    protected abstract void build(Table table);

    // Sync ui here
    protected void setValue(T value){
        this.value = value;
        consumer.onModify(value);

        updateUI();
    }

    protected void updateUI(){
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
        public void build(Table table){
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

        @Override
        protected void updateUI(){
            super.updateUI();

            field.setText(value);
        }
    }

    public static class BooleanBuilder extends ModifierBuilder<Boolean>{
        protected Image image;

        public BooleanBuilder(ModifyConsumer<Boolean> consumer){
            super(consumer);
        }

        @Override
        public void build(Table table){
            table.button(b -> {
                b.add(image = new BorderImage()).size(32f).pad(8f).expandX().left();
                b.label(() -> value ? "true" : "false").color(EPalettes.value).expandX();
            }, Styles.clearNonei, () -> {
                setValue(!value);
            }).grow();

            addResetButton(table);
        }

        @Override
        protected void updateUI(){
            super.updateUI();

            image.clearActions();
            image.addAction(Actions.color(value ? Color.green : Color.red, 0.3f));
        }
    }

    public static class ContentBuilder extends ModifierBuilder<Content>{
        protected Table contentTable;

        public ContentBuilder(ModifyConsumer<Content> consumer){
            super(consumer);
        }

        @Override
        public void build(Table table){
            contentTable = table.button(b -> {}, Styles.clearNonei, () -> {
                Class<?> type = consumer.getTypeMeta();
                consumer.getDefaultValue().getContentType();
                ContentType contentType = PatchJsonIO.classContentType(type);

                EUI.selector.select(contentType, type, c -> c != value, c -> {
                    setValue(c);
                    return true;
                });
            }).grow().get();

            addResetButton(table);
        }

        @Override
        protected void updateUI(){
            super.updateUI();

            contentTable.clearChildren();
            contentTable.image(NodeDisplay.getDisplayIcon(value)).scaling(Scaling.fit).size(40f).pad(8f).expandX().left();
            contentTable.add(NodeDisplay.getDisplayName(value)).pad(4f).ellipsis(true).width(64f);
        }
    }

    public static class ColorBuilder extends ModifierBuilder<String>{
        protected Image image;

        public ColorBuilder(ModifyConsumer<String> consumer){
            super(consumer);
        }

        @Override
        public void build(Table table){
            table.button(b -> {
                b.add(image = new BorderImage()).size(32f).pad(8f);
                b.label(() -> "#" + value).ellipsis(true).color(EPalettes.value).minWidth(64f).growX();
            }, Styles.clearNonei, () -> {
                Vars.ui.picker.show(Tmp.c1, c -> setValue(c.toString()));
            }).grow();

            addResetButton(table);
        }

        @Override
        protected void updateUI(){
            super.updateUI();

            Color color = Color.white.cpy();
            try{
                color = Color.valueOf(value);
            }catch(RuntimeException ignored){
            }
            image.addAction(Actions.color(color, 0.3f));
        }
    }

    public static class TextureRegionBuilder extends ModifierBuilder<String>{
        protected Image image;
        protected Label label;

        public TextureRegionBuilder(ModifyConsumer<String> consumer){
            super(consumer);
        }

        @Override
        public void build(Table table){
            table.button(b -> {
                b.left();
                image = b.image().scaling(Scaling.fit).size(Vars.iconXLarge).pad(8f).get();
                label = b.label(() -> value).ellipsis(true).color(EPalettes.value).minWidth(64f).growX().get();
            }, Styles.clearNonei, () -> {
                EUI.textureRegionSelector.select(region -> {
                    setValue(region.name);
                    return true;
                });
            }).grow();

            addResetButton(table);
        }

        @Override
        protected void updateUI(){
            super.updateUI();

            TextureRegion region = Core.atlas.getRegionMap().get(value);
            image.setDrawable(region == null ? Icon.none.getRegion() : region);
            label.setText(value);
        }
    }
}