package dustdustry.patcheditor.ui.dialog;

import dustdustry.patcheditor.ui.*;
import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.mod.data.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

public class MissingRegionsDialog extends BaseDialog{
    private static final float itemWidth = 300f;

    private final Seq<String> missing = new Seq<>();

    private String query = "";

    private Table itemCont;
    private ScrollPane pane;

    public MissingRegionsDialog(){
        super("@patch-editor.missingRegions.title");

        resized(this::rebuild);
        shown(() -> {
            runTrace();
            rebuild();
        });
        addCloseButton();
    }

    protected float layoutWidth(){
        return Core.scene.getWidth() / Scl.scl() * (Core.scene.getWidth() > 1000 ? 0.7f : 0.9f);
    }

    protected void runTrace(){
        if(Vars.state.data == null) return;

        RegionsTracker tracker = new RegionsTracker().begin();
        try{
            Vars.state.data.reloadContent(false);
        }catch(Throwable e){
            Log.err("Failed to trace missing regions", e);
        }finally{
            tracker.end();
        }

        missing.clear();
        missing.addAll(tracker.getMissingRegions());
        missing.sort();
    }

    protected void rebuild(){
        if(itemCont == null) itemCont = new Table();
        if(pane == null) pane = new ScrollPane(itemCont);

        cont.clearChildren();
        cont.defaults().uniformX().fillX();
        cont.table(this::setupSearchTable).row();
        cont.add(UI.formatIcons(Core.bundle.get("patch-editor.missingRegions.description"))).wrap().color(EPalettes.grayFront).pad(8f).row();
        cont.add(pane).scrollX(false).width(layoutWidth()).growY();

        itemCont.clearChildren();
        setupCont(itemCont);
    }

    protected void setupSearchTable(Table table){
        table.image(Icon.zoom).pad(8f);
        TextField field = table.field(query, s -> {
            query = s;
            itemCont.clearChildren();
            setupCont(itemCont);
        }).growX().get();
        table.button(Icon.cancel, Styles.cleari, () -> {
            query = "";
            itemCont.clearChildren();
            setupCont(itemCont);
        }).pad(8f);

        table.button(Icon.refresh, Styles.cleari, () -> {
            runTrace();
            rebuild();
        }).pad(8f).tooltip("@patch-editor.missingRegions.refresh", true);

        table.label(() -> Core.bundle.format("patch-editor.missingRegions.count", missing.size)).color(EPalettes.grayFront).pad(8f);

        field.update(() -> {
            if(!field.hasKeyboard()){
                field.requestKeyboard();
                field.setText(query);
            }
        });
    }

    protected void setupCont(Table cont){
        if(missing.isEmpty()){
            cont.add("@patch-editor.missingRegions.none").color(EPalettes.grayFront).pad(24f).center();
            return;
        }

        float width = layoutWidth();
        int index = 0, columns = Math.max(1, (int)(width / itemWidth));
        float labelWidth = itemWidth - 90f;

        for(String name : missing){
            if(!query.isEmpty() && !Strings.matches(query, name)) continue;

            Button btn = cont.button(table -> {
                table.table(t -> {
                    t.defaults().left();
                    t.image(Tex.nomap).size(Vars.iconLarge).pad(4f);
                    t.add(name).wrap().left().growX().width(labelWidth).pad(6f);
                    t.button(Icon.copy, Styles.clearNonei, () -> copyName(name)).size(36f).pad(4f).tooltip("@patch-editor.missingRegions.copy", true);
                }).growX().pad(2f);

                table.image().width(4f).color(Color.darkGray).growY().right();
                table.row();
                Cell<?> horizontalLine = table.image().height(4f).color(Color.darkGray).growX();
                horizontalLine.colspan(table.getColumns());
            }, EStyles.cardButtoni, () -> {}).pad(4f).tooltip("@patch-editor.missingRegions.hint", true).get();

            EUI.backButtonClick(btn, () -> FileChooser.open("png").submit(fi -> importRegion(name, fi)));

            if(++index % columns == 0){
                cont.row();
            }
        }
    }

    protected void copyName(String name){
        try{
            Core.app.setClipboardText(name);
            EUI.copiedToast(name);
        }catch(Exception e){
            Vars.ui.showException(e);
        }
    }

    protected void importRegion(String regionName, Fi file){
        byte[] bytes;
        try{
            bytes = file.readBytes();
        }catch(Exception e){
            Vars.ui.showException(e);
            return;
        }

        Vars.ui.loadAnd(() -> {
            try{
                String name = regionName.startsWith(DataImagePacker.regionPrefix)
                ? regionName.substring(DataImagePacker.regionPrefix.length())
                : regionName;
                name = Strings.sanitizeFilename(name);
                final String importName = name;

                String path = importName + ".png";
                Vars.state.data.getImages().removeAll(i -> i.name.equals(importName) || i.path.equals(path));

                ImageAsset image = new ImageAsset();
                image.setPath(path);
                image.updateData(bytes);
                Vars.state.data.getImages().add(image);
                Vars.state.data.reloadImages();

                runTrace();
                rebuild();

                if(Core.atlas.find(regionName).found()){
                    EUI.infoToast(Core.bundle.format("patch-editor.missingRegions.imported", regionName));
                }else{
                    EUI.infoToast(Core.bundle.format("patch-editor.missingRegions.importedButMissing", DataImagePacker.regionPrefix + importName, regionName));
                }
            }catch(Throwable e){
                Vars.ui.showException(e);
            }
        });
    }
}
