package dustdustry.patcheditor.ui;

import arc.*;
import arc.graphics.g2d.*;
import arc.struct.*;

public class RegionsTracker{
    private static boolean tracking = false;

    private ObjectSet<String> missing;
    private TextureAtlas coreAtlas;

    private final TextureAtlas atlasTracker = new TextureAtlas(){
        @Override
        public AtlasRegion find(String name){
            AtlasRegion region = coreAtlas.find(name);
            if(region == null || !coreAtlas.isFound(region)){
                missing.add(name);
            }
            return region;
        }
    };

    public RegionsTracker begin(){
        if(tracking){
            throw new RuntimeException("Do not enable RegionsTracker at twice!");
        }

        if(missing == null) missing = new ObjectSet<>();
        coreAtlas = Core.atlas;
        Core.atlas = atlasTracker;
        tracking = true;
        return this;
    }

    public void end(){
        if(!tracking){
            throw new RuntimeException("Do not disable RegionsTracker when already disable!");
        }

        Core.atlas = coreAtlas;
        coreAtlas = null;
        tracking = false;
    }

    public ObjectSet<String> getMissingRegions(){
        return missing;
    }
}
