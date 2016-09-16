package com.feed_the_beast.ftbl.api.config.impl;

import com.feed_the_beast.ftbl.api.config.IConfigKey;
import com.feed_the_beast.ftbl.api.config.IConfigTree;
import com.feed_the_beast.ftbl.api.config.IConfigValue;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import net.minecraft.nbt.NBTBase;

import javax.annotation.Nullable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Created by LatvianModder on 12.09.2016.
 */
public enum EmptyConfigTree implements IConfigTree
{
    INSTANCE;

    @Override
    public Map<IConfigKey, IConfigValue> getTree()
    {
        return Collections.emptyMap();
    }

    @Override
    public void add(IConfigKey key, IConfigValue value)
    {
    }

    @Override
    public void add(IConfigKey key)
    {
    }

    @Override
    public boolean has(IConfigKey key)
    {
        return false;
    }

    @Override
    public void remove(IConfigKey key)
    {
    }

    @Override
    public IConfigValue get(IConfigKey key)
    {
        return PropertyNull.INSTANCE;
    }

    @Override
    public boolean isEmpty()
    {
        return true;
    }

    @Override
    public void addAll(String id, Class<?> clazz, @Nullable Object obj)
    {
    }

    @Override
    public void writeData(DataOutput data, boolean extended) throws IOException
    {
    }

    @Override
    public void readData(DataInput data, boolean extended) throws IOException
    {
    }

    @Override
    public void fromJson(JsonElement json)
    {
    }

    @Override
    public JsonElement getSerializableElement()
    {
        return JsonNull.INSTANCE;
    }

    @Override
    public NBTBase serializeNBT()
    {
        return null;
    }

    @Override
    public void deserializeNBT(NBTBase nbt)
    {
    }
}