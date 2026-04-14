package MinRi2.PatchEditor.ui.dialog;

import MinRi2.PatchEditor.*;
import MinRi2.PatchEditor.ui.*;
import MinRi2.PatchEditor.FieldFavorites.*;
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

    private final Table favoritesTable = new Table();

    public FavoritesDialog(){
        super("@patch-editor.favorites");

        resized(this::rebuild);
        shown(this::rebuild);

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

            table.pane(Styles.noBarPane, favoritesTable).scrollX(false).grow().row();

            table.table(Styles.grayPanel, buttons -> {
                buttons.defaults().growX().height(42f);

                buttons.button("@favorites.export", Icon.copy, Styles.cleart, this::exportFavorites);
                buttons.button("@favorites.import", Icon.download, Styles.cleart, this::importFavorites)
                .disabled(b -> Core.app.getClipboardText() == null || Core.app.getClipboardText().isEmpty());
                buttons.button("@favorites.clear", Icon.cancel, Styles.cleart, () -> {
                    Vars.ui.showConfirm("@confirm", "@favorites.clear.confirm", () -> {
                        FieldFavorites.clear();
                        rebuildFavoritesTable();
                    });
                }).disabled(b -> FieldFavorites.all().isEmpty());

                for(Element child : buttons.getChildren()){
                    if(child instanceof Button btn){
                        for(Cell<?> cell : btn.getCells()){
                            cell.pad(8f);
                        }
                    }
                }
            });
        }).width(width).growY();

        rebuildFavoritesTable();
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

        Seq<FavoriteField> favorites = FieldFavorites.all().select(favorite ->
        Strings.matches(searchText, favorite.displayName())
        || Strings.matches(searchText, favorite.id));

        if(favorites.isEmpty()){
            table.add("@favorites.empty").pad(16f).color(Color.lightGray);
            return;
        }

        OrderedMap<String, Seq<FavoriteField>> mapped = new OrderedMap<>();
        for(FavoriteField favorite : favorites){
            mapped.get(favorite.ownerType, Seq::new).add(favorite);
        }

        boolean first = true;
        int shownCount = 0;
        shownTypes.clear();
        for(Entry<String, Seq<FavoriteField>> entry : mapped){
            String type = entry.key;
            Seq<FavoriteField> fields = entry.value;

            if(shownCount < 100) shownTypes.add(type);

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
                for(FavoriteField field : fields){
                    cont.table(t -> setupFavoriteFieldTable(t, field)).padBottom(4f).growX();
                    cont.row();
                }
            }, () -> shownTypes.contains(type)).growX();

            table.row();

            shownCount += fields.size;
            first = false;
        }
    }

    private void setupFavoriteFieldTable(Table table, FavoriteField favorite){
        table.background(Tex.whiteui).setColor(EPalettes.gray);

        table.table(info -> {
            info.left().defaults().left().growX();
            info.add(favorite.displayName()).pad(8f).tooltip(favorite.id);

            String note = FieldNotes.getNote(favorite.id);
            if(note != null){
                info.row();
                info.add(Core.bundle.format("favorites.note.value", note)).padLeft(8f).padRight(8f).padBottom(8f).color(Color.lightGray).wrap().growX();
            }
        }).growX();

        table.table(buttons -> {
            buttons.defaults().size(Vars.iconSmall).pad(4f);
            buttons.button(Icon.editSmall, Styles.clearNonei, () -> {
                EUI.noteEditor.show(favorite.id);
            }).tooltip("@patch-editor.note.edit");
            buttons.button(Icon.copySmall, Styles.clearNonei, () -> {
                Core.app.setClipboardText(favorite.id);
                EUI.infoToast("@favorites.copy-id.succeed");
            }).tooltip("@favorites.copy-id");
            buttons.button(Icon.cancelSmall, Styles.clearNonei, () -> {
                FieldFavorites.remove(favorite.id);
                rebuildFavoritesTable();
            }).tooltip("@favorites.remove");
        }).pad(4f);

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

        int imported;
        try{
            imported = FieldFavorites.importJson(text, false);
        }catch(Exception e){
            Vars.ui.showException("@favorites.import.failed", e);
            return;
        }

        rebuildFavoritesTable();
        EUI.infoToast(Core.bundle.format("favorites.import.succeed", imported));
    }
}