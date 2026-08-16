package dustdustry.patcheditor.ui.dialog;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import dustdustry.patcheditor.core.*;
import dustdustry.patcheditor.ui.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import java.lang.reflect.*;

public class ProgressTutorialDialog extends BaseDialog{
    private static final String[] buildSignatures = {"build(progress)", "build(number)", "build(progress, interp)"};

    public ProgressTutorialDialog(){
        super("@patch-editor.editProgress.tutorial.title");

        shown(() -> {
            if(!cont.hasChildren()){
                cont.pane(Styles.noBarPane, content -> {
                    content.top().left();
                    setupIntroSection(content);
                    setupBuildSection(content);
                    setupOpsSection(content);
                    setupVars(content);
                }).width(layoutWidth()).grow();
            }
        });
        addCloseButton();
    }

    private static float layoutWidth(){
        return Math.min(Core.graphics.getWidth() * 0.7f, 1000f);
    }

    private static String bundle(String key){
        return Core.bundle.get("patch-editor.editProgress.tutorial." + key, null);
    }

    private static void sectionHeader(Table table, String title, boolean first){
        table.add(title).style(Styles.outlineLabel).padTop(first ? 8f : 20f).left().growX();
        table.row();
    }

    private static void groupHeader(Table table, String title){
        table.add(title).padTop(12f).left().growX();
        table.row();
    }

    private static void text(Table table, String textKey){
        table.add(bundle(textKey)).wrap().padTop(8f).growX();
        table.row();
    }

    private static void grid(Table table, float itemWidth, int items, Cons2<Table, Integer> cell){
        Table grid = table.table().padLeft(16f).growX().get();
        table.row();

        int columns = (int)(layoutWidth() / Scl.scl() / itemWidth);
        for(int i = 0; i < items; i++){
            int finalI = i;
            grid.table(Styles.grayPanel, t -> {
                t.left().margin(6f);
                cell.get(t, finalI);
            }).pad(6f).grow();
            if((i + 1) % Math.max(1, columns) == 0) grid.row();
        }
    }

    private static void opCell(Table grid, String signature, String params, String desc){
        grid.table(t -> {
            t.background(Styles.grayPanel);
            t.add(signature).style(Styles.outlineLabel).color(EPalettes.value).padBottom(4f).expandX().left();

            t.defaults().padTop(4f);
            t.row();
            if(params != null && !params.isEmpty()){
                t.add(params).color(EPalettes.gray).expandX().left();
                t.row();
            }
            if(desc != null && !desc.isEmpty()){
                t.add(desc).color(EPalettes.gray).growX();
            }
        }).pad(8f);
    }

    private void setupIntroSection(Table table){
        sectionHeader(table, bundle("section.1.title"), true);
        text(table, "section.1.text.1");
        text(table, "section.1.text.2");
        table.row();
    }

    private void setupBuildSection(Table table){
        sectionHeader(table, bundle("section.2.title"), false);
        text(table, "section.2.text.1");

        grid(table, 460f, buildSignatures.length, (grid, i) -> {
            String prefix = "build." + (i + 1);
            opCell(grid, buildSignatures[i], bundle(prefix + ".params"), bundle(prefix + ".desc"));
        });
    }

    private void setupOpGroup(Table table, String groupKey, TutorialOp... ops){
        groupHeader(table, bundle(groupKey));

        grid(table, 460f, ops.length, (t, i) -> {
            TutorialOp op = ops[i];
            t.add(op.signature).color(EPalettes.value).padRight(16f);
            t.add(op.formula).color(EPalettes.gray).wrap().growX();
        });
    }

    private void setupOpsSection(Table table){
        sectionHeader(table, bundle("section.3.title"), false);
        text(table, "section.3.text.1");

        setupOpGroup(table, "op.group.basic",
        new TutorialOp("inv()", "1 - p"),
        new TutorialOp("slope()", "1 - |p - 0.5| * 2"),
        new TutorialOp("clamp()", "clamp(p, 0, 1)"));

        setupOpGroup(table, "op.group.number",
        new TutorialOp("delay(amount)", "(p - amount) / (1 - amount)"),
        new TutorialOp("shorten(amount)", "p / (1 - amount)"),
        new TutorialOp("compress(start, end)", "(p - start) / (end - start)"),
        new TutorialOp("curve(offset, duration)", "(p - offset) / duration"),
        new TutorialOp("sustain(offset, grow, sustain)", "min(max(0, x/grow), (2*grow + sustain - x)/grow), x = p - offset"),
        new TutorialOp("mod(amount)", "p mod amount"),
        new TutorialOp("loop(time)", "mod(p / time, 1)"),
        new TutorialOp("sin(scl, mag)", "p + sin(time/scl) * mag"),
        new TutorialOp("sin(offset, scl, mag)", "p + sin((time + offset)/scl) * mag"),
        new TutorialOp("absin(scl, mag)", "p + |sin(time/scl)| * mag"));

        setupOpGroup(table, "op.group.progress",
        new TutorialOp("add(other)", "p + other"),
        new TutorialOp("mul(other)", "p * other"),
        new TutorialOp("min(other)", "min(p, other)"),
        new TutorialOp("blend(other, amount)", "lerp(p, other, amount)"));

        setupOpGroup(table, "op.group.interp",
        new TutorialOp("curve(interp)", "interp(p)"));
    }

    private void setupVars(Table table){
        sectionHeader(table, bundle("section.4.title"), false);
        text(table, "section.4.text.1");

        groupHeader(table, bundle("var.group.progress"));
        setupProgressVar(table, EditorList.getPartProgressFields());

        groupHeader(table, bundle("var.group.interp"));
        setupInterpVar(table, EditorList.getInterpFields());
    }

    private void setupProgressVar(Table table, Seq<Field> fields){
        grid(table, 300f, fields.size, (t, i) -> {
            Field field = fields.get(i);
            String desc = bundle("var.progress." + field.getName());
            t.add(field.getName()).style(Styles.outlineLabel).color(EPalettes.value).expandX().left();
            t.row();
            if(desc != null) t.add(desc).color(EPalettes.gray).padTop(8f).wrap().growX();

            copyVar(t, field.getName());
        });
    }

    private void setupInterpVar(Table table, Seq<Field> fields){
        grid(table, 130f, fields.size, (t, i) -> {
            Field field = fields.get(i);
            Interp interp = Reflect.get(field);
            t.add(field.getName()).style(Styles.outlineLabel).color(EPalettes.value).expandX();
            t.row();
            t.add(new ProgressElem(p -> interp.apply(p.x)).hideText()).size(128f).padTop(4f).expandX();

            copyVar(t, field.getName());
        });
    }

    private static void copyVar(Element cell, String text){
        cell.clicked(() -> {
            Core.app.setClipboardText(text);
            EUI.copiedToast(text);
        });
        cell.addListener(new Tooltip(t -> t.background(Styles.black8).margin(8f).add("@patch-editor.editProgress.tutorial.var.copy")));
    }

    private static class TutorialOp{
        final String signature, formula;

        TutorialOp(String signature, String formula){
            this.signature = signature;
            this.formula = formula;
        }
    }
}
