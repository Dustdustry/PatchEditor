package MinRi2.PatchEditor.ui.editor;

import MinRi2.PatchEditor.ui.*;
import MinRi2.PatchEditor.ui.editor.FavoriteFields.*;
import arc.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

public class FavoritesDialog extends BaseDialog{
    private final Table favoritesContainer = new Table();
    private final Table favoritesTable = new Table();

    private String searchText = "";

    public FavoritesDialog(){
        super("@patch-editor.favorites");

        resized(this::rebuildContent);
        shown(() -> {
            setup();
            rebuildContent();
        });

        addCloseButton();
    }

    private void setup(){
        if(cont.hasChildren()) return;

        cont.top();
        cont.add(favoritesContainer).width(containerWidth()).growY();
    }

    private void rebuildContent(){
        setup();
        Cell<?> containerCell = cont.getCell(favoritesContainer);
        if(containerCell != null){
            containerCell.width(containerWidth()).growY();
        }

        Table table = favoritesContainer;
        table.clearChildren();
        table.top();

        table.table(Styles.grayPanel, this::setupSearchTable).pad(8f).growX();
        table.row();

        table.pane(Styles.noBarPane, favoritesTable).scrollX(false).pad(8f).grow();
        table.row();

        table.table(Styles.grayPanel, buttons -> {
            buttons.defaults().minWidth(160f).height(42f).pad(8f).growX();

            buttons.button("@favorites.export", Icon.copy, Styles.cleart, this::exportFavorites);
            buttons.button("@favorites.import", Icon.download, Styles.cleart, this::importFavorites)
            .disabled(b -> Core.app.getClipboardText() == null || Core.app.getClipboardText().isEmpty());
            buttons.button("@favorites.clear", Icon.cancel, Styles.cleart, () -> {
                Vars.ui.showConfirm("@confirm", "@favorites.clear.confirm", () -> {
                    FavoriteFields.clear();
                    rebuildFavoritesTable();
                });
            }).disabled(b -> FavoriteFields.all().isEmpty());
        }).pad(8f).growX();

        rebuildFavoritesTable();
    }

    private float containerWidth(){
        float available = Core.graphics.getWidth() / Scl.scl() - 64f;
        return Math.max(360f, Math.min(760f, available));
    }

    private void setupSearchTable(Table table){
        table.image(Icon.zoomSmall).size(Vars.iconSmall);

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
        favoritesTable.defaults().growX().pad(4f);

        Seq<FavoriteField> favorites = FavoriteFields.all().select(favorite ->
        Strings.matches(searchText, favorite.displayName()) || Strings.matches(searchText, favorite.id));

        if(favorites.isEmpty()){
            favoritesTable.add("@favorites.empty").pad(16f).color(Color.lightGray);
            return;
        }

        for(FavoriteField favorite : favorites){
            favoritesTable.table(Tex.whiteui, row -> {
                row.add(favorite.displayName()).left().growX().pad(8f).tooltip(favorite.id);

                row.table(buttons -> {
                    buttons.defaults().size(32f).pad(4f);
                    buttons.button(Icon.copySmall, Styles.clearNonei, () -> {
                        Core.app.setClipboardText(favorite.id);
                        EUI.infoToast("@favorites.copy-id.succeed");
                    }).tooltip("@favorites.copy-id");
                    buttons.button(Icon.cancelSmall, Styles.clearNonei, () -> {
                        FavoriteFields.remove(favorite.id);
                        rebuildFavoritesTable();
                    }).tooltip("@favorites.remove");
                }).pad(4f);

                row.image().width(4f).color(Color.darkGray).growY().right();
                row.row();
                Cell<?> horizontalLine = row.image().height(4f).color(Color.darkGray).growX();
                horizontalLine.colspan(row.getColumns());
            }).color(EPalettes.gray);
            favoritesTable.row();
        }
    }

    private void exportFavorites(){
        Core.app.setClipboardText(FavoriteFields.exportJson());
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
            imported = FavoriteFields.importJson(text, false);
        }catch(Exception e){
            Vars.ui.showException("@favorites.import.failed", e);
            return;
        }

        rebuildFavoritesTable();
        EUI.infoToast(Core.bundle.format("favorites.import.succeed", imported));
    }
}
