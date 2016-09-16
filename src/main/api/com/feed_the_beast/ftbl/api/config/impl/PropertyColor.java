package com.feed_the_beast.ftbl.api.config.impl;

import com.feed_the_beast.ftbl.FTBLibFinals;
import com.feed_the_beast.ftbl.api.config.IConfigKey;
import com.feed_the_beast.ftbl.api.config.IConfigValue;
import com.feed_the_beast.ftbl.api.config.IGuiEditConfig;
import com.feed_the_beast.ftbl.api.gui.IMouseButton;
import com.feed_the_beast.ftbl.gui.GuiSelectColor;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Created by LatvianModder on 26.08.2016.
 */
public class PropertyColor extends PropertyBase
{
    public static final ResourceLocation ID = new ResourceLocation(FTBLibFinals.MOD_ID, "color");

    private byte value;

    public PropertyColor(byte v)
    {
        value = v;
    }

    @Override
    public ResourceLocation getID()
    {
        return ID;
    }

    @Nullable
    @Override
    public Object getValue()
    {
        return getColorID();
    }

    public void set(byte v)
    {
        value = v;
    }

    public byte getColorID()
    {
        return value;
    }

    @Override
    public void writeData(DataOutput data, boolean extended) throws IOException
    {
        data.writeByte(getColorID());
    }

    @Override
    public void readData(DataInput data, boolean extended) throws IOException
    {
        set(data.readByte());
    }

    @Override
    public String getString()
    {
        return Integer.toString(getColorID());
    }

    @Override
    public boolean getBoolean()
    {
        return getColorID() != 0;
    }

    @Override
    public int getInt()
    {
        return getColorID();
    }

    @Override
    public IConfigValue copy()
    {
        return new PropertyColor(getColorID());
    }

    @Override
    public int getColor()
    {
        return 0xAA5AE8;
    }

    @Override
    public void onClicked(IGuiEditConfig gui, IConfigKey key, IMouseButton button)
    {
        new GuiSelectColor(null, getColorID(), (id, val) ->
        {
            set((Byte) val);
            gui.onChanged(key, getSerializableElement());
            gui.openGui();
        }).openGui();
    }

    @Override
    public NBTBase serializeNBT()
    {
        return new NBTTagByte(getColorID());
    }

    @Override
    public void deserializeNBT(NBTBase nbt)
    {
        set(((NBTPrimitive) nbt).getByte());
    }

    @Override
    public void fromJson(JsonElement json)
    {
        set(json.getAsByte());
    }

    @Override
    public JsonElement getSerializableElement()
    {
        return new JsonPrimitive(getColorID());
    }
}