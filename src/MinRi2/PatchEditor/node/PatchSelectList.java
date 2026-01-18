package MinRi2.PatchEditor.node;

import arc.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.type.*;

public class PatchSelectList{
    private static final ObjectMap<Class<?>, Seq<String>> subTypeMap = new ObjectMap<>();

    private static final Seq<Weapon> weaponList = new Seq<>();
    private static final Seq<AtlasRegion> regionList = new Seq<>();

    public static Seq<Weapon> getWeapons(){
        if(weaponList.isEmpty()){
            ObjectSet<String> nameSet = new ObjectSet<>();
            Seq<Weapon> seq = Vars.content.units().flatMap(unit -> unit.weapons).retainAll(w -> nameSet.add(w.name));
            weaponList.set(seq);
        }
        return weaponList;
    }

    public static Seq<AtlasRegion> getRegions(){
        if(regionList.isEmpty()){
            // yes, sort by name
            regionList.set(Core.atlas.getRegions());
            regionList.sort(Structs.comparing(region -> region.name));
        }
        return regionList;
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
