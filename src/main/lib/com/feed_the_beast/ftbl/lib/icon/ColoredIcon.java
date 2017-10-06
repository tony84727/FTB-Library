package com.feed_the_beast.ftbl.lib.icon;

import com.feed_the_beast.ftbl.lib.gui.GuiHelper;
import com.feed_the_beast.ftbl.lib.util.misc.Color4I;
import com.google.gson.JsonObject;
import net.minecraft.client.renderer.GlStateManager;

/**
 * @author LatvianModder
 */
public class ColoredIcon extends Icon
{
	public final Icon parent;
	public final Color4I color;

	public ColoredIcon(Icon p, Color4I c)
	{
		parent = p;
		color = c;
	}

	public ColoredIcon(Icon p, int c)
	{
		this(p, Color4I.rgba(c).mutable());
	}

	@Override
	public void draw(int x, int y, int w, int h, Color4I col)
	{
		Color4I col1 = col.hasColor() ? col : color;

		if (parent.isEmpty())
		{
			GuiHelper.drawBlankRect(x, y, w, h, col1);
		}
		else
		{
			parent.draw(x, y, w, h, col1);
		}

		GlStateManager.color(1F, 1F, 1F, 1F);
	}

	@Override
	public JsonObject getJson()
	{
		JsonObject o = new JsonObject();
		o.addProperty("id", "colored");

		if (color.hasColor())
		{
			o.add("color", color.toJson());
		}

		o.add("parent", parent.getJson());
		return o;
	}
}