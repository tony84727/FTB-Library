package com.feed_the_beast.ftblib.lib.data;

import com.feed_the_beast.ftblib.FTBLib;
import com.feed_the_beast.ftblib.FTBLibConfig;
import com.feed_the_beast.ftblib.events.ServerReloadEvent;
import com.feed_the_beast.ftblib.events.player.ForgePlayerLoadedEvent;
import com.feed_the_beast.ftblib.events.player.ForgePlayerSavedEvent;
import com.feed_the_beast.ftblib.events.team.ForgeTeamDeletedEvent;
import com.feed_the_beast.ftblib.events.team.ForgeTeamLoadedEvent;
import com.feed_the_beast.ftblib.events.team.ForgeTeamSavedEvent;
import com.feed_the_beast.ftblib.events.universe.PersistentScheduledTaskEvent;
import com.feed_the_beast.ftblib.events.universe.UniverseClearCacheEvent;
import com.feed_the_beast.ftblib.events.universe.UniverseClosedEvent;
import com.feed_the_beast.ftblib.events.universe.UniverseLoadedEvent;
import com.feed_the_beast.ftblib.events.universe.UniverseSavedEvent;
import com.feed_the_beast.ftblib.lib.ATHelper;
import com.feed_the_beast.ftblib.lib.EnumReloadType;
import com.feed_the_beast.ftblib.lib.EnumTeamColor;
import com.feed_the_beast.ftblib.lib.io.DataReader;
import com.feed_the_beast.ftblib.lib.math.MathUtils;
import com.feed_the_beast.ftblib.lib.math.Ticks;
import com.feed_the_beast.ftblib.lib.util.FileUtils;
import com.feed_the_beast.ftblib.lib.util.NBTUtils;
import com.feed_the_beast.ftblib.lib.util.ServerUtils;
import com.feed_the_beast.ftblib.lib.util.StringUtils;
import com.feed_the_beast.ftblib.lib.util.misc.IScheduledTask;
import com.feed_the_beast.ftblib.lib.util.misc.TimeType;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author LatvianModder
 */
@Mod.EventBusSubscriber(modid = FTBLib.MOD_ID)
public class Universe
{
	private static class ScheduledTask
	{
		private final TimeType type;
		private final long time;
		private final IScheduledTask task;

		public ScheduledTask(TimeType tt, long t, IScheduledTask tk)
		{
			type = tt;
			time = t;
			task = tk;
		}
	}

	private static class PersistentScheduledTask
	{
		private final ResourceLocation id;
		private final TimeType type;
		private final long time;
		private final NBTTagCompound data;

		public PersistentScheduledTask(ResourceLocation i, TimeType tt, long t, NBTTagCompound d)
		{
			id = i;
			type = tt;
			time = t;
			data = d;
		}
	}

	private static final HashSet<UUID> LOGGED_IN_PLAYERS = new HashSet<>(); //Required because of a Forge bug https://github.com/MinecraftForge/MinecraftForge/issues/5696
	private static Universe INSTANCE = null;

	public static boolean loaded()
	{
		return INSTANCE != null;
	}

	public static Universe get()
	{
		if (INSTANCE == null)
		{
			throw new NullPointerException("FTBLib Universe == null!");
		}

		return INSTANCE;
	}

	// Event handlers start //

	@SubscribeEvent
	public static void onWorldLoaded(WorldEvent.Load event)
	{
		if (event.getWorld().provider.getDimension() == 0 && !event.getWorld().isRemote)
		{
			INSTANCE = new Universe((WorldServer) event.getWorld());
			INSTANCE.load();
		}
	}

	@SubscribeEvent
	public static void onWorldUnloaded(WorldEvent.Unload event)
	{
		if (loaded() && event.getWorld() == INSTANCE.world)
		{
			for (ForgePlayer player : INSTANCE.getPlayers())
			{
				if (player.isOnline())
				{
					player.onLoggedOut(player.getPlayer());
				}
			}

			LOGGED_IN_PLAYERS.clear();
			INSTANCE.save();
			new UniverseClosedEvent(INSTANCE).post();
			INSTANCE = null;
		}
	}

