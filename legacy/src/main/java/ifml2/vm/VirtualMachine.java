package ifml2.vm;

import static ifml2.om.Procedure.SystemProcedureType.SHOW_LOCATION;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ifml2.IFML2Exception;
import ifml2.engine.Engine;
import ifml2.om.Action;
import ifml2.om.Hook;
import ifml2.om.IFMLObject;
import ifml2.om.InstructionList;
import ifml2.om.Item;
import ifml2.om.Location;
import ifml2.om.Procedure;
import ifml2.om.Procedure.SystemProcedureType;
import ifml2.om.Story;
import ifml2.om.Trigger;
import ifml2.vm.instructions.Instruction;
import ifml2.vm.values.BooleanValue;
import ifml2.vm.values.EmptyValue;
import ifml2.vm.values.Value;

public class VirtualMachine {
    private final Map<String, Value> systemConstants = new HashMap<String, Value>() {
        {
            put(BooleanValue.TRUE, new BooleanValue(true));
            put(BooleanValue.FALSE, new BooleanValue(false));
            put(EmptyValue.LITERAL, new EmptyValue());
        }
    };
    private Engine engine;
    private final Map<SystemProcedureType, Procedure> inheritedSystemProcedures = new HashMap<SystemProcedureType, Procedure>() {
        @Override
        public Procedure get(Object key) {
            // lazy initialization
            if (!containsKey(key)) {
                Procedure inheritor = engine.getStory().getSystemInheritorProcedure((SystemProcedureType) key);
                put((SystemProcedureType) key, inheritor);
                return inheritor;
            } else {
                return super.get(key);
            }
        }
    };

    public Engine getEngine() {
        return engine;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    public void runAction(@NotNull Action action, List<Variable> parameters) throws IFML2Exception {
        runProcedure(action.getProcedureCall().getProcedure(), parameters);
    }

    public void runProcedureWithoutParameters(@NotNull Procedure procedure) throws IFML2Exception {
        try {
            RunningContext runningContext = RunningContext.CreateCallContext(this, procedure, null);
            runInstructionList(procedure.getProcedureBody(), runningContext);
        } catch (IFML2VMException e) {
            throw new IFML2VMException(e, "{0}\n  в процедуре \"{1}\"", e.getMessage(), procedure.getName());
        }
    }

    public Value callProcedureWithParameters(@NotNull Procedure procedure, List<Variable> parameters)
            throws IFML2Exception {
        try {
            RunningContext runningContext = RunningContext.CreateCallContext(this, procedure, parameters);
            runInstructionList(procedure.getProcedureBody(), runningContext);
            return runningContext.getReturnValue();
        } catch (IFML2VMException e) {
            throw new IFML2VMException(e, "{0}\n  в процедуре \"{1}\"", e.getMessage(), procedure.getName());
        }
    }

    void runProcedure(@NotNull Procedure procedure, List<Variable> parameters) throws IFML2Exception {
        try {
            RunningContext runningContext = RunningContext.CreateCallContext(this, procedure, parameters);
            runInstructionList(procedure.getProcedureBody(), runningContext);
        } catch (IFML2VMException e) {
            throw new IFML2VMException(e, "{0}\n  в процедуре \"{1}\"", e.getMessage(), procedure.getName());
        }
    }

    public void runHook(@NotNull Hook hook, List<Variable> parameters) throws IFML2Exception {
        RunningContext runningContext = RunningContext.CreateNewContext(this);
        runningContext.populateParameters(parameters);
        runInstructionList(hook.getInstructionList(), runningContext);
    }

    public void runInstructionList(@NotNull InstructionList instructionList, @NotNull RunningContext runningContext)
            throws IFML2Exception {
        for (Instruction instruction : instructionList.getInstructions()) {
            instruction.virtualMachine = this;
            try {
                instruction.run(runningContext);
            } catch (IFML2VMException e) {
                throw new IFML2VMException(e, "{0}\n  в инструкции #{1} ({2})", e.getMessage(),
                        instructionList.getInstructions().indexOf(instruction) + 1, instruction.toString());
            }
        }
    }

    public void showLocation(@Nullable Location location) throws IFML2Exception {
        if (location == null) {
            return;
        }

        // check if inherited
        Procedure inheritor = inheritedSystemProcedures.get(SHOW_LOCATION);

        if (inheritor != null) {
            // inherited! run inheritor
            runProcedureWithoutParameters(inheritor);
        } else {
            // not inherited! do as usual...
            outTextLn(location.getName());
            outTextLn(location.getDescription());
            if (location.getItems().size() > 0) {
                String objectsList = convertObjectsToString(location.getItems());
                outTextLn("А также тут {0}", objectsList);
            }
        }
    }

    private String convertObjectsToString(List<Item> inventory) {
        StringBuilder result = new StringBuilder();

        Iterator<Item> iterator = inventory.iterator();
        while (iterator.hasNext()) {
            String itemName = iterator.next().getName();

            if (result.length() == 0) { // it's the first word
                result.append(itemName);
            } else  {
                result.append(iterator.hasNext() ? ", " : " и ").append(itemName);
            }
        }
        result.append(".");

        return result.toString();
    }

    public Value resolveSymbol(String symbol) throws IFML2VMException {
        return systemConstants.getOrDefault(symbol.toLowerCase(), engine.resolveSymbol(symbol));
    }

    public Value runTrigger(Trigger trigger, IFMLObject ifmlObject) throws IFML2Exception {
        RunningContext runningContext = RunningContext.CreateNewContext(this);
        runningContext.setDefaultObject(ifmlObject);

        runInstructionList(trigger.getInstructions(), runningContext);

        return runningContext.getReturnValue();
    }

    public void setCurrentLocation(Location location) {
        engine.setCurrentLocation(location);
    }

    public Story getStory() {
        return engine.getStory();
    }

    public void outTextLn(String text, Object... args) {
        engine.outTextLn(text, args);
    }

    public void outText(String text) {
        engine.outText(text);
    }

    public Variable searchGlobalVariable(String name) {
        return engine.searchGlobalVariable(name);
    }

    /**
     * Initializes (resets) virtual machine.
     */
    public void init() {
        inheritedSystemProcedures.clear();
    }

    public void outPicture(String filePath, int maxHeight, int maxWidth) {
        engine.outIcon(filePath, maxHeight, maxWidth);
    }
}
