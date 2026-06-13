package dustdustry.patcheditor.utils;

import dustdustry.patcheditor.node.patch.*;

public class NodePath{
    public static boolean isRelatedPath(String a, String b){
        if(a == null || b == null) return false;
        return isSameOrChildPath(a, b) || isSameOrChildPath(b, a);
    }

    public static boolean isSameOrChildPath(String path, String ancestor){
        if(ancestor.isEmpty()) return true;
        return path.equals(ancestor) || path.startsWith(ancestor + NodeManager.pathComp);
    }
}
