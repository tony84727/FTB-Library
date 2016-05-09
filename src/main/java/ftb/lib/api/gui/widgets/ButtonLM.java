package ftb.lib.api.gui.widgets;

import ftb.lib.api.IClickable;
import ftb.lib.api.MouseButton;
import ftb.lib.api.gui.IGuiLM;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class ButtonLM extends WidgetLM implements IClickable
{
	private long lastClickMillis;
	
	public ButtonLM(IGuiLM g, int x, int y, int w, int h)
	{
		super(g, x, y, w, h);
		lastClickMillis = System.currentTimeMillis();
	}
	
	public boolean isDoubleClickRequired()
	{ return false; }
	
	@Override
	public void mousePressed(int b)
	{
		if(mouseOver())
		{
			if(isDoubleClickRequired())
			{
				long l = System.currentTimeMillis();
				if(l - lastClickMillis < 300) { onClicked(MouseButton.get(b)); }
				lastClickMillis = l;
			}
			
			else { onClicked(MouseButton.get(b)); }
		}
	}
}