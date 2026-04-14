package MinRi2.PatchEditor.ui.dialog;

import MinRi2.PatchEditor.*;
import MinRi2.PatchEditor.ui.*;
import arc.*;
import arc.graphics.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

public class FavoritesDialog extends BaseDialog{
    private String searchText = "";
    private final ObjectSet<String> shownTypes = new ObjectSet<>();

    private ScrollPane pane;
    private final Table favoritesTable = new Table();

    public FavoritesDialog(){
        super("@patch-editor.favorites");

        resized(this::rebuild);
        shown(this::rebuild);
        hidden(() -> pane = null);

        addCloseButton();
    }

    private void rebuild(){
        cont.top();
        cont.clearChildren();

        float width = Math.min(Core.graphics.getWidth() * 0.7f, 1000f);
        cont.table(table -> {
            table.top();
            table.defaults().pad(8f).growX();

            table.table(Styles.grayPanel, this::setupSearchTable).row();

            if(pane == null) pane = new ScrollPane(favoritesTable, Styles.noBarPane);
            table.add(pane).scrollX(false).grow().row();
        }).width(width).growY();

        rebuildFavoritesTable();
        rebuildButtons();
    }
    private void rebuildButtons(){
        buttons.clearChildren();

        boolean isPortrait = Core.graphics.isPortrait();

        if(!isPortrait) buttons.button("@back", Icon.left, this::hide);

        buttons.button("@favorites.export", Icon.copy, this::exportFavorites);
        buttons.button("@favorites.import", Icon.download, this::importFavorites)
        .disabled(b -> Core.app.getClipboardText() == null || Core.app.getClipboardText().isEmpty());

        if(isPortrait){
            buttons.row();
            buttons.button("@back", Icon.left, this::hide);
        }

        buttons.button("@favorites.clear", Icon.cancel, () -> {
            Vars.ui.showConfirm("@confirm", "@favorites.clear.confirm", () -> {
                FieldFavorites.clear();
                rebuildFavoritesTable();
            });
        }).disabled(b -> FieldFavorites.fieldCount() == 0);

        for(Element child : buttons.getChildren()){
            if(child instanceof Button btn){
                for(Cell<?> cell : btn.getCells()){
                    cell.pad(8f);
                }
            }
        }
    }

    private void setupSearchTable(Table table){
        table.image(Icon.zoomSmall).size(Vars.iconMed);

        TextField field = table.add(EUI.deboundTextField(searchText, text -> {
            searchText = text;
            rebuildFavoritesTable();
        })).padLeft(4f).padRight(4f).growX().get();

        if(Core.app.isDesktop()){
            Core.scene.setKeyboardFocus(field);
        }

        table.button(Icon.cancelSmall, Styles.clearNonei, () -> {
            searchText = "";
            field.setText(searchText);
            rebuildFavoritesTable();
        }).disabled(b -> searchText.isEmpty()).width(Vars.iconSmall).growY();
    }

    private void rebuildFavoritesTable(){
        final Table table = favoritesTable;
        table.clear();
        table.top().defaults().pad(4f);

        Seq<String> fieldIds = FieldFavorites.allId().select(fieldId -> Strings.matches(searchText, fieldId));

        if(fieldIds.isEmpty()){
            table.add("@favorites.empty").pad(16f).color(Color.lightGray);
            return;
        }

        OrderedMap<String, Seq<String>> mapped = new OrderedMap<>();
        for(String stringId : fieldIds){
            String ownerType = stringId.split("#")[0];
            mapped.get(ownerType, Seq::new).add(stringId);
        }

        boolean first = true;
        int shownCount = 0;
        shownTypes.clear();
        for(Entry<String, Seq<String>> entry : mapped){
            String type = entry.key;
            Seq<String> fields = entry.value;

            if(shownCount < 100 && fields.size < 30){
                shownTypes.add(type);
                shownCount += fields.size;
            }

            table.table(top -> {
                top.image().color(Pal.darkerGray).width(32f).padRight(8f);
                top.add(type).labelAlign(Align.left).color(EPalettes.type).minWidth(64f);
                top.add("(" + fields.size + ")").color(EPalettes.grayFront).padLeft(4f);
                top.image().color(Pal.darkerGray).padLeft(8f).padRight(16f).growX();
                top.button(Icon.eyeSmall, Styles.clearTogglei, () -> {
                    if(!shownTypes.add(type)){
                        shownTypes.remove(type);
                    }
                }).size(Vars.iconMed).checked(shownTypes.contains(type));
            }).padTop(first ? 0f : 16f).growX();

            table.row();

            table.collapser(cont -> {
                for(String fieldId : fields){
                    cont.table(t -> setupFavoriteFieldTable(t, fieldId)).padBottom(4f).growX();
                    cont.row();
                }
            }, () -> shownTypes.contains(type)).growX();

            table.row();

            first = false;
        }
    }

    private void setupFavoriteFieldTable(Table table, String fieldId){
        table.background(Tex.whiteui).setColor(EPalettes.gray);

        table.table(info -> {
            info.left().defaults().left().growX();

            int split = fieldId.indexOf("#");
            String fieldName = fieldId.substring(split + 1);
            info.add(fieldName).style(Styles.outlineLabel).pad(8f);

            String note = FieldNotes.getNote(fieldId);
            if(note != null){
                info.row();
                info.add(Core.bundle.format("favorites.note.value", note)).padLeft(8f).padRight(8f).padBottom(8f).color(Pal.lightishGray).wrap().growX();
            }
        }).growX();

        table.table(buttons -> {
            buttons.defaults().width(Vars.iconMed).growY().pad(4f);
            buttons.button(Icon.editSmall, Styles.clearNonei, () -> {
                EUI.noteEditor.show(fieldId, () -> {
                    table.clear();
                    setupFavoriteFieldTable(table, fieldId);
                });
            }).tooltip("@patch-editor.note.edit");
            buttons.button(Icon.copySmall, Styles.clearNonei, () -> {
                Core.app.setClipboardText(fieldId);
                EUI.infoToast("@favorites.copy-id.succeed");
            }).tooltip("@favorites.copy-id");
            buttons.button(Icon.cancelSmall, Styles.clearNonei, () -> {
                FieldFavorites.remove(fieldId);

                table.clear();
                setupFavoriteFieldTable(table, fieldId);
            }).tooltip("@favorites.remove");
        }).growY().pad(4f);

        table.image().width(4f).color(Color.darkGray).growY().right();
        table.row();
        Cell<?> horizontalLine = table.image().height(4f).color(Color.darkGray).growX();
        horizontalLine.colspan(table.getColumns());
    }

    private void exportFavorites(){
        Core.app.setClipboardText(FieldFavorites.exportJson());
        EUI.infoToast("@favorites.export.succeed");
    }

    private void importFavorites(){
        String text = Core.app.getClipboardText();
        if(text == null || text.isEmpty()){
            EUI.infoToast("@favorites.import.failed");
            return;
        }

        try{
            FieldFavorites.importJson(text, false);
        }catch(Exception e){
            Vars.ui.showException("@favorites.import.failed", e);
            return;
        }

        rebuildFavoritesTable();
        EUI.infoToast(Core.bundle.format("favorites.import.succeed"));
    }
}