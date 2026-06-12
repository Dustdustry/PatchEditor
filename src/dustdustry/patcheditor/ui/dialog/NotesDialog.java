package dustdustry.patcheditor.ui.dialog;

import dustdustry.patcheditor.*;
import dustdustry.patcheditor.FieldNotes.*;
import dustdustry.patcheditor.ui.*;
import arc.*;
import arc.graphics.*;
import arc.net.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.struct.ObjectMap.*;
import arc.util.*;
import java.text.*;
import java.util.*;

import mindustry.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

public class NotesDialog extends BaseDialog{
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private String searchText = "";
    private final ObjectSet<String> shownTypes = new ObjectSet<>();

    private ScrollPane pane;
    private final Table notesTable = new Table();

    public NotesDialog(){
        super("@patch-editor.notes");

        resized(this::rebuild);
        shown(this::rebuild);
        hidden(() -> pane = null);

        addCloseButton();
    }

    private void rebuild(){
        cont.top();
        cont.clearChildren();

        float width = Math.min(Core.graphics.getWidth() * 0.7f, 1000f);
        cont.table(table -> {
            table.top();
            table.defaults().pad(8f).growX();

            table.table(Styles.grayPanel, this::setupSearchTable).row();

            if(pane == null) pane = new ScrollPane(notesTable, Styles.noBarPane);
            table.add(pane).scrollX(false).grow().row();
        }).width(width).growY();

        rebuildNotesTable();
    }

    private void setupSearchTable(Table table){
        table.image(Icon.zoomSmall).size(Vars.iconMed);

        TextField field = table.add(EUI.deboundTextField(searchText, text -> {
            searchText = text;
            rebuildNotesTable();
        })).padLeft(4f).padRight(4f).growX().get();

        if(Core.app.isDesktop()){
            Core.scene.setKeyboardFocus(field);
        }

        table.button(Icon.cancelSmall, Styles.clearNonei, () -> {
            searchText = "";
            field.setText(searchText);
            rebuildNotesTable();
        }).disabled(b -> searchText.isEmpty()).width(Vars.iconSmall).growY();
    }

    private void rebuildNotesTable(){
        final Table table = notesTable;
        table.clear();
        table.top().defaults().pad(4f);

        setupManagementPanel(table);

        Seq<String> notes = FieldNotes.allId().select(fieldId ->
        Strings.matches(searchText, fieldId)
        || (FieldNotes.getNote(fieldId) != null && Strings.matches(searchText, FieldNotes.getNote(fieldId))));

        if(notes.isEmpty()){
            table.add("@notes.empty").pad(16f).color(Color.lightGray);
            table.row();
            return;
        }

        OrderedMap<String, Seq<String>> mapped = new OrderedMap<>();
        for(String fieldId : notes){
            String ownerType = fieldId.split("#")[0];
            mapped.get(ownerType, Seq::new).add(fieldId);
        }

        boolean first = true;
        int shownCount = 0;
        shownTypes.clear();
        for(Entry<String, Seq<String>> entry : mapped){
            String type = entry.key;
            Seq<String> fields = entry.value;

            if(shownCount < 100 && fields.size < 30){
                shownTypes.add(type);
                shownCount += fields.size;
            }

            table.table(top -> {
                top.image().color(Pal.darkerGray).width(32f).padRight(8f);
                top.add(type).labelAlign(Align.left).color(EPalettes.type).minWidth(64f);
                top.add("(" + fields.size + ")").color(EPalettes.grayFront).padLeft(4f);
                top.image().color(Pal.darkerGray).padLeft(8f).padRight(16f).growX();
                top.button(Icon.eyeSmall, Styles.clearTogglei, () -> {
                    if(!shownTypes.add(type)){
                        shownTypes.remove(type);
                    }
                }).size(Vars.iconMed).checked(shownTypes.contains(type));
            }).padTop(first ? 0f : 16f).growX();

            table.row();

            table.collapser(cont -> {
                for(String fieldId : fields){
                    cont.table(t -> setupNoteFieldTable(t, fieldId)).padBottom(4f).growX();
                    cont.row();
                }
            }, () -> shownTypes.contains(type)).growX();

            table.row();

            first = false;
        }
    }

