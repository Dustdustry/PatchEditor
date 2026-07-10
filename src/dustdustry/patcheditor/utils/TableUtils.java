package dustdustry.patcheditor.utils;

import arc.func.*;
import arc.scene.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;

public class TableUtils{
    public static void insertColumnAfter(Table table, int column, Cons<Table> tableCons){
        insertColumnAfter(table, column, new Table(tableCons));
    }

    public static void insertColumnAfter(Table table, int column, Table columnTable){
        Seq<Cell> columnCells = columnTable.getCells();
        if(columnCells.isEmpty()) return;

        Seq<Cell> cells = table.getCells();
        Seq<Cell<?>> newCells = new Seq<>();

        column = Math.min(column, table.getColumns() - 1);

        boolean appendEnd = column == table.getColumns() - 1;

        int newColumns = table.getColumns();
        for(Cell<?> cell : cells){
            newCells.add(cell);

            int cellCol = Reflect.get(cell, "column");
            if(cellCol < column) continue;
            int cellRow = Reflect.get(cell, "row");
            if(cellRow > columnCells.size) break;

            if(cellCol == column){
                Cell<?> columnCell = columnCells.get(cellRow);
                Cell<?> insertCell = Pools.get(Cell.class, Cell::new).obtain();
                insertCell.set(columnCell);

                Element element = columnCell.get();
                table.addChild(element);
                Reflect.set(insertCell, "element", element);
                Reflect.set(insertCell, "column", cellCol + 1);

                newCells.add(insertCell);

                if(appendEnd){
                    Reflect.set(cell, "endRow", false);
                    Reflect.set(insertCell, "endRow", true);
                }
            }

            if(cellCol > column){
                int cellAboveIndex = Reflect.get(cell, "cellAboveIndex");
                Reflect.set(cell, "column", cellCol);
                Reflect.set(cell, "cellAboveIndex", cellAboveIndex);
            }
        }

        cells.set(newCells);
        Reflect.set(table, "columns", newColumns);
        table.invalidate();
    }
}
