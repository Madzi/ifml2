package ifml2.editor.gui.instructions;

import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jetbrains.annotations.NotNull;

import ifml2.editor.DataNotValidException;
import ifml2.editor.IFML2EditorException;
import ifml2.vm.instructions.Instruction;
import ifml2.vm.instructions.SetVarInstruction;

public class SetVarInstrEditor extends AbstractInstrEditor {
    private static final String SET_VAR_EDITOR_TITLE = Instruction.getTitleFor(SetVarInstruction.class);
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField nameText;
    private JTextField valueText;

    public SetVarInstrEditor(Window owner, SetVarInstruction instruction) {
        super(owner);
        initializeEditor(SET_VAR_EDITOR_TITLE, contentPane, buttonOK, buttonCancel);

        /*
         * nameText.getDocument().addDocumentListener(new DocumentListener() {
         * 
         * @Override public void insertUpdate(DocumentEvent e) { updateScope(); }
         * 
         * @Override public void removeUpdate(DocumentEvent e) { updateScope(); }
         * 
         * @Override public void changedUpdate(DocumentEvent e) {
         *//* do nothing *//*
                            * } });
                            */

        // -- init form data --

        nameText.setText(instruction.getName());
        valueText.setText(instruction.getValue());
    }

    /*
     * private void updateScope() { Variable.VariableScope variableScope =
     * runningContext.getVariableScopeByName(nameText.getText().trim());
     * typeLabel.setText(variableScope != null ? variableScope.toString() :
     * "локальная"); }
     */

    @Override
    protected Class<? extends Instruction> getInstrClass() {
        return SetVarInstruction.class;
    }

    @Override
    public void getInstruction(@NotNull Instruction instruction) throws IFML2EditorException {
        updateData(instruction);

        SetVarInstruction setVarInstruction = (SetVarInstruction) instruction;
        setVarInstruction.setName(nameText.getText().trim());
        setVarInstruction.setValue(valueText.getText().trim());
    }

    @Override
    protected void validateData() throws DataNotValidException {
        // check name
        if (nameText.getText().trim().length() == 0) {
            throw new DataNotValidException("Должно быть задано имя переменной.", nameText);
        }

        // check value
        if (valueText.getText().trim().length() == 0) {
            throw new DataNotValidException("Должно быть задано значение переменной.", valueText);
        }
    }
}