    private void setupManagementPanel(Table table){
        table.table(Styles.grayPanel, panel -> {
            panel.defaults().left();

            panel.table(title -> {
                title.add("@notes.source.wiki").color(EPalettes.type);
                title.image(Icon.infoSmall).size(Vars.iconSmall).color(Pal.lightishGray).padLeft(4f);

                if(FieldNotes.getWikiMetaVersion() != null){
                    title.defaults().padLeft(12f);
                    title.add(Core.bundle.format("notes.wiki.github.meta.version", FieldNotes.getWikiMetaVersion())).color(EPalettes.grayFront);
                    if(FieldNotes.getWikiMetaUpdateTime() > 0){
                        title.add(dateFormat.format(FieldNotes.getWikiMetaUpdateTime())).color(EPalettes.grayFront);
                    }
                }
            }).tooltip("@notes.source.wiki.tooltip", true).row();

            panel.table(buttons -> {
                buttons.defaults().pad(4f).height(48f).growX();
                buttons.button("@notes.wiki.github", Icon.book, Styles.cleart, this::showGithubDialog);
                buttons.button("@notes.wiki.file", Icon.download, Styles.cleart, () -> {
                    Vars.platform.showMultiFileChooser(fi -> {
                        Vars.ui.showConfirm("@confirm", "@notes.import.replace.confirm", () -> {
                            FieldNotes.replaceWikiNotes(fi.readString());
                            FieldNotes.saveWikiNotes();
                            rebuildNotesTable();
                            EUI.infoToast(Core.bundle.format("notes.wiki.file.succeed", fi.name()));
                        });
                    }, "json");
                });

                buttons.row();
                buttons.button("@notes.wiki.export", Icon.copy, Styles.cleart, () -> {
                    Core.app.setClipboardText(FieldNotes.exportWikiNotesJson());
                    EUI.infoToast("@notes.export.succeed");
                });
                buttons.button("@notes.wiki.clear", Icon.cancel, Styles.cleart, () -> {
                    Vars.ui.showConfirm("@confirm", "@notes.wiki.clear.confirm", () -> {
                        FieldNotes.clearWikiNotes();
                        rebuildNotesTable();
                        EUI.infoToast("@notes.wiki.clear.succeed");
                    });
                }).disabled(b -> FieldNotes.wikiNoteCount() == 0);
            }).growX().row();

            panel.image().color(Pal.darkerGray).height(4f).growX().padTop(4f).padBottom(4f).row();

            panel.table(title -> {
                title.add("@notes.source.custom").color(EPalettes.type);
            }).row();

            panel.table(buttons -> {
                buttons.defaults().pad(4f).height(48f).growX();
                buttons.button("@notes.user.file", Icon.download, Styles.cleart, () -> {
                    Vars.platform.showMultiFileChooser(fi -> {
                        FieldNotes.importUserNotesClipboard(fi.readString(), false);
                        rebuildNotesTable();
                        EUI.infoToast(Core.bundle.format("notes.user.file.succeed", fi.name()));
                    }, "json");
                });
                buttons.button("@notes.import", Icon.download, Styles.cleart, this::importNotesFromClipboard);

                buttons.row();
                buttons.button("@notes.export", Icon.copy, Styles.cleart, () -> {
                    Core.app.setClipboardText(FieldNotes.exportUserNotesJson());
                    EUI.infoToast("@notes.export.succeed");
                });

                buttons.button("@notes.clear", Icon.cancel, Styles.cleart, () -> {
                    Vars.ui.showConfirm("@confirm", "@notes.clear.confirm", () -> {
                        if(FieldNotes.clearUserNotes()){
                            rebuildNotesTable();
                            EUI.infoToast("@notes.clear.succeed");
                        }
                    });
                }).disabled(b -> FieldNotes.userNoteCount() == 0);
            }).growX().row();
        }).growX().padBottom(12f);
        table.row();
    }

