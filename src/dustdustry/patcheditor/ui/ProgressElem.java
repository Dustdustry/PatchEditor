package dustdustry.patcheditor.ui;

import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.entities.part.DrawPart.*;
import mindustry.graphics.*;
import mindustry.ui.*;

public class ProgressElem extends Element{
    private PartProgress progress;
    private final PartParams params = new PartParams();
    public float step = 1f / 50f;

    public Color boundColor = Pal.darkerGray;
    public Color meshColor = Pal.darkerGray;
    public float boundStroke = 3f, boundPadding = 12f;
    public float meshStroke = 1f;

    public Color fontColor = Pal.gray;
    public float fontScale = 0.8f, fontBoundSize = 40f;
    public float loopTime = 180f, fadeTime = 75f;

    public float stroke = 2.5f;
    public Color lineColor = Color.white;
    public Color shadowColor = Pal.darkerGray;

    public ProgressElem(PartProgress progress){
        this.progress = progress;
    }

    public void setProgress(PartProgress progress){
        this.progress = progress;
    }

    public ProgressElem hideText(){
        fontScale = 0f;
        fontBoundSize = 0f;
        return this;
    }

    // inline?
    protected float getValue(float value){
        return progress.get(params.set(value, value, value, value, value, value, value, value, value));
    }

    @Override
    public void draw(){
        super.draw();

        float time = Time.globalTime % (loopTime + fadeTime + 10f);
        float fadeFrac = Interp.pow4In.apply(Mathf.clamp((time - loopTime) / fadeTime));

        // mesh
        float meshWidth = width - fontBoundSize - boundPadding;
        float meshHeight = height - fontBoundSize - boundPadding;
        float meshX = x + fontBoundSize;
        float meshY = y + fontBoundSize;
        if(!Mathf.zero(meshStroke)){
            Lines.stroke(meshStroke, meshColor);
            for(int i = 1; i <= 3; i++){
                float px = i / 4f;
                float lx = meshX + px * meshWidth;
                Lines.line(lx, meshY, lx, meshY + meshHeight);
            }
            for(int i = 1; i <= 3; i++){
                float py = i / 4f;
                float ly = meshY + py * meshHeight;
                Lines.line(meshX, ly, meshX + meshWidth, ly);
            }
        }

        // bound
        if(!Mathf.zero(boundStroke)){
            Lines.stroke(boundStroke, boundColor);
            float halfBgStroke = boundStroke / 2f;
            Lines.rect(meshX + halfBgStroke, meshY + halfBgStroke, meshWidth - halfBgStroke, meshHeight - halfBgStroke);
        }

        // end line
        float end = Mathf.clamp(time / loopTime);
        Lines.stroke(1f, Tmp.c1.set(Color.gray).a(1f - fadeFrac));
        float endX = meshX + meshWidth * end;
        float endY = meshY + meshHeight * getValue(end);
        Lines.dashLine(endX, endY, endX, meshY, Mathf.ceil(meshHeight / 32f));
        Lines.dashLine(endX, endY, meshX, endY, Mathf.ceil(meshWidth / 32f));

        // line and shadow line
        if(!Mathf.zero(stroke)){
            Lines.beginLine();
            float lastX = meshX, lastY = meshY + meshHeight * getValue(0f);
            for(float px = step; px <= 1f; px += step){
                float py = getValue(px);
                float lx = meshX + meshWidth * px;
                float ly = meshY + meshHeight * py;

                if(px <= end){
                    Lines.stroke(stroke, Tmp.c1.set(lineColor).a(1f - fadeFrac));
                }else{
                    Lines.stroke(stroke, shadowColor);
                }

                Lines.line(lastX, lastY, lx, ly);

                lastX = lx;
                lastY = ly;
            }
            Lines.endLine();
        }

        if(!Mathf.zero(fontScale)){
            float fontPadding = 8;
            drawText("1.0", meshX - fontPadding, meshY + meshHeight, fontScale, Align.left, false, fontColor);
            drawText("0.5", meshX - fontPadding, meshY + meshHeight / 2f, fontScale, Align.left, false, fontColor);
            drawText("0.0", meshX - fontPadding / 2f, meshY - fontPadding / 2f, fontScale, Align.topLeft, false, fontColor);

            drawText("0.5", meshX + meshWidth / 2f, meshY - fontPadding, fontScale, Align.top, false, fontColor);
            drawText("1.0", meshX + meshWidth, meshY - fontPadding, fontScale, Align.top, false, fontColor);
        }

        Draw.reset();
    }

    private static void drawText(String text, float x, float y, float scale, int align, boolean outline, Color color){
        Font font = outline ? Fonts.outline : Fonts.def;

        boolean ints = font.usesIntegerPositions();
        float lastScaleX = font.getData().scaleX, lastScaleY = font.getData().scaleY;
        font.setUseIntegerPositions(false);
        font.getData().setScale(scale);

        GlyphLayout layout = Pools.get(GlyphLayout.class, GlyphLayout::new).obtain();
        layout.setText(font, text);

        x -= layout.width / 2f;
        y += layout.height / 2f;

        if(Align.isBottom(align)){
            y += layout.height / 2f;
        }else if(Align.isTop(align)){
            y -= layout.height / 2f;
        }

        if(Align.isLeft(align)){
            x -= layout.width / 2f;
        }else if(Align.isRight(align)){
            x += layout.width / 2f;
        }

        font.setColor(color);
        font.draw(text, x, y, layout.width, Align.center, false);

        Draw.reset();
        Pools.free(layout);

        font.setColor(Color.white);
        font.getData().setScale(lastScaleX, lastScaleY);
        font.setUseIntegerPositions(ints);
    }
}
