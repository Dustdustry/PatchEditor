package MinRi2.PatchEditor.node;

import arc.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.logic.LogicFx.*;
import mindustry.mod.*;
import mindustry.type.*;
import mindustry.ui.dialogs.*;

public class PatchSelectList{
    private static final ObjectMap<Class<?>, Seq<String>> subTypeMap = new ObjectMap<>();

    private static Seq<Weapon> weaponList;
    private static Seq<AtlasRegion> regionList;
    private static Seq<EffectEntry> effectList;

    public static Seq<Weapon> getWeapons(){
        if(weaponList == null){
            ObjectSet<String> nameSet = new ObjectSet<>();
            weaponList = Vars.content.units().flatMap(unit -> unit.weapons).retainAll(w -> nameSet.add(w.name));
        }
        return weaponList;
    }

    public static Seq<AtlasRegion> getRegions(){
        if(regionList == null){
            // yes, sort by name
            regionList = Seq.with(Core.atlas.getRegions());
            regionList.sort(Structs.comparing(region -> region.name));
        }
        return regionList;
    }

    public static Seq<EffectEntry> getEffectList(){
        if(effectList == null){
            // copy from EffectDialog
            effectList = Seq.select(Fx.class.getFields(), f -> f.getType() == Effect.class).map(f -> new EffectEntry(Reflect.get(f)).name(f.getName()));
        }
        return effectList;
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
}
