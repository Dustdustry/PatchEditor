package dustdustry.patcheditor.ui;

import arc.graphics.*;
import arc.scene.style.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.ScrollPane.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

import static mindustry.ui.Styles.*;

/**
 * @author minri2
 * Create by 2024/2/15
 */
public class EStyles{
    public static ImageButtonStyle cardButtoni, cardModifiedButtoni, cardWarni, cardRemovedi;
    public static ImageButtonStyle addButtoni, favoriteButton, noteButton;
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

        favoriteButton = new ImageButtonStyle(Styles.clearNonei){{
           imageUpColor = EPalettes.lighterGray;
           imageOverColor = Pal.lightishGray;
           imageDownColor = EPalettes.gray;
           imageCheckedColor = Color.gold;

           over = up = down = none;
        }};

        noteButton = new ImageButtonStyle(favoriteButton){{
            imageUpColor = EPalettes.gray;
            imageOverColor = Pal.accent;
            imageDownColor = Pal.darkerGray;
            imageCheckedColor = EPalettes.value;
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
