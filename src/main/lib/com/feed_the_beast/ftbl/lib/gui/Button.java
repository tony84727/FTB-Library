package com.feed_the_beast.ftbl.lib.gui;

import com.feed_the_beast.ftbl.lib.icon.Icon;
import com.feed_the_beast.ftbl.lib.icon.TexturelessRectangle;
import com.feed_the_beast.ftbl.lib.util.misc.Color4I;
import com.feed_the_beast.ftbl.lib.util.misc.MouseButton;

public abstract class Button extends Widget
{
	public static final TexturelessRectangle DEFAULT_BACKGROUND = new TexturelessRectangle(Color4I.NONE).setLineColor(0xFFC0C0C0);
	public static final Icon DEFAULT_MOUSE_OVER = new TexturelessRectangle(Color4I.WHITE_A[33]);

	private String title = "";
	private Icon icon = Icon.EMPTY;

	public Button(int x, int y, int w, int h)
	{
		super(x, y, w, h);
	}

	public Button(int x, int y, int w, int h, String t)
	{
		this(x, y, w, h);
		setTitle(t);
	}

	@Override
	public String getTitle(GuiBase gui)
	{
		return title;
	}

	public Button setTitle(String s)
	{
		title = s;
		return this;
	}

	public Button setIcon(Icon i)
	{
		icon = i;
		return this;
	}

	@Override
	public Icon getIcon(GuiBase gui)
	{
		return icon;
	}

	@Override
	public boolean mousePressed(GuiBase gui, MouseButton button)
	{
		if (gui.isMouseOver(this))
		{
			onClicked(gui, button);
			return true;
		}

		return false;
	}

	public abstract void onClicked(GuiBase gui, MouseButton button);
}