package MinRi2.ContentsEditor.ui;

import arc.graphics.*;
import arc.scene.style.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.ScrollPane.*;
import mindustry.gen.*;
import mindustry.ui.*;

import static mindustry.ui.Styles.cleari;

/**
 * @author minri2
 * Create by 2024/2/15
 */
public class EStyles{
    public static ImageButtonStyle cardButtoni, cardModifiedButtoni;
    public static ImageButtonStyle addButtoni;
    public static ScrollPaneStyle cardGrayPane, cardPane;

    public static void init(){
        cardButtoni = new ImageButtonStyle(cleari){{
            up = colored(EPalettes.gray);
            down = over = colored(EPalettes.main4);
        }};

        cardModifiedButtoni = new ImageButtonStyle(cardButtoni){{
            up = colored(EPalettes.modified);
        }};

        addButtoni = new ImageButtonStyle(cardButtoni){{
            up = colored(EPalettes.add);
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
