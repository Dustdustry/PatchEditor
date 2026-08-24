package dustdustry.patcheditor.ui.dialog;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import dustdustry.patcheditor.*;
import dustdustry.patcheditor.core.*;
import dustdustry.patcheditor.export.ObjectExporter.*;
import arc.*;
import arc.struct.*;
import dustdustry.patcheditor.core.JsonProcessor.*;
import dustdustry.patcheditor.ui.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.ui.dialogs.SettingsMenuDialog.*;
import mindustry.ui.dialogs.SettingsMenuDialog.SettingsTable.*;

import javax.lang.model.element.*;
import java.time.format.*;

import static arc.Core.settings;
import static mindustry.ui.Styles.*;

public class EditorSettings extends BaseDialog{
    private static final TextButtonStyle grayTogglet = new TextButtonStyle(){{
        down = over = Styles.flatOver;
        disabled = Styles.grayPanelDark;
        up = Styles.grayPanel;
        checked = Styles.flatDown;
        font = Fonts.def;
        fontColor = EPalettes.gray;
        downFontColor = overFontColor = EPalettes.lighterGray;
        checkedFontColor = Color.white;
    }};

    public EditorSettings(){
        super("@patch-editor.settings");

        fallback();

        shown(() -> {
            if(!cont.hasChildren()) setup();
        });
    }

    private void fallback(){
        if(settings.has("patch-editor.simplifyPatch")){
            boolean simplifyPatch = settings.getBool("patch-editor.simplifyPatch");
            settings.put("patch-editor.simplifyPath", simplifyPatch);
            settings.remove("patch-editor.simplifyPatch");
        }
    }

    private void setup(){
        SettingsTable table = new SettingsTable();
        Seq<Setting> settings = table.getSettings();
        cont.pane(Styles.noBarPane, table);

        table.checkPref("patch-editor.simplifyPath", true);
        table.checkPref("patch-editor.sugar.stacks", true);
        table.checkPref("patch-editor.magicExport.allowDefault", false);
        table.checkPref("patch-editor.editNotes", false);
        table.checkPref("patch-editor.rememberPath", false);
        table.checkPref("patch-editor.formatJson", false);

        table.sliderPref("patch-editor.undoLimit", 20, 0, 160, 20,s -> Core.bundle.format("setting.patch-editor.undoLimit.text", s));
        settings.add(new SingleEnumSettings("patch-editor.exportType", ExportType.values(), ExportType.hjson));
        settings.add(new SlotSettings(16f, cont -> {
            cont.button(t -> {
                t.background(Styles.grayPanel).left();

                t.image().color(Pal.lightishGray).width(16f).padRight(16f).growY();
                t.image(new TextureRegion(EVars.thisMod.iconTexture)).padTop(8f).padBottom(8f).size(Vars.iconXLarge);
                t.table(info -> {
                    info.defaults().expandX().left();
                    info.add(EVars.thisMod.meta.displayName);
                    info.row();
                    info.add(UI.formatIcons(Core.bundle.get("patch-editor.bannerInfo"))).color(Pal.lightishGray).padTop(4f);
                }).padLeft(8f);

                t.add().expandX();

                t.image().color(Pal.lightishGray).size(2f, 16f).padRight(16f);
                t.add(EVars.thisMod.meta.author).padRight(16f);
            }, graySquarei, () -> Core.app.openURI(EVars.repoLink)).grow();
        }));

        table.rebuild();
        addCloseButton();
    }

    public static ExportConfig getExportConfig(){
        ExportConfig config = new ExportConfig();
        config.allowDefault = settings.getBool("patch-editor.magicExport.allowDefault");
        return config;
    }

    public static PatchExportOptions getPatchExportOptions(){
        String exportType = settings.getString("patch-editor.exportType");
        OutputFormat format = ExportType.hjson.is(exportType) ? OutputFormat.hjson : OutputFormat.json;
        return new PatchExportOptions(
        settings.getBool("patch-editor.sugar.stacks"),
        settings.getBool("patch-editor.simplifyPath"),
        settings.getBool("patch-editor.formatJson"),
        format
        );
    }

    public enum ExportType{
        hjson, json;

        public boolean is(String name){
            return name().equals(name);
        }
    }

    public static class SingleEnumSettings extends Setting{
        public Enum<?>[] enums;
        public Enum<?> def;

        public SingleEnumSettings(String name, Enum<?>[] enums, Enum<?> def){
            super(name);

            this.enums = enums;
            this.def = def;

            settings.defaults(name, def.name());
        }

        @Override
        public void add(SettingsTable table){
            table.table(cont -> {
                cont.background(Styles.grayPanel).margin(8f);

                cont.table(top -> {
                    top.left();
                    top.image(Icon.settingsSmall);
                    top.add(title).padLeft(8f);
                }).growX().row();

                cont.table(buttons -> {
                    for(Enum<?> anEnum : enums){
                        String text = Core.bundle.get(name + "." + anEnum.name(), anEnum.name());
                        buttons.button(text, grayTogglet, () -> settings.put(name, anEnum.name()))
                        .margin(8f).growX().checked(b -> anEnum.name().equals(settings.getString(name)));
                    }
                }).padTop(4f).growX();

                addDesc(cont);
            }).padTop(6f).growX();

            table.row();
        }
    }

    public static class SlotSettings extends Setting{
        public float padding;
        protected Cons<Table> cons;

        public SlotSettings(Cons<Table> cons){
            this(6f, cons);
        }

        public SlotSettings(float padding, Cons<Table> cons){
            super("");

            this.cons = cons;
            this.padding = padding;
        }

        @Override
        public void add(SettingsTable table){
            table.table(cons).padTop(padding).padBottom(padding).growX().row();
            table.row();
        }
    }
}
