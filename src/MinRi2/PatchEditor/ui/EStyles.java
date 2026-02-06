package MinRi2.PatchEditor.ui;

import arc.graphics.*;
import arc.scene.style.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.ScrollPane.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

import static mindustry.ui.Styles.cleari;

/**
 * @author minri2
 * Create by 2024/2/15
 */
public class EStyles{
    public static ImageButtonStyle cardButtoni, cardModifiedButtoni, cardWarni, cardRemovedi;
    public static ImageButtonStyle addButtoni;
    public static ScrollPaneStyle cardGrayPane, cardPane;

    public static void init(){
        cardButtoni = new ImageButtonStyle(cleari){{
            up = colored(EPalettes.gray);
            down = over = colored(EPalettes.main4);
            disabled = colored(Pal.darkerGray);
        }};

        cardModifiedButtoni = new ImageButtonStyle(cardButtoni){{
            up = colored(EPalettes.modified);
        }};

        cardWarni = new ImageButtonStyle(cardButtoni){{
            up = colored(EPalettes.warn);
        }};

        addButtoni = new ImageButtonStyle(cardButtoni){{
            up = colored(EPalettes.add);
        }};

        cardRemovedi = new ImageButtonStyle(cardButtoni){{
            up = disabled = colored(EPalettes.remove);
        }};

        cardGrayPane = new ScrollPaneStyle(){{
            background = Styles.grayPanel;
        }};

        cardPane = new ScrollPaneStyle(){{
            background = Tex.pane2;
        }};
    }

    private static TextureRegionDrawable colored(Color color){
        TextureRegionDrawable whiteui = (TextureRegionDrawable)Tex.whiteui;
        return ((TextureRegionDrawable)whiteui.tint(color));
    }
}
