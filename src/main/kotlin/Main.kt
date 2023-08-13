
import me.heroostech.geyserutils.InstanceHolder
import me.heroostech.geyserutils.component.ButtonComponent
import me.heroostech.geyserutils.forms.SimpleForm
import me.heroostech.geyserutils.forms.response.SimpleFormResponse
import me.heroostech.geyserutils.minestom.GeyserUtils
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.MinecraftServer
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.CommandExecutor
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryClickEvent
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.player.PlayerLoginEvent
import net.minestom.server.event.player.PlayerResourcePackStatusEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.extras.velocity.VelocityProxy
import net.minestom.server.instance.Instance
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.block.Block
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect
import net.minestom.server.resourcepack.ResourcePack
import net.minestom.server.resourcepack.ResourcePackStatus
import net.minestom.server.timer.TaskSchedule
import net.minestom.server.utils.NamespaceID
import net.minestom.server.utils.chunk.ChunkSupplier
import net.minestom.server.world.DimensionType
import net.minestom.server.world.biomes.Biome
import net.minestom.server.world.biomes.BiomeEffects
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File


class TestCommand : Command("mem") {
    init {
        // Executed if no other executor can be used
        defaultExecutor = CommandExecutor { sender: CommandSender, context: CommandContext? ->
            sender.sendMessage(
                ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024).toString()
            )
        }

    }
}

val pinv: HashMap<Player, Inventory> = HashMap()
val smp_button = arrayOf(0,1,2,3,9,10,11,12,18,19,20,21,27,28,29)
val mystery_button = arrayOf(5,6,7,8,14,15,16,17,23,24,25,26,32,33,34,35)

fun connectSMP(player: Player) {
    player.closeInventory()
    player.sendPluginMessage("geyserutils:stop", ByteArray(0))
    val os = ByteArrayOutputStream()
    val buf = DataOutputStream(os)
    buf.writeUTF("Connect")
    buf.writeUTF("smp")
    buf.close()
    println("Sending x bytes: " + os.toByteArray().size)
    player.sendPluginMessage("bungeecord:main", os.toByteArray())
}



fun main(args: Array<String>) {
    val server = MinecraftServer.init()
    val config = File("./velocity-secret.txt")
    if (!config.exists()) {
        println("Error: Please create velocity-secret.txt with velocity proxy secret")
        return
    }
    VelocityProxy.enable(config.readText())
    val manager = MinecraftServer.getInstanceManager()
    MinecraftServer.getCommandManager().register(TestCommand())
    val dt = DimensionType.builder(NamespaceID.from("minecraft:the_end"))
        .ambientLight(1f).skylightEnabled(false).effects("minecraft:the_end").build()
    MinecraftServer.getDimensionTypeManager().addDimension(dt)
    val hub = manager.createInstanceContainer(dt)
    val b = Biome.builder().name(NamespaceID.from("minecraft:the_end"))
            .effects(BiomeEffects.builder().skyColor(0).fogColor(0).build()).build()
    MinecraftServer.getBiomeManager().addBiome(b)
    hub.setGenerator {
        it.modifier().fillHeight(30, 49, Block.BARRIER)
        it.modifier().fillBiome(b)
        it.modifier().fillHeight(30, 49, Block.BARRIER)
    }
    hub.chunkSupplier = ChunkSupplier { instance: Instance?, chunkX: Int, chunkZ: Int ->
        LightingChunk(
            instance!!, chunkX, chunkZ
        )
    }
    hub.timeRate = 0
    hub.time = 6000
    val events = MinecraftServer.getGlobalEventHandler()

    GeyserUtils().initialize(events)
    events.addListener(PlayerLoginEvent::class.java) {
        println("Login recieved")

        it.setSpawningInstance(hub)
        it.player.respawnPoint = Pos(0.0,51.0,0.0)
    }
    events.addListener(PlayerSpawnEvent::class.java) {
        it.player.addEffect(Potion(PotionEffect.INVISIBILITY, 1, 9999, Potion.ICON_FLAG))
        it.player.setResourcePack(ResourcePack.forced("https://download.mc-packs.net/pack/ca62f3487e892d9babe36cf597c7929fc4b60596.zip",
            "ca62f3487e892d9babe36cf597c7929fc4b60596", Component.text("Accept pwetty please")))
        it.player.setAutoViewEntities(false)
        it.player.isAutoViewable = false
        val ui = Inventory(InventoryType.CHEST_6_ROW, Component.text("\u2704\u2702\u2703\u2700        \u2701").color(NamedTextColor.WHITE))
        pinv[it.player] = ui
        val player = it.player
        player.sendMessage(Component.text("Welcome, ").color(NamedTextColor.GREEN)
            .append(player.name.color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)))
        MinecraftServer.getSchedulerManager().submitTask {
            if (!player.isOnline)
                TaskSchedule.stop()
            else {
                if (InstanceHolder.getApi().isFloodgatePlayer(player.uuid)) {
                    val fplayer = InstanceHolder.getApi().getPlayer(player.uuid)!!
                    val comps = ArrayList<ButtonComponent>()
                    comps.add(ButtonComponent("SMP", null))
                    comps.add(ButtonComponent("Back", null))
                    val form = SimpleForm("Menu", "Select a server", comps, player.uuid)
                    fplayer.sendForm(form)
                }
                TaskSchedule.tick(20)
            }
        }

    }
    events.addListener(SimpleFormResponse::class.java) {
        when (it.button.text) {
            "SMP" -> connectSMP(it.player)
            "Back" -> it.player.kick(Component.text("Menu closed").color(NamedTextColor.RED))
        }
    }
    events.addListener(PlayerResourcePackStatusEvent::class.java) {
        if (it.status == ResourcePackStatus.DECLINED) {
            it.player.kick("Resource pack required")
        }
        it.player.openInventory(pinv[it.player]!!)
    }
    events.addListener(InventoryCloseEvent::class.java) {
        it.player.kick(Component.text("Menu closed").color(NamedTextColor.RED))
    }
    events.addListener(InventoryClickEvent::class.java) {
        if (it.slot in smp_button) {
            connectSMP(it.player)
        }
        if (it.slot in mystery_button) {
            val sound = Sound.sound(Key.key("block.glass.break"), Sound.Source.MASTER, 1f, 1f)
            it.player.playSound(sound)
        }
    }

    server.start("localhost",25569);
}