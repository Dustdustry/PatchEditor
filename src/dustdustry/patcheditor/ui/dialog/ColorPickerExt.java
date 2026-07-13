package dustdustry.patcheditor.ui.dialog;

import arc.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import dustdustry.patcheditor.ui.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

public class ColorPickerExt{

    private static Color current;
    private static TextField hexField;

    public static void init(){
        Vars.ui.picker.shown(() -> Core.app.post(ColorPickerExt::tryAddPalTable));
    }

    private static void tryAddPalTable(){
        ColorPicker picker = Vars.ui.picker;
        try{
            if(current == null) current = Reflect.get(ColorPicker.class, picker, "current");
            hexField = Reflect.get(ColorPicker.class, picker, "hexField");
            Table table = (Table)hexField.parent;
            table.row();
            table.button("@selector.color", Icon.editSmall, Styles.grayt, () -> EUI.colorSelector.select(entry -> {
                boolean lastChange = hexField.getProgrammaticChangeEvents();
                hexField.setProgrammaticChangeEvents(true);
                hexField.setText(entry.color.toString());
                hexField.setProgrammaticChangeEvents(lastChange);
                return true;
            })).height(Vars.iconLarge).padTop(8f);
        }catch(Exception e){
            Vars.ui.showException("@patch-editor.colorPickerExt.failed", e);
            Log.err(e);
        }
    }
}
