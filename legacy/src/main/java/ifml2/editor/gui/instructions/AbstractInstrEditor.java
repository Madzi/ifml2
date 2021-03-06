package ifml2.editor.gui.instructions;

import java.awt.Window;
import java.text.MessageFormat;

import org.jetbrains.annotations.NotNull;

import ifml2.editor.IFML2EditorException;
import ifml2.editor.gui.AbstractEditor;
import ifml2.vm.instructions.Instruction;

/**
 * Common ancestor for Instruction editors. Auto reacts to OK, Cancel and X
 * buttons clicks. To use it implement get methods to tune editor and call
 * super() and init() in constructor.
 */
public abstract class AbstractInstrEditor extends AbstractEditor<Instruction> {
    public AbstractInstrEditor(Window owner) {
        super(owner);
    }

    protected abstract Class<? extends Instruction> getInstrClass();

    public abstract void getInstruction(@NotNull Instruction instruction) throws IFML2EditorException;

    @Override
    public void updateData(@NotNull Instruction data) throws IFML2EditorException {
        if (!data.getClass().equals(getInstrClass())) {
            throw new IFML2EditorException(MessageFormat.format("Instruction should be of class {0}", getInstrClass()));
        }
    }
}
