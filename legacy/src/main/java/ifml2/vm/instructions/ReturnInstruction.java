package ifml2.vm.instructions;

import java.text.MessageFormat;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import ifml2.IFML2Exception;
import ifml2.vm.ExpressionCalculator;
import ifml2.vm.RunningContext;
import ifml2.vm.values.Value;

@XmlRootElement(name = "return")
@XmlAccessorType(XmlAccessType.NONE)
@IFML2Instruction(title = "Вернуть значение")
public class ReturnInstruction extends Instruction {
    @XmlAttribute(name = "value")
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public void run(RunningContext runningContext) throws IFML2Exception {
        Value returnValue = ExpressionCalculator.calculate(runningContext, value);
        runningContext.setReturnValue(returnValue);
    }

    @Override
    public String toString() {
        return MessageFormat.format("Вернуть значение выражения \"{0}\"", value);
    }
}
