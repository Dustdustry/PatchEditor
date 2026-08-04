package dustdustry.patcheditor.ui;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.entities.effect.*;
import mindustry.gen.*;
import mindustry.logic.LogicFx.*;
import mindustry.type.*;
import mindustry.ui.dialogs.*;

import java.lang.reflect.*;

public class EffectElems{
    private static Method getDataMethod, calculateSizeMethod;

    // safely
    public static Object getData(Class<?> type){
        if(getDataMethod == null){
            try{
                getDataMethod = EffectsDialog.class.getDeclaredMethod("getData", Class.class);
                getDataMethod.setAccessible(true);
            }catch(NoSuchMethodException ignored){
                return null;
            }
        }
        try{
            return getDataMethod.invoke(null, type);
        }catch(IllegalAccessException | InvocationTargetException e){
            return null;
        }
    }

    // safely
    public static float calculateSize(EffectEntry entry){
        if(calculateSizeMethod == null){
            try{
                calculateSizeMethod = EffectsDialog.class.getDeclaredMethod("calculateSize", EffectEntry.class);
                calculateSizeMethod.setAccessible(true);
            }catch(NoSuchMethodException e){
                return 0f;
            }
        }

        try{
            return (float)calculateSizeMethod.invoke(null, entry);
        }catch(IllegalAccessException | InvocationTargetException e){
            return 0f;
        }
    }

    public static Element getEffectElem(EffectEntry effect, ClickListener listener){
        return new EffectCell(effect, listener);
    }

    /** copy from {@link mindustry.ui.dialogs.EffectsDialog} */
    public static class EffectCell extends Element{
        EffectEntry effect;
        Effect renderEffect;
        float size = -1f;

        int id = 1;
        float time = 0f;
        float lifetime;
        float rotation = 1f;
        Object data;
        ClickListener cl;

        public EffectCell(EffectEntry effect, ClickListener cl){
            this.effect = effect;
            this.lifetime = effect.effect.lifetime;
            this.cl = cl;

            renderEffect = effect.effect;
            if(renderEffect instanceof WrapEffect wrapEffect){
                renderEffect = wrapEffect.effect == null ? wrapEffect : wrapEffect.effect;
            }else if(renderEffect instanceof SoundEffect soundEffect){
                renderEffect = soundEffect.effect == null ? soundEffect : soundEffect.effect;
            }

            data = getData(effect.data);
        }

        @Override
        public void draw(){
            if(size < 0){
                size = calculateSize(new EffectEntry(renderEffect)) + 1f;
            }

            if(clipBegin(x, y, width, height)){
                Draw.colorl(cl.isOver() ? 0.4f : 0.5f);
                Draw.alpha(parentAlpha);
                Tex.alphaBg.draw(x, y, width, height);
                Draw.reset();
                Draw.flush();

                float scale = width / size;
                Tmp.m1.set(Draw.trans());
                Draw.trans().translate(x + width/2f, y + height/2f).scale(scale, scale);
                Draw.flush();

                if(effect.effect instanceof WrapEffect wrapEffect){
                    this.lifetime = renderEffect.render(id, wrapEffect.color, time, lifetime, wrapEffect.rotation, 0f, 0f, data);
                }else{
                    color.fromHsv((Time.globalTime * 2f) % 360f, 1f, 1f);
                    this.lifetime = renderEffect.render(id, color, time, lifetime, rotation, 0f, 0f, data);
                }

                Draw.flush();
                Draw.trans().set(Tmp.m1);
                clipEnd();
            }

            Lines.stroke(Scl.scl(3f), Color.black);
            Lines.rect(x, y, width, height);
            Draw.reset();
        }

        @Override
        public void act(float delta){
            super.act(delta);

            time += Time.delta;
            if(time >= lifetime){
                id ++;
            }
            time %= lifetime;
        }
    }
}
