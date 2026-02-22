package MinRi2.PatchEditor.node;

import arc.*;
import arc.audio.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.math.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.type.*;
import mindustry.world.meta.*;

public class EditorList{
    private static final ObjectMap<Class<?>, Seq<String>> subTypeMap = new ObjectMap<>();

    private static Seq<Weapon> weaponList;
    private static Seq<AtlasRegion> regionList;
    private static Seq<Sound> soundList;

    private static Seq<String> visibilityList, interpList;

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

    public static Seq<Sound> getSoundList(){
        if(soundList == null){
            ObjectMap<String, Sound> map = PatchJsonIO.getKeyEntryMap(Sound.class);
            soundList = map.keys().toSeq().sort().map(map::get);
        }
        return soundList;
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