	@SubscribeEvent
	public static void onWorldSaved(WorldEvent.Save event)
	{
		if (loaded() && event.getWorld() == INSTANCE.world)
		{
			INSTANCE.save();
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
	{
		if (loaded() && event.player instanceof EntityPlayerMP && !ServerUtils.isFake((EntityPlayerMP) event.player))
		{
			LOGGED_IN_PLAYERS.add(event.player.getUniqueID());
			INSTANCE.onPlayerLoggedIn((EntityPlayerMP) event.player);
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
	{
		if (loaded() && event.player instanceof EntityPlayerMP && LOGGED_IN_PLAYERS.remove(event.player.getUniqueID()))
		{
			ForgePlayer p = INSTANCE.getPlayer(event.player.getGameProfile());

			if (p != null)
			{
				p.onLoggedOut((EntityPlayerMP) event.player);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onPlayerClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event)
	{
		if (event.getEntity() instanceof EntityPlayerMP)
		{
			ForgePlayer p = INSTANCE.getPlayer(event.getEntityPlayer().getGameProfile());

			if (p != null)
			{
				p.tempPlayer = (EntityPlayerMP) event.getEntity();
			}

			INSTANCE.clearCache();

			if (p != null)
			{
				p.tempPlayer = null;
			}
		}
	}

	@SubscribeEvent
	public static void onTickEvent(TickEvent.WorldTickEvent event)
	{
		if (!loaded())
		{
			return;
		}

		Universe universe = get();

		if (event.phase == TickEvent.Phase.START)
		{
			universe.ticks = Ticks.get(universe.world.getTotalWorldTime());
		}
		else if (!event.world.isRemote && event.world.provider.getDimension() == 0)
		{
			universe.scheduledTasks.addAll(universe.scheduledTaskQueue);
			universe.scheduledTaskQueue.clear();
			universe.persistentScheduledTasks.addAll(universe.persistentScheduledTaskQueue);
			universe.persistentScheduledTaskQueue.clear();

			Iterator<ScheduledTask> iterator = universe.scheduledTasks.iterator();

			while (iterator.hasNext())
			{
				ScheduledTask task = iterator.next();

				if (task.task.isComplete(universe, task.type, task.time))
				{
					task.task.execute(universe);
					iterator.remove();
				}
			}

			Iterator<PersistentScheduledTask> piterator = universe.persistentScheduledTasks.iterator();

			while (piterator.hasNext())
			{
				PersistentScheduledTask task = piterator.next();

				if ((task.type == TimeType.TICKS ? universe.ticks.ticks() : System.currentTimeMillis()) >= task.time)
				{
					new PersistentScheduledTaskEvent(universe, task.id, task.data).post();
					piterator.remove();
				}
			}

			if (universe.server.isSinglePlayer())
			{
				boolean cheats = ATHelper.areCommandsAllowedForAll(universe.server.getPlayerList());

				if (universe.prevCheats != cheats)
				{
					universe.prevCheats = cheats;
					universe.clearCache();
				}
			}
		}
	}

	// Event handler end //

	@Nonnull
	public final MinecraftServer server;
	public final WorldServer world;
	public final Map<UUID, ForgePlayer> players;
	private final Map<String, ForgeTeam> teams;
	private final Short2ObjectOpenHashMap<ForgeTeam> teamMap;
	private final ForgeTeam noneTeam;
	private UUID uuid;
	private boolean needsSaving;
	boolean checkSaving;
	public ForgeTeam fakePlayerTeam;
	public FakeForgePlayer fakePlayer;
	private final List<ScheduledTask> scheduledTasks;
	private final List<PersistentScheduledTask> persistentScheduledTasks;
	private final List<ScheduledTask> scheduledTaskQueue;
	private final List<PersistentScheduledTask> persistentScheduledTaskQueue;
	public Ticks ticks;
	private boolean prevCheats = false;

	@SuppressWarnings("ConstantConditions")
	public Universe(WorldServer w)
	{
		server = w.getMinecraftServer();
		world = w;
		ticks = Ticks.get(world.getTotalWorldTime());
		players = new HashMap<>();
		teams = new HashMap<>();
		teamMap = new Short2ObjectOpenHashMap<>();
		noneTeam = new ForgeTeam(this, (short) 0, "", TeamType.NONE);
		uuid = null;
		needsSaving = false;
		checkSaving = true;
		scheduledTasks = new ArrayList<>();
		persistentScheduledTasks = new ArrayList<>();
		scheduledTaskQueue = new ArrayList<>();
		persistentScheduledTaskQueue = new ArrayList<>();
	}

	public void markDirty()
	{
		needsSaving = true;
		checkSaving = true;
	}

	public UUID getUUID()
	{
		if (uuid == null)
		{
			uuid = UUID.randomUUID();
			markDirty();
		}

		return uuid;
	}

	public void scheduleTask(TimeType type, long time, IScheduledTask task)
	{
		scheduledTaskQueue.add(new ScheduledTask(type, time, task));
	}

	public void scheduleTask(ResourceLocation id, TimeType type, long time, NBTTagCompound data)
	{
		persistentScheduledTaskQueue.add(new PersistentScheduledTask(id, type, time, data));
		markDirty();
	}

	private void load()
	{
		File folder = new File(getWorldDirectory(), "data/ftb_lib/");
		NBTTagCompound universeData = NBTUtils.readNBT(new File(folder, "universe.dat"));

		if (universeData == null)
		{
			universeData = new NBTTagCompound();
		}

		File worldDataJsonFile = new File(getWorldDirectory(), "world_data.json");
		JsonElement worldData = DataReader.get(worldDataJsonFile).safeJson();

		if (worldData.isJsonObject())
		{
			JsonObject jsonWorldData = worldData.getAsJsonObject();

			if (jsonWorldData.has("world_id"))
			{
				universeData.setString("UUID", jsonWorldData.get("world_id").getAsString());
			}

			worldDataJsonFile.delete();
		}

		uuid = StringUtils.fromString(universeData.getString("UUID"));

		if (uuid != null && uuid.getLeastSignificantBits() == 0L && uuid.getMostSignificantBits() == 0L)
		{
			uuid = null;
		}

		NBTTagList taskTag = universeData.getTagList("PersistentScheduledTasks", Constants.NBT.TAG_COMPOUND);

		for (int i = 0; i < taskTag.tagCount(); i++)
		{
			NBTTagCompound taskData = taskTag.getCompoundTagAt(i);
			persistentScheduledTasks.add(new PersistentScheduledTask(new ResourceLocation(taskData.getString("ID")), TimeType.NAME_MAP.get(taskData.getString("Type")), taskData.getLong("Time"), taskData.getCompoundTag("Data")));
		}

		NBTTagCompound data = universeData.getCompoundTag("Data");

		new UniverseLoadedEvent.Pre(this, world, data).post();

		Map<UUID, NBTTagCompound> playerNBT = new HashMap<>();
		Map<String, NBTTagCompound> teamNBT = new HashMap<>();

		try
		{
			File[] files = new File(folder, "players").listFiles();

			if (files != null && files.length > 0)
			{
				for (File file : files)
				{
					if (file.getName().indexOf('.') == file.getName().lastIndexOf('.'))
					{
						NBTTagCompound nbt = NBTUtils.readNBT(file);

						if (nbt != null)
						{
							String uuidString = nbt.getString("UUID");

							if (uuidString.isEmpty())
							{
								uuidString = FileUtils.getBaseName(file);
								FileUtils.deleteSafe(file);
							}

							UUID uuid = StringUtils.fromString(uuidString);

							if (uuid != null)
							{
								playerNBT.put(uuid, nbt);
								ForgePlayer player = new ForgePlayer(this, uuid, nbt.getString("Name"));
								players.put(uuid, player);
							}
						}
					}
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		try
		{
			File[] files = new File(folder, "teams").listFiles();

			if (files != null && files.length > 0)
			{
				for (File file : files)
				{
					if (file.isFile() && file.getName().indexOf('.') == file.getName().lastIndexOf('.'))
					{
						NBTTagCompound nbt = NBTUtils.readNBT(file);

						if (nbt != null)
						{
							String s = nbt.getString("ID");

							if (s.isEmpty())
							{
								s = FileUtils.getBaseName(file);
							}

							teamNBT.put(s, nbt);
							short uid = nbt.getShort("UID");
							ForgeTeam team = new ForgeTeam(this, generateTeamUID(uid), s, TeamType.NAME_MAP.get(nbt.getString("Type")));
							addTeam(team);

							if (uid == 0)
							{
								team.markDirty();
							}
						}
					}
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		fakePlayerTeam = new ForgeTeam(this, (short) 1, "fakeplayer", TeamType.SERVER_NO_SAVE)
		{
			@Override
			public void markDirty()
			{
				Universe.this.markDirty();
			}
		};

		fakePlayer = new FakeForgePlayer(this);
		fakePlayer.team = fakePlayerTeam;
		fakePlayerTeam.setColor(EnumTeamColor.GRAY);

		new UniverseLoadedEvent.CreateServerTeams(this, world).post();

		for (ForgePlayer player : players.values())
		{
			NBTTagCompound nbt = playerNBT.get(player.getId());

			if (nbt != null && !nbt.isEmpty())
			{
				player.team = getTeam(nbt.getString("TeamID"));
				player.deserializeNBT(nbt);
			}

			new ForgePlayerLoadedEvent(player).post();
		}

		for (ForgeTeam team : getTeams())
		{
			if (!team.type.save)
			{
				continue;
			}

			NBTTagCompound nbt = teamNBT.get(team.getId());

			if (nbt != null && !nbt.isEmpty())
			{
				team.deserializeNBT(nbt);
			}

			new ForgeTeamLoadedEvent(team).post();
		}

		if (universeData.hasKey("FakePlayer"))
		{
			fakePlayer.deserializeNBT(universeData.getCompoundTag("FakePlayer"));
		}

		if (universeData.hasKey("FakeTeam"))
		{
			fakePlayerTeam.deserializeNBT(universeData.getCompoundTag("FakeTeam"));
		}

		fakePlayerTeam.owner = fakePlayer;

		new UniverseLoadedEvent.Post(this, world, data).post();
		new UniverseLoadedEvent.Finished(this, world).post();

		FTBLibAPI.reloadServer(this, server, EnumReloadType.CREATED, ServerReloadEvent.ALL);
	}

	private void save()
	{
		if (!checkSaving)
		{
			return;
		}

		if (needsSaving)
		{
			if (FTBLibConfig.debugging.print_more_info)
			{
				FTBLib.LOGGER.info("Saving universe data");
			}

			NBTTagCompound universeData = new NBTTagCompound();
			NBTTagCompound data = new NBTTagCompound();
			new UniverseSavedEvent(this, data).post();
			universeData.setTag("Data", data);
			universeData.setString("UUID", StringUtils.fromUUID(getUUID()));

			NBTTagList taskTag = new NBTTagList();

			for (PersistentScheduledTask task : persistentScheduledTasks)
			{
				NBTTagCompound taskData = new NBTTagCompound();
				taskData.setString("ID", task.id.toString());
				taskData.setString("Type", TimeType.NAME_MAP.getName(task.type));
				taskData.setLong("Time", task.time);
				taskData.setTag("Data", task.data);
				taskTag.appendTag(taskData);
			}

			universeData.setTag("PersistentScheduledTasks", taskTag);
			universeData.setTag("FakePlayer", fakePlayer.serializeNBT());
			universeData.setTag("FakeTeam", fakePlayerTeam.serializeNBT());
			NBTUtils.writeNBTSafe(new File(getWorldDirectory(), "data/ftb_lib/universe.dat"), universeData);
			needsSaving = false;
		}

		for (ForgePlayer player : players.values())
		{
			if (player.needsSaving)
			{
				if (FTBLibConfig.debugging.print_more_info)
				{
					FTBLib.LOGGER.info("Saved player data for " + player.getName());
				}

				NBTTagCompound nbt = player.serializeNBT();
				nbt.setString("Name", player.getName());
				nbt.setString("UUID", StringUtils.fromUUID(player.getId()));
				nbt.setString("TeamID", player.team.getId());
				NBTUtils.writeNBTSafe(player.getDataFile(""), nbt);
				new ForgePlayerSavedEvent(player).post();
				player.needsSaving = false;
			}
		}

		for (ForgeTeam team : getTeams())
		{
			if (team.needsSaving)
			{
				if (FTBLibConfig.debugging.print_more_info)
				{
					FTBLib.LOGGER.info("Saved team data for " + team.getId());
				}

				File file = team.getDataFile("");

				if (team.type.save && team.isValid())
				{
					NBTTagCompound nbt = team.serializeNBT();
					nbt.setString("ID", team.getId());
					nbt.setShort("UID", team.getUID());
					nbt.setString("Type", team.type.getName());
					NBTUtils.writeNBTSafe(file, nbt);
					new ForgeTeamSavedEvent(team).post();
					team.needsSaving = false;
				}
				else if (file.exists())
				{
					file.delete();
				}

				team.needsSaving = false;
			}
		}

		checkSaving = false;
	}

	public File getWorldDirectory()
	{
		return world.getSaveHandler().getWorldDirectory();
	}

	private void onPlayerLoggedIn(EntityPlayerMP player)
	{
		if (!player.server.getPlayerList().canJoin(player.getGameProfile()))
		{
			return;
		}

		ForgePlayer p = getPlayer(player.getGameProfile());

		if (p != null && p.isOnline())
		{
			return;
		}

		boolean firstLogin = p == null;

		if (firstLogin)
		{
			p = new ForgePlayer(this, player.getUniqueID(), player.getName());
			players.put(p.getId(), p);
		}
		else if (!p.getName().equals(player.getName()))
		{
			p.setName(player.getName());
		}

		p.onLoggedIn(player, this, firstLogin);
	}

	public Collection<ForgePlayer> getPlayers()
	{
		return players.values();
	}

	@Nullable
	public ForgePlayer getPlayer(@Nullable UUID id)
	{
		if (id == null)
		{
			return null;
		}
		else if (id.equals(ServerUtils.FAKE_PLAYER_PROFILE.getId()))
		{
			return fakePlayer;
		}

		return players.get(id);
	}

	@Nullable
	public ForgePlayer getPlayer(CharSequence nameOrId)
	{
		String s = nameOrId.toString().toLowerCase();

		if (s.isEmpty())
		{
			return null;
		}

		UUID id = StringUtils.fromString(s);

		if (id != null)
		{
			return getPlayer(id);
		}
		else if (s.equals(ServerUtils.FAKE_PLAYER_PROFILE.getName().toLowerCase()))
		{
			return fakePlayer;
		}

		for (ForgePlayer p : players.values())
		{
			if (p.getName().toLowerCase().equals(s))
			{
				return p;
			}
		}

		for (ForgePlayer p : players.values())
		{
			if (p.getName().toLowerCase().contains(s))
			{
				return p;
			}
		}

		return null;
	}

	public ForgePlayer getPlayer(@Nullable ICommandSender sender)
	{
		if (sender instanceof EntityPlayerMP)
		{
			EntityPlayerMP player = (EntityPlayerMP) sender;

			if (ServerUtils.isFake(player))
			{
				fakePlayer.tempPlayer = player;
				fakePlayer.clearCache();
				return fakePlayer;
			}

			ForgePlayer p = getPlayer(player.getGameProfile());

			if (p == null)
			{
				throw new NullPointerException("Player can't be found for " + player.getName() + ":" + StringUtils.fromUUID(player.getUniqueID()) + ":" + player.getClass().getName());
			}

			return p;
		}

		throw new IllegalArgumentException("Sender is not a player!");
	}

	public ForgePlayer getPlayer(ForgePlayer player)
	{
		ForgePlayer p = getPlayer(player.getId());
		return p == null ? player : p;
	}

	@Nullable
	public ForgePlayer getPlayer(GameProfile profile)
	{
		ForgePlayer player = getPlayer(profile.getId());

		if (player == null && FTBLibConfig.general.merge_offline_mode_players.get(!server.isDedicatedServer()))
		{
			player = getPlayer(profile.getName());

			if (player != null)
			{
				players.put(profile.getId(), player);
				player.markDirty();
			}
		}

		return player;
	}

	public Collection<ForgeTeam> getTeams()
	{
		return teams.values();
	}

	public ForgeTeam getTeam(String id)
	{
		if (id.isEmpty())
		{
			return noneTeam;
		}
		else if (id.length() == 4)
		{
			try
			{
				ForgeTeam team = getTeam(Integer.valueOf(id, 16).shortValue());

				if (team.isValid())
				{
					return team;
				}
			}
			catch (Exception ex)
			{
			}
		}

		if (id.equals("fakeplayer"))
		{
			return fakePlayerTeam;
		}

		ForgeTeam team = teams.get(id);

		if (team != null)
		{
			return team;
		}

		ForgePlayer player = getPlayer(id);

		if (player != null)
		{
			return player.team;
		}

		return noneTeam;
	}

	public ForgeTeam getTeam(short uid)
	{
		if (uid == 0)
		{
			return noneTeam;
		}
		else if (uid == 1)
		{
			return fakePlayerTeam;
		}

		ForgeTeam team = teamMap.get(uid);
		return team == null ? noneTeam : team;
	}

	public Collection<ForgePlayer> getOnlinePlayers()
	{
		Collection<ForgePlayer> set = Collections.emptySet();

		for (ForgePlayer player : getPlayers())
		{
			if (player.isOnline())
			{
				if (set.isEmpty())
				{
					set = new HashSet<>();
				}

				set.add(player);
			}
		}

		return set;
	}

	public void clearCache()
	{
		new UniverseClearCacheEvent(this).post();
		getTeams().forEach(ForgeTeam::clearCache);
		getPlayers().forEach(ForgePlayer::clearCache);
		fakePlayer.clearCache();
	}

	public void addTeam(ForgeTeam team)
	{
		teamMap.put(team.getUID(), team);
		teams.put(team.getId(), team);
	}

	public void removeTeam(ForgeTeam team)
	{
		File folder = new File(getWorldDirectory(), "data/ftb_lib/teams/");
		new ForgeTeamDeletedEvent(team, folder).post();
		teamMap.remove(team.getUID());
		teams.remove(team.getId());
		FileUtils.deleteSafe(new File(folder, team.getId() + ".dat"));
		markDirty();
		clearCache();
	}

	public short generateTeamUID(short id)
	{
		while (id == 0 || id == 1 || id == 2 || teamMap.containsKey(id))
		{
			id = (short) MathUtils.RAND.nextInt();
		}

		return id;
	}
}