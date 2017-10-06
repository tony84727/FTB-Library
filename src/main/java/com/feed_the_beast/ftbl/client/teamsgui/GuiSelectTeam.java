package com.feed_the_beast.ftbl.client.teamsgui;

import com.feed_the_beast.ftbl.lib.client.ClientUtils;
import com.feed_the_beast.ftbl.lib.gui.Button;
import com.feed_the_beast.ftbl.lib.gui.GuiBase;
import com.feed_the_beast.ftbl.lib.gui.GuiIcons;
import com.feed_the_beast.ftbl.lib.gui.Panel;
import com.feed_the_beast.ftbl.lib.gui.PanelScrollBar;
import com.feed_the_beast.ftbl.lib.gui.Widget;
import com.feed_the_beast.ftbl.lib.icon.Icon;
import com.feed_the_beast.ftbl.lib.icon.PlayerHeadIcon;
import com.feed_the_beast.ftbl.lib.icon.TexturelessRectangle;
import com.feed_the_beast.ftbl.lib.util.misc.Color4I;
import com.feed_the_beast.ftbl.lib.util.misc.MouseButton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author LatvianModder
 */
public class GuiSelectTeam extends GuiBase
{
	private final Panel panelTeams;
	private final PanelScrollBar scrollTeams;

	private static class ButtonCreateTeam extends Button
	{
		private final Icon background;

		private ButtonCreateTeam()
		{
			super(0, 0, 32, 32);
			setTitle("Create a New Team");
			setIcon(GuiIcons.ADD);
			background = new TexturelessRectangle(Color4I.NONE).setLineColor(Color4I.WHITE).setRoundEdges(true);
		}

		@Override
		public void onClicked(GuiBase gui, MouseButton button)
		{
			new GuiCreateTeam().openGui();
		}

		@Override
		public void renderWidget(GuiBase gui)
		{
			int ax = getAX();
			int ay = getAY();
			background.draw(ax, ay, 32, 32, Color4I.NONE);
			getIcon(gui).draw(ax + 8, ay + 8, 16, 16, Color4I.NONE);
		}
	}

	private static class ButtonTeam extends Button
	{
		private final PublicTeamData team;
		private final Icon background;
		private static final Color4I INVITED_COLOR = Color4I.rgba(0x6620A32B);

		private ButtonTeam(PublicTeamData t)
		{
			super(0, 0, 32, 32);
			team = t;
			setTitle(team.color.getTextFormatting() + team.displayName);
			setIcon(new PlayerHeadIcon(t.ownerName));
			background = new TexturelessRectangle(team.isInvited ? INVITED_COLOR : Color4I.NONE).setLineColor(team.color.getColor()).setRoundEdges(true);
		}

		@Override
		public void onClicked(GuiBase gui, MouseButton button)
		{
			if (team.isInvited)
			{
				ClientUtils.execClientCommand("/ftb team join " + team.getName());
			}
			else
			{
				ClientUtils.execClientCommand("/ftb team request_invite " + team.getName());
			}

			gui.closeGui();
		}

		@Override
		public void renderWidget(GuiBase gui)
		{
			int ax = getAX();
			int ay = getAY();
			background.draw(ax, ay, 32, 32, Color4I.NONE);
			getIcon(gui).draw(ax + 8, ay + 8, 16, 16, Color4I.NONE);
		}

		@Override
		public void addMouseOverText(GuiBase gui, List<String> list)
		{
			list.add(getTitle(gui));
			list.add("ID: " + team.getName());

			if (!team.description.isEmpty())
			{
				list.add("");
				list.add(team.description);
			}

			list.add("");
			list.add("Click to " + (team.isInvited ? "join the team" : "request invite to this team"));
		}
	}

	public GuiSelectTeam(Collection<PublicTeamData> teams0)
	{
		super(192, 170);
		List<PublicTeamData> teams = new ArrayList<>(teams0);
		teams.sort(null);

		panelTeams = new Panel(0, 1, 168, 168)
		{
			@Override
			public void addWidgets()
			{
				add(new ButtonCreateTeam());

				for (PublicTeamData t : teams)
				{
					add(new ButtonTeam(t));
				}
			}

			@Override
			public void updateWidgetPositions()
			{
				int size = 8;
				int x = 0;

				for (Widget widget : widgets)
				{
					widget.setX(8 + x * 40);
					widget.setY(size);

					x++;

					if (x == 4)
					{
						x = 0;
						size += 40;
					}
				}

				scrollTeams.setElementSize(size);
			}
		};

		panelTeams.addFlags(Panel.FLAG_DEFAULTS);

		scrollTeams = new PanelScrollBar(168, 8, 16, 152, 0, panelTeams)
		{
			@Override
			public boolean shouldRender(GuiBase gui)
			{
				return true;
			}
		};
	}

	@Override
	public void addWidgets()
	{
		add(panelTeams);
		add(scrollTeams);
	}

	@Override
	public Icon getIcon(GuiBase gui)
	{
		return DEFAULT_BACKGROUND;
	}
}