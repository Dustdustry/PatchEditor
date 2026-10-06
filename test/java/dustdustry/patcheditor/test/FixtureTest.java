package dustdustry.patcheditor.test;

import static org.junit.jupiter.api.Assertions.*;

import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import dustdustry.patcheditor.core.*;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;

/** Golden file test: {@code test/test.hjson} parsed against the fabricated content and read back as json. */
@DisplayName("test.hjson fixture")
class FixtureTest extends HeadlessTest{
    private static final String FIXTURE = "test/test.hjson";

    private static PatchNode patch;

    @BeforeAll
    static void parseFixture(){
        patch = HeadlessEnv.parse(HeadlessEnv.patchRoot(), fixtureText());
    }

    @Test
    void root(){
        assertEquals("TestPatch", patch.getOrNull("name").value, "the patch name is kept");
    }

    @Test
    void abilityArray(){
        PatchNode abilities = path("unit.toxopid.abilities");
        assertSame(Jtype.array, abilities.type);
        assertSame(ModifierSign.MODIFY, abilities.sign, "a bare array overrides the list");

        PatchNode ability = abilities.getOrNull("0");
        assertSame(ModifierSign.PLUS, ability.sign, "array elements are appended");
        assertEquals("ShieldArcAbility", ability.getOrNull("type").value, "the element type is resolved");
        assertEquals("ff0000ff", ability.getOrNull("color").value);
        assertEquals("false", ability.getOrNull("whenShooting").value, "a quoted boolean stays text");
        assertEquals("45", ability.getOrNull("radius").value, "a quoted number stays text");
    }

    @Test
    void appendedWeapons(){
        PatchNode appended = path("unit.toxopid.weapons").getOrNull(PatchJsonIO.appendPrefix + "0");
        assertNotNull(appended, "'weapons.+:' becomes an append node");
        assertSame(ModifierSign.PLUS, appended.sign);
    }

    @Test
    void sugaredStacks(){
        PatchNode requirement = path("unit.avert.cachedRequirements.0");
        assertSame(ModifierSign.PLUS, requirement.sign);
        assertNotNull(requirement.getOrNull("item"), "the stack is expanded into item/amount");
        assertEquals("titanium", requirement.getOrNull("item").value);
        assertEquals("10", requirement.getOrNull("amount").value);
        assertSame(Jtype.number, requirement.getOrNull("amount").type);

        PatchNode second = path("unit.avert.firstRequirements.1");
        assertEquals("metaglass", second.getOrNull("item").value);
        assertEquals("20", second.getOrNull("amount").value);
    }

    @Test
    void appendedCommands(){
        PatchNode command = path("unit.avert.commands." + PatchJsonIO.appendPrefix + "0");
        assertSame(ModifierSign.PLUS, command.sign);
        assertEquals("mine", command.value);
    }

    @Test
    void ammoTypes(){
        PatchNode copper = path("block.duo.ammoTypes.copper");
        assertSame(ModifierSign.REMOVE, copper.sign, "'copper: -' removes the ammo entry");
        assertEquals("-", copper.value);

        PatchNode blastCompound = path("block.duo.ammoTypes.blast-compound");
        assertSame(ModifierSign.PLUS, blastCompound.sign, "an ammo entry that does not exist yet is appended");
        assertEquals("RailBulletType", blastCompound.getOrNull("type").value);
    }

    @Test
    void region(){
        PatchNode region = path("block.duo.region");
        assertSame(ModifierSign.MODIFY, region.sign, "a region overrides the field");
        assertEquals("block-atmospheric-concentrator-ui", region.value);
    }

    @Test
    void consumerRemovals(){
        assertEquals("-", path("block.duo.consumes.remove.coolant").value);
        assertEquals("-", path("block.blast-drill.consumes.remove.power").value);

        PatchNode buffered = path("block.blast-drill.consumes.powerBuffered");
        assertEquals("60", buffered.value, "an integer literal keeps its integral text");
        assertSame(Jtype.number, buffered.type);
    }

    @Test
    void drillMultipliers(){
        PatchNode copper = path("block.blast-drill.drillMultipliers.copper");
        assertSame(ModifierSign.PLUS, copper.sign, "a map key that does not exist yet is appended");
        assertEquals("4", copper.value, "the multiplier keeps its text form");
    }

    @Test
    void upgrades(){
        PatchNode appended = path("block.additive-reconstructor.upgrades." + PatchJsonIO.appendPrefix + "0");
        assertSame(ModifierSign.PLUS, appended.sign);
        assertSame(Jtype.array, appended.type);
        assertEquals("dagger", appended.getOrNull("0").value);
        assertEquals("stell", appended.getOrNull("1").value);

        PatchNode replaced = path("block.additive-reconstructor.upgrades.0");
        assertSame(ModifierSign.MODIFY, replaced.sign, "an indexed row overrides that row");
        assertEquals("merui", replaced.getOrNull("0").value);
        assertEquals("cleroi", replaced.getOrNull("1").value);
    }

    @Test
    void smelterConsumers(){
        PatchNode item = path("block.silicon-smelter.consumes.items.items.0");
        assertEquals("copper", item.getOrNull("item").value, "the item consume is unwrapped");
        assertEquals("100", item.getOrNull("amount").value);

        PatchNode liquid = path("block.silicon-smelter.consumes.liquids.liquids.0");
        assertEquals("water", liquid.getOrNull("liquid").value, "the liquid consume is unwrapped");
        assertEquals("0.1", liquid.getOrNull("amount").value);

        assertEquals("12.0", path("block.silicon-smelter.consumes.power").getOrNull("usage").value);
    }

    @Test
    void exportsBack(){
        JNode json = JsonTransform.toJsonTree(patch);

        assertNotNull(json.get("name"), "the name survives");
        assertNotNull(json.get("unit").get("toxopid").get("abilities").get(0), "the ability array survives");
        assertNotNull(json.get("unit").get("toxopid").get("weapons").get(ModifierSign.PLUS.sign),
        "the appended weapon array folds into the '+' form");
        assertNotNull(json.get("block").get("duo").get("ammoTypes").get("copper"), "the removed map key survives");
        assertNotNull(json.get("block").get("silicon-smelter").get("consumes").get("power").get("usage"),
        "the sugared power consume survives as its object form");
    }

    private static PatchNode path(String path){
        PatchNode node = patch.navigateChild(path, false);
        assertNotNull(node, "the fixture must produce a node at '" + path + "'");
        return node;
    }

    private static String fixtureText(){
        File dir = new File("").getAbsoluteFile();
        while(dir != null){
            File candidate = new File(dir, FIXTURE);
            if(candidate.isFile()){
                try{
                    return new String(Files.readAllBytes(candidate.toPath()), StandardCharsets.UTF_8);
                }catch(IOException e){
                    throw new AssertionError("failed to read " + candidate, e);
                }
            }
            dir = dir.getParentFile();
        }
        throw new AssertionError(FIXTURE + " must exist in the project (searched up from " + new File("").getAbsolutePath() + ")");
    }
}
