package dustdustry.patcheditor.node;

import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.math.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import dustdustry.patcheditor.ui.dialog.selector.ColorSelector.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.mod.*;
import mindustry.type.*;
import mindustry.world.meta.*;

import java.lang.reflect.*;
import java.util.*;

public class EditorList{
    private static final ObjectMap<Class<?>, Seq<String>> subTypeMap = new ObjectMap<>();

    // Read only. Do not hold the reference.
    private static Seq<Weapon> weaponList;

    private static Seq<String> visibilityList, interpList, attributeList;
    private static Seq<ColorEntry> colorList;

    public static Seq<Weapon> getWeapons(){
        if(weaponList == null){
            ObjectSet<String> nameSet = new ObjectSet<>();
            weaponList = Vars.content.units().flatMap(unit -> unit.weapons).retainAll(w -> nameSet.add(w.name));
        }
        return weaponList;
    }

    public static Seq<String> getSubTypeNames(Class<?> clazz){
        Seq<String> typeNames = subTypeMap.get(clazz, Seq::new);
        if(typeNames.isEmpty()){
            for(Entry<String, Class<?>> entry : ClassMap.classes){
                if(clazz.isAssignableFrom(entry.value)){
                    typeNames.add(entry.key);
                }
            }
        }
        return typeNames;
    }

    public static UnitConstructorType getUnitTypeName(Class<?> type){
        for(UnitConstructorType consType : UnitConstructorType.values()){
            if(consType.type.isAssignableFrom(type)) return consType;
        }
        return UnitConstructorType.flying;
    }

    public static Seq<String> getVisibilityList(){
        if(visibilityList == null){
            visibilityList = PatchJsonIO.getKeyEntryMap(BuildVisibility.class).keys().toSeq();
        }
        return visibilityList;
    }

    public static Seq<String> getInterpList(){
        if(interpList == null){
            interpList = PatchJsonIO.getKeyEntryMap(Interp.class).keys().toSeq();
        }
        return interpList;
    }

    public static Seq<String> getAttributeList(){
        if(attributeList == null){
            attributeList = Attribute.map.keys().toSeq();
        }

        return attributeList;
    }

    public static Seq<ColorEntry> getColorList(){
        if(colorList == null){
            ObjectMap<String, Color> map = new ObjectMap<>();

            for(var entry : Colors.getColors()){
                map.put(entry.key.toLowerCase(), entry.value);
            }

            for(Field field : Seq.with(Pal.class.getFields())){
                map.put(field.getName().toLowerCase(), Reflect.get(field));
            }

            colorList = new Seq<>();
            for(var entry : map){
                colorList.add(new ColorEntry(entry.key, entry.value));
            }

            colorList.sortComparing(e -> e.name);
        }
        return colorList;
    }

    public enum UnitConstructorType{
        flying(UnitEntity.class),
        mech(MechUnit.class),
        legs(LegsUnit.class),
        naval(UnitWaterMove.class),
        payload(PayloadUnit.class),
        missile(TimedKillUnit.class),
        tank(TankUnit.class),
        hover(ElevationMoveUnit.class),
        tether(BuildingTetherPayloadUnit.class),
        crawl(CrawlUnit.class);

        public final Class<?> type;

        UnitConstructorType(Class<?> type){
            this.type = type;
        }
    }

    public static class ColorEntry{
        public String name;
        public Color color;

        public ColorEntry(String name, Color color){
            this.name = name;
            this.color = color;
        }

        @Override
        public boolean equals(Object object){
            if(!(object instanceof ColorEntry that)) return false;
            return Objects.equals(name, that.name) && Objects.equals(color, that.color);
        }

        @Override
        public int hashCode(){
            return Objects.hash(name, color);
        }

        @Override
        public String toString(){
            return "ColorEntry{" +
            "name='" + name + '\'' +
            ", color=" + color +
            '}';
        }
    }
}
