package com.feed_the_beast.ftblib.lib.data;

import com.feed_the_beast.ftblib.FTBLib;
import com.feed_the_beast.ftblib.events.team.ForgeTeamConfigEvent;
import com.feed_the_beast.ftblib.events.team.ForgeTeamConfigSavedEvent;
import com.feed_the_beast.ftblib.events.team.ForgeTeamDataEvent;
import com.feed_the_beast.ftblib.events.team.ForgeTeamOwnerChangedEvent;
import com.feed_the_beast.ftblib.events.team.ForgeTeamPlayerJoinedEvent;
import com.feed_the_beast.ftblib.events.team.ForgeTeamPlayerLeftEvent;
import com.feed_the_beast.ftblib.lib.EnumTeamColor;
import com.feed_the_beast.ftblib.lib.EnumTeamStatus;
import com.feed_the_beast.ftblib.lib.config.ConfigGroup;
import com.feed_the_beast.ftblib.lib.config.IConfigCallback;
import com.feed_the_beast.ftblib.lib.icon.Icon;
import com.feed_the_beast.ftblib.lib.icon.PlayerHeadIcon;
import com.feed_the_beast.ftblib.lib.util.FinalIDObject;
import com.feed_the_beast.ftblib.lib.util.StringUtils;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * @author LatvianModder
 */
public class ForgeTeam extends FinalIDObject implements INBTSerializable<NBTTagCompound>, IConfigCallback
{
	public static final int MAX_TEAM_ID_LENGTH = 35;
	public static final Pattern TEAM_ID_PATTERN = Pattern.compile("^[a-z0-9_]{1," + MAX_TEAM_ID_LENGTH + "}$");

	private final short uid;
	public final Universe universe;
	public final TeamType type;
	public ForgePlayer owner;
	private final NBTDataStorage dataStorage;
	private String title;
	private String desc;
	private EnumTeamColor color;
	private String icon;
	private boolean freeToJoin;
	private EnumTeamStatus fakePlayerStatus;
	private final Collection<ForgePlayer> requestingInvite;
	public final Map<ForgePlayer, EnumTeamStatus> players;
	private ConfigGroup cachedConfig;
	private ITextComponent cachedTitle;
	private Icon cachedIcon;
	public boolean needsSaving;

	public ForgeTeam(Universe u, short id, String n, TeamType t)
	{
		super(n, t.isNone ? 0 : (StringUtils.FLAG_ID_DEFAULTS | StringUtils.FLAG_ID_ALLOW_EMPTY));
		uid = id;
		universe = u;
		type = t;
		title = "";
		desc = "";
		color = EnumTeamColor.BLUE;
		icon = "";
		freeToJoin = false;
		fakePlayerStatus = EnumTeamStatus.ALLY;
		requestingInvite = new HashSet<>();
		players = new HashMap<>();
		dataStorage = new NBTDataStorage();
		new ForgeTeamDataEvent(this, dataStorage).post();
		clearCache();
		cachedIcon = null;
		needsSaving = false;
	}

	public final short getUID()
	{
		return uid;
	}

	public final int hashCode()
	{
		return uid;
	}

	public final boolean equals(Object o)
	{
		return o == this || uid == Objects.hashCode(o);
	}

	public final String getUIDCode()
	{
		return String.format("%04X", uid);
	}

	@Override
	public NBTTagCompound serializeNBT()
	{
		NBTTagCompound nbt = new NBTTagCompound();
		if (owner != null)
		{
			nbt.setString("Owner", owner.getName());
		}

		nbt.setString("Title", title);
		nbt.setString("Desc", desc);
		nbt.setString("Color", EnumTeamColor.NAME_MAP.getName(color));
		nbt.setString("Icon", icon);
		nbt.setBoolean("FreeToJoin", freeToJoin);
		nbt.setString("FakePlayerStatus", EnumTeamStatus.NAME_MAP_PERMS.getName(fakePlayerStatus));

		NBTTagCompound nbt1 = new NBTTagCompound();

		if (!players.isEmpty())
		{
			for (Map.Entry<ForgePlayer, EnumTeamStatus> entry : players.entrySet())
			{
				nbt1.setString(entry.getKey().getName(), entry.getValue().getName());
			}
		}

		nbt.setTag("Players", nbt1);

		NBTTagList list = new NBTTagList();

		for (ForgePlayer player : requestingInvite)
		{
			list.appendTag(new NBTTagString(player.getName()));
		}

		nbt.setTag("RequestingInvite", list);
		nbt.setTag("Data", dataStorage.serializeNBT());
		return nbt;
	}

