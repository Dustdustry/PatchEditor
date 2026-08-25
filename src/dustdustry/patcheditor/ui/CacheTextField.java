package dustdustry.patcheditor.ui;

import arc.func.*;
import arc.scene.ui.*;

public class CacheTextField extends TextField {
    protected boolean cacheValid;

    public CacheTextField(String text, Cons<String> onChanged){
        super(text);
        cacheValid = super.isValid();
        changed(() -> {
            cacheValid = super.isValid();
            if(cacheValid) onChanged.get(getText());
        });
    }

    @Override
    public boolean isValid(){
        return cacheValid;
    }
}