    private void setupNoteFieldTable(Table table, String fieldId){
        table.background(Tex.whiteui).setColor(EPalettes.gray);

        table.table(info -> {
            info.left().defaults().left();

            int split = fieldId.indexOf("#");
            String fieldName = fieldId.substring(split + 1);
            info.add(fieldName).style(Styles.outlineLabel).pad(8f);
            String note = FieldNotes.getNote(fieldId);
            if(note != null){
                info.row();
                info.add(Core.bundle.format("notes.value", note))
                .padLeft(8f).padRight(8f).padBottom(8f).wrap().growX().colspan(2);
            }
        }).growX();

        boolean isUser = FieldNotes.getUserNote(fieldId) != null;
        String source = Core.bundle.get(isUser ? "notes.source.custom" : "notes.source.wiki");
        table.add(source).padLeft(8f).color(EPalettes.grayFront);

        table.table(buttons -> {
            buttons.defaults().width(Vars.iconMed).growY().pad(4f);
            buttons.button(Icon.editSmall, Styles.clearNonei, () -> {
                EUI.noteEditor.show(fieldId, () -> {
                    table.clear();
                    setupNoteFieldTable(table, fieldId);
                });
            }).tooltip("@patch-editor.note.edit");
            buttons.button(Icon.copySmall, Styles.clearNonei, () -> {
                Core.app.setClipboardText(fieldId);
                EUI.infoToast("@favorites.copy-id.succeed");
            }).tooltip("@favorites.copy-id");
            if(isUser){
                buttons.button(Icon.cancelSmall, Styles.clearNonei, () -> {
                    FieldNotes.removeUserNote(fieldId);
                    table.clear();
                    setupNoteFieldTable(table, fieldId);
                }).tooltip("@notes.clear");
            }
        }).growY().pad(4f);

        table.image().width(4f).color(Color.darkGray).growY().right();
        table.row();
        Cell<?> horizontalLine = table.image().height(4f).color(Color.darkGray).growX();
        horizontalLine.colspan(table.getColumns());
    }

    private void importNotesFromClipboard(){
        String text = Core.app.getClipboardText();
        if(text == null || text.isEmpty()){
            EUI.infoToast("@notes.import.failed");
            return;
        }

        try{
            FieldNotes.importUserNotesClipboard(text, false);
        }catch(Exception e){
            Vars.ui.showException("@notes.import.failed", e);
            return;
        }

        rebuildNotesTable();
        EUI.infoToast("@notes.import.succeed");
    }

    private void showGithubDialog(){
        BaseDialog dialog = new BaseDialog("@notes.wiki.github.title");
        dialog.addCloseButton();

        TextField[] repoField = {null};
        TextField[] branchField = {null};

        dialog.cont.table(t -> {
            t.defaults().pad(6f);
            t.add(Core.bundle.format("notes.wiki.github.hint", EVars.githubNotesRepo)).left().row();

            repoField[0] = new TextField(EVars.githubNotesRepo);
            repoField[0].setMessageText("owner/repo");
            t.add(repoField[0]).growX();
            t.row();

            t.add("@notes.wiki.github.branch").left().row();
            branchField[0] = new TextField("main");
            t.add(branchField[0]).growX();
            t.row();
        });

        dialog.buttons.button("@confirm", () -> {
            String repo = repoField[0] != null ? repoField[0].getText().trim() : EVars.githubNotesRepo;
            String branch = branchField[0] != null ? branchField[0].getText().trim() : EVars.githubNotesBranch;

            if(repo.isEmpty()){
                EUI.infoToast("@notes.wiki.github.failed");
                return;
            }
            if(branch.isEmpty()) branch = "main";

            dialog.hide();
            fetchGithubIndex(repo, branch);
        });

        dialog.show();
    }

    private void fetchGithubIndex(String ownerRepo, String branch){
        String indexUrl = "https://raw.githubusercontent.com/" + ownerRepo + "/" + branch + "/notes/index.json";

        Vars.ui.loadfrag.show("@notes.wiki.github.fetching");

        Http.get(indexUrl)
        .timeout(15000)
        .error(t -> Core.app.post(() -> {
            Vars.ui.loadfrag.hide();
            Log.err("Failed to fetch notes index", t);
            EUI.infoToast("@notes.wiki.github.failed");
        }))
        .submit(response -> {
            if(response.getStatus().code < 200 || response.getStatus().code >= 300){
                Core.app.post(() -> {
                    Vars.ui.loadfrag.hide();
                    EUI.infoToast("@notes.wiki.github.failed");
                });
                return;
            }

            String text = response.getResultAsString();
            Core.app.post(() -> {
                Vars.ui.loadfrag.hide();
                FieldNotes.IndexData index = FieldNotes.parseNotesIndex(text);

                if(index == null || index.notes.isEmpty()){
                    EUI.infoToast("@notes.wiki.github.empty");
                    return;
                }

                showFileSelectionDialog(ownerRepo, branch, index);
            });
        });
    }

