package ifml2.om;

import static ifml2.om.xml.XmlSchemaConstants.WORDS_MAIN_WORD_ATTRIBUTE;
import static ifml2.om.xml.XmlSchemaConstants.WORDS_WORD_TAG;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlTransient;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

@XmlAccessorType(XmlAccessType.NONE)
public class WordLinks implements Cloneable {
    private final ListEventListener<Word> listEventListener = new ListEventListener<Word>() {
        @Override
        public void listChanged(ListEvent<Word> listChanges) {
            fireChangeEvent();
        }
    };

    @XmlTransient
    private final List<ChangeListener> changeListeners = new ArrayList<ChangeListener>();

    @XmlAttribute(name = WORDS_MAIN_WORD_ATTRIBUTE)
    @XmlIDREF
    private Word mainWord;

    @XmlElement(name = WORDS_WORD_TAG)
    @XmlIDREF
    private EventList<Word> words = new BasicEventList<Word>();

    {
        words.addListEventListener(listEventListener);
    }

    @Override
    public WordLinks clone() throws CloneNotSupportedException {
        WordLinks clone = (WordLinks) super.clone();
        clone.words = GlazedLists.eventList(words); // copy links
        return clone;
    }

    public Word getMainWord() {
        return mainWord;
    }

    public void setMainWord(Word mainWord) {
        this.mainWord = mainWord;
        fireChangeEvent();
    }

    public EventList<Word> getWords() {
        return words;
    }

    public void setWords(EventList<Word> words) {
        this.words = words;
        words.addListEventListener(listEventListener);
        fireChangeEvent();
    }

    public void addChangeListener(ChangeListener listener) {
        changeListeners.add(listener);
    }

    private void fireChangeEvent() {
        for (ChangeListener changeListener : changeListeners) {
            changeListener.stateChanged(new ChangeEvent(this));
        }
    }

    public String getAllWords() {
        String result = "";

        if (words != null) {
            for (Word word : getWords()) {
                result += ' ' + word.ip;
            }
        }

        return result.trim();
    }

    public void add(Word word) {
        words.add(word);
        fireChangeEvent();
    }

    public void remove(Word word) {
        words.remove(word);
    }
}
