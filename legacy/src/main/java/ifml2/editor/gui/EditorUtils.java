package ifml2.editor.gui;

import java.awt.Dialog;
import java.awt.Window;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.text.MessageFormat;

import javax.swing.JOptionPane;

import org.jetbrains.annotations.NotNull;

import ifml2.GUIUtils;
import ifml2.editor.IFML2EditorException;
import ifml2.editor.gui.instructions.AbstractInstrEditor;
import ifml2.om.Story;
import ifml2.vm.instructions.Instruction;

public class EditorUtils {
    public static Instruction.Type askInstructionType(Dialog owner) {
        return (Instruction.Type) JOptionPane.showInputDialog(owner, "Выберите тип инструкции", "Новая инструкция",
                JOptionPane.QUESTION_MESSAGE, null, Instruction.Type.values(), Instruction.Type.SHOW_MESSAGE);
    }

    /**
     * Shows editor associated for given instruction and update it if closed by Ok
     * button.
     *
     * @param owner
     *            Dialog-owner of showing editor
     * @param instruction
     *            Instruction to edit
     * @param storyDataHelper
     *            Story.DataHelper for acquiring additional data
     * @return true if Ok button was pressed and instruction was updated and false
     *         in other case
     */
    public static boolean showAssociatedEditor(Window owner, @NotNull Instruction instruction,
            Story.DataHelper storyDataHelper) {
        try {
            Instruction.Type instrType = Instruction.Type.getItemByClass(instruction.getClass());
            Class<? extends Instruction> instrClass = instrType.getInstrClass();
            Class<? extends AbstractInstrEditor> editorClass = instrType.getAssociatedEditor();
            if (editorClass != null) {
                for (Constructor<?> constructor : editorClass.getConstructors()) {
                    AbstractInstrEditor instrEditor = null;
                    Type[] parameterTypes = constructor.getGenericParameterTypes();
                    try {
                        int length = parameterTypes.length;
                        if (length >= 2 && parameterTypes[0].equals(Window.class)
                                && parameterTypes[1].equals(instrClass)) {
                            if (length == 2) {
                                instrEditor = (AbstractInstrEditor) constructor.newInstance(owner, instruction);
                            } else if (length == 3 && parameterTypes[2].equals(Story.DataHelper.class)) {
                                instrEditor = (AbstractInstrEditor) constructor.newInstance(owner, instruction,
                                        storyDataHelper);
                            }
                        }
                    } catch (InvocationTargetException e) {
                        throw e.getTargetException();
                    }

                    if (instrEditor != null) {
                        if (instrEditor.showDialog()) {
                            instrEditor.getInstruction(instruction);
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
                throw new IFML2EditorException("Для инструкции {0} не найден подходящий редактор (конструктор)",
                        instrType.getTitle());
            } else {
                JOptionPane.showMessageDialog(owner,
                        MessageFormat.format("Инструкция \"{0}\" пока не редактируется", instrType.getTitle()));
            }
        } catch (Throwable e) {
            GUIUtils.showErrorMessage(owner, e);
        }

        return false;
    }
}
