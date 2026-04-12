package MinRi2.PatchEditor.ui.dialog;

import MinRi2.PatchEditor.*;
import MinRi2.PatchEditor.ui.*;
import MinRi2.PatchEditor.NodeFavorites.*;
import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

public class FavoritesDialog extends BaseDialog{
    private final Table favoritesTable = new Table();

    private String searchText = "";

    public FavoritesDialog(){
        super("@patch-editor.favorites");

        resized(this::rebuild);
        shown(this::rebuild);

        addCloseButton();
    }

    private void rebuild(){
        cont.top();
        cont.clearChildren();

        float width = Math.min(Math.max(600f, Core.graphics.getWidth() * 0.7f), 1000f);
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
                        NodeFavorites.clear();
                        rebuildFavoritesTable();
                    });
                }).disabled(b -> NodeFavorites.all().isEmpty());

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
        favoritesTable.clearChildren();
        favoritesTable.defaults().pad(4f);

        Seq<FavoriteField> favorites = NodeFavorites.all().select(favorite ->
        Strings.matches(searchText, favorite.displayName())
        || Strings.matches(searchText, favorite.id));

        if(favorites.isEmpty()){
            favoritesTable.add("@favorites.empty").pad(16f).color(Color.lightGray);
            return;
        }

        for(FavoriteField favorite : favorites){
            favoritesTable.table(Tex.whiteui, row -> {
                row.table(info -> {
                    info.left().defaults().left().growX();
                    info.add(favorite.displayName()).pad(8f).tooltip(favorite.id);

                    String note = FieldNotes.getNote(favorite.id);
                    if(note != null){
                        info.row();
                        info.add(Core.bundle.format("favorites.note.value", note)).padLeft(8f).padRight(8f).padBottom(8f).color(Color.lightGray).wrap().growX();
                    }
                }).growX();

                row.table(buttons -> {
                    buttons.defaults().size(Vars.iconSmall).pad(4f);
                    buttons.button(Icon.editSmall, Styles.clearNonei, () -> {
                        EUI.fieldNote.show(favorite.id);
                    }).tooltip("@patch-editor.note.edit");
                    buttons.button(Icon.copySmall, Styles.clearNonei, () -> {
                        Core.app.setClipboardText(favorite.id);
                        EUI.infoToast("@favorites.copy-id.succeed");
                    }).tooltip("@favorites.copy-id");
                    buttons.button(Icon.cancelSmall, Styles.clearNonei, () -> {
                        NodeFavorites.remove(favorite.id);
                        rebuildFavoritesTable();
                    }).tooltip("@favorites.remove");
                }).pad(4f);

                row.image().width(4f).color(Color.darkGray).growY().right();
                row.row();
                Cell<?> horizontalLine = row.image().height(4f).color(Color.darkGray).growX();
                horizontalLine.colspan(row.getColumns());
            }).color(EPalettes.gray).growX();
            favoritesTable.row();
        }
    }

    private void exportFavorites(){
        Core.app.setClipboardText(NodeFavorites.exportJson());
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
            imported = NodeFavorites.importJson(text, false);
        }catch(Exception e){
            Vars.ui.showException("@favorites.import.failed", e);
            return;
        }

        rebuildFavoritesTable();
        EUI.infoToast(Core.bundle.format("favorites.import.succeed", imported));
    }
}