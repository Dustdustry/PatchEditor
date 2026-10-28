package dustdustry.patcheditor.ui.dialog.selector;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.Button.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import dustdustry.patcheditor.ui.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.mod.*;
import mindustry.ui.*;

import java.io.*;
import java.util.zip.*;

public class TextureRegionSelector extends SelectorDialog<AtlasRegion>{
    private static final ButtonStyle selectBorder = new ButtonStyle(){{
        down = Styles.black6;
        up = Styles.none;
        over = Styles.black3;
    }};
    private final ObjectSet<AtlasRegion> selectedRegions = new ObjectSet<>();

    public TextureRegionSelector(){
        super("@selector.texture-region");
    }

    @Override
    protected void resetSelect(){
        super.resetSelect();

        selectedRegions.clear();
    }

    @Override
    protected void setupCont(Table cont){
        float width = layoutWidth();
        int columns = (int)(width / 360f);

        ObjectMap<String, Seq<AtlasRegion>> map = new OrderedMap<>();
        Seq<AtlasRegion> dpRegions = new Seq<>();
        map.put(Core.bundle.get("selector.texture-region.custom"), dpRegions);
        for(AtlasRegion item : getItems()){
            if(item.name.startsWith(DataImagePacker.regionPrefix)){
                dpRegions.add(item);
            }else{
                map.get("" + Character.toUpperCase(item.name.charAt(0)), Seq::new).add(item);
            }
        }

        for(var entry : map){
            String key = entry.key;
            Seq<AtlasRegion> regions = entry.value;

            if(regions.isEmpty()) continue;

            cont.table(t -> {
                t.image().color(Pal.darkerGray).size(32f, 6f);
                t.add(key).color(EPalettes.type).padLeft(16f).padRight(16f).left();
                t.image().color(Pal.darkerGray).height(4f).growX();
            }).marginTop(16f).marginBottom(8f).growX();
            cont.row();
            Table regionsCont = cont.table().growX().get();
            cont.row();

            int index = 0;
            for(AtlasRegion region : regions){
                if(!query.isEmpty() && !matchQuery(region)) continue;

                Button button = regionsCont.button(table -> {
                    table.table(t -> setupItemTable(t, region)).growX();

                    table.image().width(4f).color(Color.darkGray).growY().right();
                    table.row();
                    Cell<?> horizontalLine = table.image().height(4f).color(Color.darkGray).growX();
                    horizontalLine.colspan(table.getColumns());
                }, EStyles.cardButtoni, () -> {}).pad(8f).growX().get();

                EUI.backButtonClick(button, () -> {
                    if(consumer.get(region)){
                        hide();
                    }
                });

                if(++index % columns == 0){
                    regionsCont.row();
                }
            }
        }

        map.clear();
    }

    @Override
    protected void setupItemTable(Table table, AtlasRegion item){
        table.image(item).scaling(Scaling.fit).size(Vars.iconXLarge).pad(8f);
        table.add(item.name).ellipsis(true).wrap(true).pad(8f).grow().tooltip(item.name, true);

        table.button(b -> {
            b.margin(8f);
            BorderImage image = new BorderImage(Icon.ok){
                {
                    thickness = 3f;
                }

                @Override
                public void layout(){
                    // a hacky way to keep the size of border
                    Drawable drawable = this.drawable;
                    this.drawable = Icon.ok;
                    super.layout();
                    this.drawable = drawable;
                }
            };
            b.add(image).size(Vars.iconMed).grow();
            image.update(() -> image.setDrawable(!selectedRegions.contains(item) ? null : Icon.ok));
        }, selectBorder, () -> {
            if(!selectedRegions.add(item)){
                selectedRegions.remove(item);
            }
        }).width(Vars.iconXLarge).growY().tooltip("@selector.texture-region.select", true);
    }

    @Override
    protected void setupButtons(){
        super.setupButtons();

        buttons.button("@selector.texture-region.clear", selectedRegions::clear).disabled(b -> selectedRegions.isEmpty());
        buttons.button("@selector.texture-region.export", () -> {
            FileChooser.save("zip").name("export").submit(file -> {
                int count = selectedRegions.size;

                try(OutputStream stream = file.write(false, 4096); ZipOutputStream zip = new ZipOutputStream(stream)){
                    zip.setLevel(Deflater.NO_COMPRESSION);

                    for(AtlasRegion region : selectedRegions){
                        zip.putNextEntry(new ZipEntry(region.name + ".png"));
                        zip.write(readPng(region));
                        zip.closeEntry();
                    }
                }catch(Throwable e){
                    Vars.ui.showException(e);
                    return;
                }

                EUI.infoToast(Core.bundle.format("selector.texture-region.exported", count));
            });
        }).disabled(b -> selectedRegions.isEmpty());
    }

    @Override
    protected Seq<AtlasRegion> getItems(){
        return Core.atlas.getRegionMap().values().toSeq().sortComparing(region -> region.name);
    }

    @Override
    protected boolean matchQuery(AtlasRegion item){
        return Strings.matches(query, item.name);
    }

    private static byte[] readPng(AtlasRegion region){
        int width = region.width, height = region.height;

        Mat projection = Draw.proj(), transform = Draw.trans();
        Mat oldProjection = Tmp.m1.set(projection), oldTransform = Tmp.m2.set(transform);
        FrameBuffer buffer = new FrameBuffer(width, height);

        try{
            Draw.flush();
            buffer.begin(Color.clear);
            projection.setOrtho(0f, 0f, width, height);
            transform.idt();
            Draw.blend(Blending.disabled);
            Draw.color();
            Draw.rect(region, width / 2f, height / 2f, width, height);
            Draw.flush();

            Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, width, height, true);
            try{
                return PixmapIO.writePngBytes(pixmap);
            }finally{
                pixmap.dispose();
            }
        }catch(IOException e){
            throw new ArcRuntimeException("Failed to encode region '" + region.name + "'", e);
        }finally{
            Draw.flush();
            projection.set(oldProjection);
            transform.set(oldTransform);
            Draw.blend();
            buffer.end();
            buffer.dispose();
        }
    }
}
