package ifml2.om;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;
import java.text.MessageFormat;

public class PropertyDefinition {
    @XmlAttribute(name = "description")
    public String description;
    @XmlAttribute(name = "name")
    private String name;
    @XmlAttribute(name = "value")
    private String value;
    @XmlAttribute(name = "type")
    private Type type;

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return MessageFormat.format("Определение свойства \"{0}\" типа {1}", name, type);
    }

    @XmlEnum
    @XmlType(namespace = "PropertyDefinition")
    public enum Type {
        @XmlEnumValue(value = "text")
        TEXT,
        @XmlEnumValue(value = "number")
        NUMBER,
        @XmlEnumValue(value = "logic")
        LOGIC,
        @XmlEnumValue(value = "collection")
        COLLECTION
    }
}