	@Override
	public void deserializeNBT(NBTTagCompound nbt)
	{
		owner = universe.getPlayer(nbt.getString("Owner"));

		if (!isValid())
		{
			return;
		}

		title = nbt.getString("Title");
		desc = nbt.getString("Desc");
		color = EnumTeamColor.NAME_MAP.get(nbt.getString("Color"));
		icon = nbt.getString("Icon");
		freeToJoin = nbt.getBoolean("FreeToJoin");
		fakePlayerStatus = EnumTeamStatus.NAME_MAP_PERMS.get(nbt.getString("FakePlayerStatus"));

		players.clear();

		if (nbt.hasKey("Players"))
		{
			NBTTagCompound nbt1 = nbt.getCompoundTag("Players");

			for (String s : nbt1.getKeySet())
			{
				ForgePlayer player = universe.getPlayer(s);

				if (player != null)
				{
					EnumTeamStatus status = EnumTeamStatus.NAME_MAP.get(nbt1.getString(s));

					if (status.canBeSet())
					{
						setStatus(player, status);
					}
				}
			}
		}

		NBTTagList list = nbt.getTagList("RequestingInvite", Constants.NBT.TAG_STRING);

		for (int i = 0; i < list.tagCount(); i++)
		{
			ForgePlayer player = universe.getPlayer(list.getStringTagAt(i));

			if (player != null && !isMember(player))
			{
				setRequestingInvite(player, true);
			}
		}

		list = nbt.getTagList("Invited", Constants.NBT.TAG_STRING);

		for (int i = 0; i < list.tagCount(); i++)
		{
			ForgePlayer player = universe.getPlayer(list.getStringTagAt(i));

			if (player != null && !isMember(player))
			{
				setStatus(player, EnumTeamStatus.INVITED);
			}
		}

		dataStorage.deserializeNBT(nbt.getCompoundTag("Data"));
	}

	public void clearCache()
	{
		cachedTitle = null;
		cachedIcon = null;
		cachedConfig = null;
		dataStorage.clearCache();
	}

	public void markDirty()
	{
		needsSaving = true;
		universe.checkSaving = true;
	}

	public NBTDataStorage getData()
	{
		return dataStorage;
	}

	@Nullable
	public ForgePlayer getOwner()
	{
		return type.isPlayer ? owner : null;
	}

	public ITextComponent getTitle()
	{
		if (cachedTitle != null)
		{
			return cachedTitle;
		}

		if (title.isEmpty())
		{
			cachedTitle = getOwner() != null ? getOwner().getDisplayName().appendText("'s Team") : new TextComponentTranslation("ftblib.lang.team.no_team");
		}
		else
		{
			cachedTitle = new TextComponentString(title);
		}

		cachedTitle = StringUtils.color(cachedTitle, getColor().getTextFormatting());
		return cachedTitle;
	}

