package ifml2.om;

import ifml2.IFML2Exception;
import ifml2.vm.RunningContext;
import ifml2.vm.values.CollectionValue;
import ifml2.vm.values.ObjectValue;
import ifml2.vm.values.Value;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@XmlTransient
public class Location extends IFMLObject implements Cloneable
{
    protected HashMap<ExitDirection, Location> exits = new HashMap<ExitDirection, Location>();

    protected List<Item> items = new ArrayList<Item>();

    public static String getClassName()
    {
        return "Локация";
    }

    @Override
    public Location clone() throws CloneNotSupportedException
    {
        Location location = (Location) super.clone();
        copyFieldsTo(location);
        return location;
    }

    private void copyFieldsTo(Location location)
    {
        location.exits = new HashMap<ExitDirection, Location>(exits);
        location.items = new ArrayList<Item>(items);
    }

    public Location getExit(ExitDirection exitDirection)
    {
        return exits.get(exitDirection);
    }

    public void setExit(ExitDirection exitDirection, Location location)
    {
        exits.put(exitDirection, location);
    }

    public Location getNorth()
    {
        return getExit(ExitDirection.NORTH);
    }

    public void setNorth(Location north)
    {
        setExit(ExitDirection.NORTH, north);
    }

    public Location getEast()
    {
        return getExit(ExitDirection.EAST);
    }

    public void setEast(Location east)
    {
        setExit(ExitDirection.EAST, east);
    }

    public Location getSouth()
    {
        return getExit(ExitDirection.SOUTH);
    }

    public void setSouth(Location south)
    {
        setExit(ExitDirection.SOUTH, south);
    }

    public Location getWest()
    {
        return getExit(ExitDirection.WEST);
    }

    public void setWest(Location west)
    {
        setExit(ExitDirection.WEST, west);
    }

    public List<Item> getItems()
    {
        return items;
    }

    //@XmlTransient // is loaded in OMManager through item.location
    public void setItems(List<Item> items)
    {
        this.items = items;
    }

    /**
     * Checks whether location contains item.
     *
     * @param item item to check
     * @return True if location contains item and false otherwise.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean contains(Item item)
    {
        return items.contains(item);
    }

    @Override
    public Value getMemberValue(String propertyName, RunningContext runningContext) throws IFML2Exception
    {
        if ("север".equalsIgnoreCase(propertyName))
        {
            return new ObjectValue(getNorth());
        }
        else if ("восток".equalsIgnoreCase(propertyName))
        {
            return new ObjectValue(getEast());
        }
        else if ("юг".equalsIgnoreCase(propertyName))
        {
            return new ObjectValue(getSouth());
        }
        else if ("запад".equalsIgnoreCase(propertyName))
        {
            return new ObjectValue(getWest());
        }
        else if ("верх".equalsIgnoreCase(propertyName))
        {
            return new ObjectValue(getUp());
        }
        else if ("низ".equalsIgnoreCase(propertyName))
        {
            return new ObjectValue(getDown());
        }
        else if ("предметы".equalsIgnoreCase(propertyName))
        {
            return new CollectionValue(items);
        }
        else
        {
            return super.getMemberValue(propertyName, runningContext);
        }
    }

    public Location getDown()
    {
        return getExit(ExitDirection.DOWN);
    }

    public void setDown(Location down)
    {
        setExit(ExitDirection.DOWN, down);
    }

    public Location getUp()
    {
        return getExit(ExitDirection.UP);
    }

    public void setUp(Location up)
    {
        setExit(ExitDirection.UP, up);
    }

    public void copyTo(@NotNull Location location) throws CloneNotSupportedException
    {
        super.copyTo(location);
        copyFieldsTo(location);
    }
}