    private void showFileSelectionDialog(String ownerRepo, String branch, FieldNotes.IndexData index){
        Seq<FieldNotes.NoteDataIndexed> files = index.notes;

        OrderedMap<String, Seq<FieldNotes.NoteDataIndexed>> groupedByLang = new OrderedMap<>();
        for(FieldNotes.NoteDataIndexed file : files){
            groupedByLang.get(file.lang, Seq::new).add(file);
        }

        BaseDialog dialog = new BaseDialog("@notes.wiki.github.select");
        dialog.addCloseButton();

        Table fileTable = new Table();
        fileTable.top().defaults().pad(4f).growX();

        boolean first = true;
        for(var entry : groupedByLang){
            String lang = entry.key;
            Seq<FieldNotes.NoteDataIndexed> langFiles = entry.value;

            fileTable.table(top -> {
                top.image().color(Pal.darkerGray).width(32f).padRight(8f);
                top.add(lang).labelAlign(Align.left).color(EPalettes.type).minWidth(64f);
                top.add("(" + langFiles.size + ")").color(EPalettes.grayFront).padLeft(4f);
                top.image().color(Pal.darkerGray).padLeft(8f).padRight(16f).growX();
            }).padTop(first ? 0f : 16f);

            fileTable.row();

            for(NoteDataIndexed file : langFiles){
                fileTable.button(row -> {
                    row.table(info -> {
                        info.left().defaults().left();
                        info.add(file.fileName).style(Styles.outlineLabel).row();
                        info.add(Core.bundle.format("notes.wiki.github.meta.version", file.versionTag))
                            .color(EPalettes.grayFront).padTop(4f).row();
                        if(file.updateTime > 0){
                            info.add(Core.bundle.format("notes.wiki.github.meta.updateTime", dateFormat.format(file.updateTime)))
                                .color(EPalettes.grayFront).row();
                        }
                        info.add(Core.bundle.format("notes.wiki.github.meta.count", file.noteCount))
                            .color(EPalettes.grayFront);
                    }).pad(8f).growX();

                    row.image().width(4f).color(Color.darkGray).growY().right();
                    row.row();
                    Cell<?> horizontalLine = row.image().height(4f).color(Color.darkGray).growX();
                    horizontalLine.colspan(row.getColumns());
                }, EStyles.cardButtoni, () -> {
                    showDownloadConfirm(file);
                });
            }
            fileTable.row();
            first = false;
        }

        float width = Math.min(Core.graphics.getWidth() * 0.7f, 1000f);
        dialog.cont.table(t -> {
            t.pane(Styles.noBarPane, fileTable).grow().scrollX(false);
        }).width(width).growY();

        dialog.show();
    }

    private void showDownloadConfirm(FieldNotes.NoteDataIndexed file){
        Vars.ui.showConfirm("@confirm", Core.bundle.format("notes.wiki.github.confirm", file.fileName,
            file.versionTag, file.noteCount), () -> {
            downloadSelectedFile(file);
        });
    }

    private void downloadSelectedFile(FieldNotes.NoteDataIndexed file){
        Vars.ui.loadfrag.show("@notes.wiki.github.fetching");

        Http.get(file.fileUrl)
        .timeout(15000)
        .error(t -> Core.app.post(() -> {
            Vars.ui.loadfrag.hide();
            Log.err("Failed to download " + file.fileName, t);
            EUI.infoToast("@notes.wiki.github.failed");
        }))
        .submit(response -> {
            int code = response.getStatus().code;
            String notes = response.getResultAsString();
            Core.app.post(() -> {
                Vars.ui.loadfrag.hide();
                if(code >= 200 && code < 300){
                    FieldNotes.mergeWikiNotes(notes);
                    FieldNotes.setWikiMeta(file);
                    FieldNotes.saveWikiNotes();
                    rebuildNotesTable();
                    EUI.infoToast(Core.bundle.format("notes.wiki.github.succeed", file.fileName));
                }else{
                    EUI.infoToast("@notes.wiki.github.failed");
                }
            });
        });
    }
}
