package ifml2.engine.saved;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ifml2.om.Item;
import ifml2.om.Property;
import ifml2.om.PropertyDefinition;
import ifml2.om.PropertyDefinition.Type;
import ifml2.om.Role;
import ifml2.om.RoleDefinition;
import ifml2.om.Story;
import ifml2.vm.values.CollectionValue;
import ifml2.vm.values.Value;

public class SavedProperty {
    private static final Logger LOG = LoggerFactory.getLogger(SavedProperty.class);
    @XmlAttribute(name = "name")
    private String name;
    @XmlElement(name = "item")
    private ArrayList<String> items = new ArrayList<>();

    @SuppressWarnings("UnusedDeclaration")
    public SavedProperty() {
        // JAXB
    }

    public SavedProperty(@NotNull Property property) {
        name = property.getName();
        Value value = property.getValue();
        if (value instanceof CollectionValue) {
            ((CollectionValue) value).getValue().stream().filter(obj -> obj instanceof Item).map(obj -> (Item) obj)
                    .forEach(obj -> items.add(obj.getId()));
        } else {
            LOG.error(
                    "Системная ошибка: свойство \"{0}\" помечено как коллекция, но хранит значение другого типа - \"{1)\".",
                    name, value.getTypeName());
        }
    }

    public void restore(@NotNull Role role, @NotNull Story.DataHelper dataHelper) {
        Property property = role.findPropertyByName(name);
        if (property != null) {
            RoleDefinition roleDefinition = role.getRoleDefinition();
            PropertyDefinition propertyDefinition = roleDefinition.findPropertyDefinitionByName(name);
            if (propertyDefinition != null) {
                // restore only collections
                if (Type.COLLECTION.equals(propertyDefinition.getType())) {
                    List<Item> propItems = new ArrayList<Item>();
                    for (String itemId : items) {
                        Item propItem = dataHelper.findItemById(itemId);
                        if (propItem != null) {
                            propItem.moveTo(propItems);
                        } else {
                            LOG.warn("[Game loading] Location items loading: there is no item with id \"{0}\".",
                                    itemId);
                        }
                    }
                    property.setValue(new CollectionValue(propItems));
                } else {
                    LOG.error(
                            "Системная ошибка: свойство \"{0}\" в роли \"{1}\""
                                    + "не помечено как коллекция, но сохранено в сохранённой игре как коллекция.",
                            name, role.getName());
                }
            } else {
                LOG.error("Системная ошибка: в роли {0} не найдено свойство {1}.", role, name);
            }
        } else {
            LOG.warn("Не найдено свойство по имени \"{0}\" в роли \"{1}\".", name, role.getName());
        }
    }
}