	public ITextComponent getCommandTitle()
	{
		ITextComponent component = getTitle().createCopy();

		if (!isValid())
		{
			return component;
		}

		component.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("/team info " + getId())));
		component.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team info " + getId()));
		component.getStyle().setColor(getColor().getTextFormatting());
		return component;
	}

	public void setTitle(String s)
	{
		if (!title.equals(s))
		{
			title = s;
			markDirty();
		}
	}

	public String getDesc()
	{
		return desc;
	}

	public void setDesc(String s)
	{
		if (!desc.equals(s))
		{
			desc = s;
			markDirty();
		}
	}

	public EnumTeamColor getColor()
	{
		return color;
	}

	public void setColor(EnumTeamColor col)
	{
		if (color != col)
		{
			color = col;
			markDirty();
		}
	}

	public Icon getIcon()
	{
		if (cachedIcon == null)
		{
			if (icon.isEmpty())
			{
				if (getOwner() != null)
				{
					cachedIcon = new PlayerHeadIcon(getOwner().getProfile().getId());
				}
				else
				{
					cachedIcon = getColor().getColor();
				}
			}
			else
			{
				cachedIcon = Icon.getIcon(icon);
			}
		}

		return cachedIcon;
	}

	public void setIcon(String s)
	{
		if (!icon.equals(s))
		{
			icon = s;
			markDirty();
		}
	}

	public boolean isFreeToJoin()
	{
		return freeToJoin;
	}

	public void setFreeToJoin(boolean b)
	{
		if (freeToJoin != b)
		{
			freeToJoin = b;
			markDirty();
		}
	}

	public EnumTeamStatus getFakePlayerStatus(ForgePlayer player)
	{
		return fakePlayerStatus;
	}

	public EnumTeamStatus getHighestStatus(@Nullable ForgePlayer player)
	{
		if (player == null)
		{
			return EnumTeamStatus.NONE;
		}
		else if (player.isFake())
		{
			return fakePlayerStatus;
		}
		else if (isOwner(player))
		{
			return EnumTeamStatus.OWNER;
		}
		else if (isModerator(player))
		{
			return EnumTeamStatus.MOD;
		}
		else if (isMember(player))
		{
			return EnumTeamStatus.MEMBER;
		}
		else if (isEnemy(player))
		{
			return EnumTeamStatus.ENEMY;
		}
		else if (isAlly(player))
		{
			return EnumTeamStatus.ALLY;
		}
		else if (isInvited(player))
		{
			return EnumTeamStatus.INVITED;
		}

		return EnumTeamStatus.NONE;
	}

	private EnumTeamStatus getSetStatus(@Nullable ForgePlayer player)
	{
		if (player == null || !isValid())
		{
			return EnumTeamStatus.NONE;
		}
		else if (player.isFake())
		{
			return fakePlayerStatus;
		}
		else if (type == TeamType.SERVER && getId().equals("singleplayer"))
		{
			return EnumTeamStatus.MOD;
		}

		EnumTeamStatus status = players.get(player);
		return status == null ? EnumTeamStatus.NONE : status;
	}

	public boolean hasStatus(@Nullable ForgePlayer player, EnumTeamStatus status)
	{
		if (player == null || !isValid())
		{
			return false;
		}

		if (player.isFake())
		{
			return getFakePlayerStatus(player).isEqualOrGreaterThan(status);
		}

		switch (status)
		{
			case NONE:
				return true;
			case ENEMY:
				return isEnemy(player);
			case ALLY:
				return isAlly(player);
			case INVITED:
				return isInvited(player);
			case MEMBER:
				return isMember(player);
			case MOD:
				return isModerator(player);
			case OWNER:
				return isOwner(player);
			default:
				return false;
		}
	}

	public boolean setStatus(@Nullable ForgePlayer player, EnumTeamStatus status)
	{
		if (player == null || !isValid() || player.isFake())
		{
			return false;
		}
		else if (status == EnumTeamStatus.OWNER)
		{
			if (!isMember(player))
			{
				return false;
			}

			if (!player.equalsPlayer(getOwner()))
			{
				universe.clearCache();
				ForgePlayer oldOwner = getOwner();
				owner = player;
				players.remove(player);
				new ForgeTeamOwnerChangedEvent(this, oldOwner).post();

				if (oldOwner != null)
				{
					oldOwner.markDirty();
				}

				owner.markDirty();
				markDirty();
				return true;
			}

			return false;
		}
		else if (!status.isNone() && status.canBeSet())
		{
			if (players.put(player, status) != status)
			{
				universe.clearCache();
				player.markDirty();
				markDirty();
				return true;
			}
		}
		else if (players.remove(player) != status)
		{
			universe.clearCache();
			player.markDirty();
			markDirty();
			return true;
		}

		return false;
	}

	public <C extends Collection<ForgePlayer>> C getPlayersWithStatus(C collection, EnumTeamStatus status)
	{
		if (!isValid())
		{
			return collection;
		}

		for (ForgePlayer player : universe.getPlayers())
		{
			if (!player.isFake() && hasStatus(player, status))
			{
				collection.add(player);
			}
		}

		return collection;
	}

	public List<ForgePlayer> getPlayersWithStatus(EnumTeamStatus status)
	{
		return isValid() ? getPlayersWithStatus(new ArrayList<>(), status) : Collections.emptyList();
	}

	public boolean addMember(ForgePlayer player, boolean simulate)
	{
		if (isValid() && ((isOwner(player) || isInvited(player)) && !isMember(player)))
		{
			if (!simulate)
			{
				universe.clearCache();
				player.team = this;
				players.remove(player);
				requestingInvite.remove(player);

				ForgeTeamPlayerJoinedEvent event = new ForgeTeamPlayerJoinedEvent(player);
				event.post();

				if (event.getDisplayGui() != null)
				{
					event.getDisplayGui().run();
				}

				player.markDirty();
				markDirty();
			}

			return true;
		}

		return false;
	}

	public boolean removeMember(ForgePlayer player)
	{
		if (!isValid() || !isMember(player))
		{
			return false;
		}
		else if (getMembers().size() == 1)
		{
			universe.clearCache();
			new ForgeTeamPlayerLeftEvent(player).post();

			if (type.isPlayer)
			{
				delete();
			}
			else
			{
				setStatus(player, EnumTeamStatus.NONE);
			}

			player.team = universe.getTeam("");
			player.markDirty();
			markDirty();
		}
		else if (isOwner(player))
		{
			return false;
		}

		universe.clearCache();
		new ForgeTeamPlayerLeftEvent(player).post();
		player.team = universe.getTeam("");
		setStatus(player, EnumTeamStatus.NONE);
		player.markDirty();
		markDirty();
		return true;
	}

	public void delete()
	{
		universe.removeTeam(this);
	}

	public List<ForgePlayer> getMembers()
	{
		return getPlayersWithStatus(EnumTeamStatus.MEMBER);
	}

	public boolean isMember(@Nullable ForgePlayer player)
	{
		if (player == null)
		{
			return false;
		}
		else if (player.isFake())
		{
			return fakePlayerStatus.isEqualOrGreaterThan(EnumTeamStatus.MEMBER);
		}

		return isValid() && equalsTeam(player.team);
	}

	public boolean isAlly(@Nullable ForgePlayer player)
	{
		return isValid() && (isMember(player) || getSetStatus(player).isEqualOrGreaterThan(EnumTeamStatus.ALLY));
	}

	public boolean isInvited(@Nullable ForgePlayer player)
	{
		return isValid() && (isMember(player) || ((isFreeToJoin() || getSetStatus(player).isEqualOrGreaterThan(EnumTeamStatus.INVITED)) && !isEnemy(player)));
	}

	public boolean setRequestingInvite(@Nullable ForgePlayer player, boolean value)
	{
		if (player != null && isValid())
		{
			if (value)
			{
				if (requestingInvite.add(player))
				{
					player.markDirty();
					markDirty();
					return true;
				}
			}
			else if (requestingInvite.remove(player))
			{
				player.markDirty();
				markDirty();
				return true;
			}

			return false;
		}

		return false;
	}

	public boolean isRequestingInvite(@Nullable ForgePlayer player)
	{
		return player != null && isValid() && !isMember(player) && requestingInvite.contains(player) && !isEnemy(player);
	}

	public boolean isEnemy(@Nullable ForgePlayer player)
	{
		return getSetStatus(player) == EnumTeamStatus.ENEMY;
	}

	public boolean isModerator(@Nullable ForgePlayer player)
	{
		return isOwner(player) || isMember(player) && getSetStatus(player).isEqualOrGreaterThan(EnumTeamStatus.MOD);
	}

	public boolean isOwner(@Nullable ForgePlayer player)
	{
		return player != null && player.equalsPlayer(getOwner());
	}

	public ConfigGroup getSettings()
	{
		if (cachedConfig == null)
		{
			cachedConfig = ConfigGroup.newGroup("team_config");
			cachedConfig.setDisplayName(new TextComponentTranslation("gui.settings").appendSibling(StringUtils.bold(StringUtils.color(new TextComponentString(" #" + toString()), TextFormatting.DARK_GRAY), false)));
			ForgeTeamConfigEvent event = new ForgeTeamConfigEvent(this, cachedConfig);
			event.post();

			ConfigGroup main = cachedConfig.getGroup(FTBLib.MOD_ID);
			main.setDisplayName(new TextComponentString(FTBLib.MOD_NAME));
			main.addBool("free_to_join", () -> freeToJoin, v -> freeToJoin = v, false);

			ConfigGroup display = main.getGroup("display");
			display.addEnum("color", () -> color, v -> color = v, EnumTeamColor.NAME_MAP);
			display.addEnum("fake_player_status", () -> fakePlayerStatus, v -> fakePlayerStatus = v, EnumTeamStatus.NAME_MAP_PERMS);
			display.addString("title", () -> title, v -> title = v, "");
			display.addString("desc", () -> desc, v -> desc = v, "");
		}

		return cachedConfig;
	}

	public boolean isValid()
	{
		if (type.isNone)
		{
			return false;
		}

		return type.isServer || getOwner() != null;
	}

	public boolean equalsTeam(@Nullable ForgeTeam team)
	{
		return team == this || uid == Objects.hashCode(team);
	}

	public boolean anyPlayerHasPermission(String permission, EnumTeamStatus status)
	{
		for (ForgePlayer player : getPlayersWithStatus(status))
		{
			if (player.hasPermission(permission))
			{
				return true;
			}
		}

		return false;
	}

	public File getDataFile(String ext)
	{
		File dir = new File(universe.getWorldDirectory(), "data/ftb_lib/teams/");

		if (ext.isEmpty())
		{
			return new File(dir, getId() + ".dat");
		}

		File extFolder = new File(dir, ext);

		if (!extFolder.exists())
		{
			extFolder.mkdirs();
		}

		File extFile = new File(extFolder, getId() + ".dat");

		if (!extFile.exists())
		{
			File oldExtFile = new File(dir, getId() + "." + ext + ".dat");

			if (oldExtFile.exists())
			{
				oldExtFile.renameTo(extFile);
				oldExtFile.deleteOnExit();
			}
		}

		return extFile;
	}

	@Override
	public void onConfigSaved(ConfigGroup group, ICommandSender sender)
	{
		clearCache();
		markDirty();
		new ForgeTeamConfigSavedEvent(this, group, sender).post();
	}

	public List<EntityPlayerMP> getOnlineMembers()
	{
		List<EntityPlayerMP> list = new ArrayList<>();

		for (ForgePlayer player : getMembers())
		{
			if (player.isOnline())
			{
				list.add(player.getPlayer());
			}
		}

		return list;
	}
}