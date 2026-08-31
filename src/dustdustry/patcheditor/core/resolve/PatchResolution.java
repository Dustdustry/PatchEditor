package dustdustry.patcheditor.core.resolve;

import dustdustry.patcheditor.core.*;
import mindustry.ctype.*;

public class PatchResolution extends ResolutionStrategy{
    @Override
    public void resolveRoot(ObjectNode node){
        var map = PatchJsonIO.getNameToType();
        for(ContentType ctype : ContentType.all){
            if(map.containsValue(ctype, true)){
                node.addChild(map.findKey(ctype, true), ctype, ContentType.class, ctype.contentClass, null);
            }
        }

        node.addChild("name", "<unnamed>").addSign(ModifierSign.MODIFY);
    }
}
