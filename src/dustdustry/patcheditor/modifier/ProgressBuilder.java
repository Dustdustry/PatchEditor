package dustdustry.patcheditor.modifier;

import arc.math.*;
import arc.struct.*;
import mindustry.entities.part.DrawPart.*;

public class ProgressBuilder implements PartProgress{
    public final PartProgress base;
    public final Seq<Op> ops = new Seq<>();

    protected boolean constantBase;

    public ProgressBuilder(PartProgress base){
        this.base = base;
    }

    public ProgressBuilder(float number){
        this.base = PartProgress.constant(number);
        constantBase = true;
    }

    public ProgressBuilder(PartProgress base, Interp interp){
        this.base = base;
        curve(interp);
    }

    public boolean isConstantBase(){
        return constantBase;
    }

    @Override
    public float get(PartParams p){
        return apply().get(p);
    }

    @Override
    public PartProgress inv(){
        ops.add(new Op("inv"));
        return this;
    }

    @Override
    public PartProgress slope(){
        ops.add(new Op("slope"));
        return this;
    }

    @Override
    public PartProgress clamp(){
        ops.add(new Op("clamp"));
        return this;
    }

    @Override
    public PartProgress delay(float amount){
        ops.add(new Op("delay").put("amount", amount));
        return this;
    }

    @Override
    public PartProgress sustain(float offset, float grow, float sustain){
        ops.add(new Op("sustain").put("offset", offset).put("grow", grow).put("sustain", sustain));
        return this;
    }

    @Override
    public PartProgress shorten(float amount){
        ops.add(new Op("shorten").put("amount", amount));
        return this;
    }

    @Override
    public PartProgress compress(float start, float end){
        ops.add(new Op("compress").put("start", start).put("end", end));
        return this;
    }

    @Override
    public PartProgress add(float amount){
        ops.add(new Op("add").put("amount", amount));
        return this;
    }

    @Override
    public PartProgress add(PartProgress other){
        ops.add(new Op("add").put("other", other));
        return this;
    }

    @Override
    public PartProgress blend(PartProgress other, float amount){
        ops.add(new Op("blend").put("other", other).put("amount", amount));
        return this;
    }

    @Override
    public PartProgress mul(float amount){
        ops.add(new Op("mul").put("amount", amount));
        return this;
    }

    @Override
    public PartProgress mul(PartProgress other){
        ops.add(new Op("mul").put("other", other));
        return this;
    }

    @Override
    public PartProgress min(PartProgress other){
        ops.add(new Op("min").put("other", other));
        return this;
    }

    @Override
    public PartProgress sin(float scl, float mag){
        ops.add(new Op("sin").put("scl", scl).put("mag", mag));
        return this;
    }

    @Override
    public PartProgress sin(float offset, float scl, float mag){
        ops.add(new Op("sin").put("offset", offset).put("scl", scl).put("mag", mag));
        return this;
    }

    @Override
    public PartProgress absin(float scl, float mag){
        ops.add(new Op("absin").put("scl", scl).put("mag", mag));
        return this;
    }

    @Override
    public PartProgress mod(float amount){
        ops.add(new Op("mod").put("amount", amount));
        return this;
    }

    @Override
    public PartProgress loop(float time){
        ops.add(new Op("loop").put("time", time));
        return this;
    }

    @Override
    public PartProgress apply(PartProgress other, PartFunc func){
        ops.add(new Op("apply").put("other", other).put("func", func));
        return this;
    }

    @Override
    public PartProgress curve(float offset, float duration){
        ops.add(new Op("curve").put("offset", offset).put("duration", duration));
        return this;
    }

    @Override
    public PartProgress curve(Interp interp){
        ops.add(new Op("curve").put("interp", interp));
        return this;
    }

    public PartProgress apply(){
        PartProgress result = base;
        for(Op op : ops){
            result = applyOp(result, op);
        }
        return result;
    }

    /** Internal way to apply operation. */
    private static PartProgress applyOp(PartProgress prog, Op op){
        return switch(op.name){
            case "inv" -> prog.inv();
            case "slope" -> prog.slope();
            case "clamp" -> prog.clamp();
            case "delay" -> prog.delay(op.getFloat("amount"));
            case "sustain" -> prog.sustain(op.getFloat("offset", 0f), op.getFloat("grow", 0f), op.getFloat("sustain"));
            case "shorten" -> prog.shorten(op.getFloat("amount"));
            case "compress" -> prog.compress(op.getFloat("start"), op.getFloat("end"));
            case "add" -> op.params.containsKey("amount") ? prog.add(op.getFloat("amount")) : prog.add(op.getProgress("other"));
            case "blend" -> prog.blend(op.getProgress("other"), op.getFloat("amount"));
            case "mul" -> op.params.containsKey("amount") ? prog.mul(op.getFloat("amount")) : prog.mul(op.getProgress("other"));
            case "min" -> prog.min(op.getProgress("other"));
            case "sin" -> op.params.containsKey("offset") ? prog.sin(op.getFloat("offset"), op.getFloat("scl"), op.getFloat("mag")) : prog.sin(op.getFloat("scl"), op.getFloat("mag"));
            case "absin" -> prog.absin(op.getFloat("scl"), op.getFloat("mag"));
            case "mod" -> prog.mod(op.getFloat("amount"));
            case "loop" -> prog.loop(op.getFloat("time"));
            case "apply" -> prog.apply(op.getProgress("other"), (PartFunc)op.params.get("func"));
            case "curve" -> op.params.containsKey("interp") ? prog.curve((Interp)op.params.get("interp")) : prog.curve(op.getFloat("offset"), op.getFloat("duration"));
            default -> throw new IllegalArgumentException("Unknown op: " + op.name);
        };
    }

    public static class Op{
        public final String name;
        public final ObjectMap<String, Object> params = new ObjectMap<>();

        public Op(String name){
            this.name = name;
        }

        public Op put(String key, Object value){
            params.put(key, value);
            return this;
        }

        public float getFloat(String key){
            return getFloat(key, 0f);
        }

        public float getFloat(String key, float def){
            Object v = params.get(key);
            if(v instanceof Number n) return n.floatValue();
            return def;
        }

        public PartProgress getProgress(String key){
            Object v = params.get(key);
            if(v instanceof PartProgress p) return p;
            return null;
        }
    }
}
