package ifml2.parser;

import ifml2.CommonUtils;
import ifml2.IFML2Exception;
import ifml2.engine.Engine;
import ifml2.om.*;

import java.text.MessageFormat;
import java.util.*;

public class Parser
{
    private Story story = null;
    private Engine engine = null;

    public Parser(Engine engine)
    {
        this.engine = engine;
    }

    public void setStory(Story story)
    {
        this.story = story;
    }

    public ParseResult parse(String phrase) throws IFML2Exception
    {
        if (story == null)
        {
            throw new IFML2Exception("Системная ошибка: Ссылка на объектную модель (story) не задана!");
        }

        if (story.getAllActions() == null || story.getAllActions().size() == 0)
        {
            throw new IFML2Exception("Системная ошибка: В системе нет ни одного действия.");
        }

        // split phrase to array
        ArrayList<String> phraseAsList = new ArrayList<String>(Arrays.asList(phrase.split("\\s+")));

        // prepare list of all fitted templates
        ArrayList<FittedTemplate> fittedTemplates = new ArrayList<FittedTemplate>();

        IFML2Exception lastException = null;

        for (Action action : story.getAllActions())
        {
            for (Template template : action.getTemplates())
            {
                int templateSize = template.getSize();
                try
                {
                    ArrayList<FittedFormalElement> fittedFormalElements = fitPhraseWithTemplate(phraseAsList, template.getElements());

                    FittedTemplate fittedTemplate = new FittedTemplate(action, fittedFormalElements);
                    fittedTemplates.add(fittedTemplate);
                }
                catch (IFML2ParsePhraseTooLong e)
                {
                    if (lastException == null ||
                        (lastException instanceof IFML2ParseException && e.isMoreFull((IFML2ParseException) lastException, templateSize)))
                    {
                        lastException = new IFML2ParseException(MessageFormat
                                                                        .format("Я бы понял, если бы вы сказали \"{0}\", но я не понял вот эту часть фразы: \"{1}\".",
                                                                                convertFittedToString(e.getFittedFormalElements()),
                                                                                convertArrayToString(e.getPhraseRest())),
                                                                e.getUsedWords(), templateSize);
                    }
                }
                catch (IFML2ParseException e)
                {
                    if (lastException == null)
                    {
                        e.setTemplateSize(templateSize);
                        lastException = e;
                    }
                    else if (lastException instanceof IFML2ParseException)
                    {
                        e.setTemplateSize(templateSize);
                        if (e.isMoreFull((IFML2ParseException) lastException, templateSize))
                        {
                            lastException = e;
                        }
                        else if (e.isEquallyFull((IFML2ParseException) lastException, templateSize))
                        {
                            lastException = Math.round(Math.random()) == 0 ? lastException : e; // randomly take one of error :)
                        }
                    }
                }
                catch (IFML2Exception e)
                {
                    lastException = e;
                }
            }
        }

        if (fittedTemplates.size() == 0)
        {
            throw lastException;
        }

        // clean fitted templates from templates with inaccessible objects
        ArrayList<FittedTemplate> accessibleTemplates = null;
        try
        {
            accessibleTemplates = removeInaccessibleObjects(fittedTemplates);
        }
        catch (IFML2Exception e)
        {
            lastException = e; // always rewrite last exception because inaccessible objects are more important for errors
        }

        if (accessibleTemplates == null || accessibleTemplates.size() == 0)
        {
            throw lastException;
        }

        // take the first of fitted templates
        FittedTemplate firstFittedTemplate = accessibleTemplates.get(0);

        // test fitted template on variance of objects
        for (FittedFormalElement fittedFormalElement : firstFittedTemplate.fittedFormalElements)
        {
            if (fittedFormalElement instanceof FittedObjects)
            {
                List<IFMLObject> objects = ((FittedObjects) fittedFormalElement).objects;
                if (objects.size() > 1)
                {
                    throw new IFML2ParseException("Не понятно, что за " + objects.get(0).getWordLinks().getMainWord() + " имеется в виду.",
                                                  phraseAsList.size());
                }
            }
        }

        ArrayList<FormalElement> formalElements = new ArrayList<FormalElement>();
        for (FittedFormalElement fittedFormalElement : firstFittedTemplate.fittedFormalElements)
        {
            FormalElement formalElement;
            if (fittedFormalElement instanceof FittedSynonym)
            {
                FittedSynonym fittedSynonym = (FittedSynonym) fittedFormalElement;
                formalElement = new FormalElement(fittedSynonym.synonym, fittedSynonym.parameter);
            }
            else if (fittedFormalElement instanceof FittedObjects)
            {
                FittedObjects fittedObjects = (FittedObjects) fittedFormalElement;
                formalElement = new FormalElement(fittedObjects.objects.get(0), fittedObjects.parameter);
            }
            else
            {
                throw new IFML2Exception(
                        "Системная ошибка: ПодходящийФомральныйЭлемент имеет неизвестный тип\n" + "Фраза: " + phrase + "\n" + "Шаблон: " +
                        firstFittedTemplate.fittedFormalElements + ".");
            }

            formalElements.add(formalElement);
        }

        return new ParseResult(firstFittedTemplate.action, formalElements);
    }

    private String convertArrayToString(ArrayList<String> stringArrayList)
    {
        String result = "";
        for (String element : stringArrayList)
        {
            if (result.length() > 0)
            {
                result += " ";
            }
            result += element;
        }
        return result;
    }

    private String convertFittedToString(ArrayList<FittedFormalElement> fittedFormalElements) throws IFML2Exception
    {
        String result = "";

        for (FittedFormalElement fittedFormalElement : fittedFormalElements)
        {
            String element = "";
            if (fittedFormalElement instanceof FittedSynonym)
            {
                element = fittedFormalElement.toString();
            }
            else if (fittedFormalElement instanceof FittedObjects)
            {
                ArrayList<IFMLObject> fittedObjects = ((FittedObjects) fittedFormalElement).objects;

                if (fittedObjects.size() > 0)
                {
                    element = fittedObjects.get(0).getName(((FittedObjects) fittedFormalElement).gramCase);
                }
                else
                {
                    throw new IFML2Exception("Системная ошибка: в FittedObjects кол-во объектов = 0\n" +
                                             "fittedFormalElements = " + fittedFormalElements + '\n' +
                                             "fittedFormalElement = " + fittedFormalElement);
                }
            }
            result += " " + element;
        }

        return result.trim();
    }

    private ArrayList<FittedTemplate> removeInaccessibleObjects(ArrayList<FittedTemplate> fittedTemplates) throws IFML2Exception
    {
        ArrayList<FittedTemplate> result = new ArrayList<FittedTemplate>();
        IFMLObject inaccessibleObject = null;

        for (FittedTemplate fittedTemplate : fittedTemplates)
        {
            Boolean toAddTemplate = true;

            for (FittedFormalElement fittedFormalElement : fittedTemplate.getFittedFormalElements())
            {
                if (fittedFormalElement instanceof FittedObjects)
                {
                    ArrayList<IFMLObject> objectsToRemove = new ArrayList<IFMLObject>();

                    ArrayList<IFMLObject> fittedObjects = ((FittedObjects) fittedFormalElement).getObjects();

                    for (IFMLObject object : fittedObjects)
                    {
                        if (!engine.isObjectAccessible(object))
                        {
                            objectsToRemove.add(object);

                            if (inaccessibleObject == null)
                            {
                                inaccessibleObject = object;
                            }
                        }
                    }

                    fittedObjects.removeAll(objectsToRemove);

                    if (fittedObjects.size() == 0)
                    {
                        toAddTemplate = false;
                    }
                }
            }

            if (toAddTemplate)
            {
                result.add(fittedTemplate);
            }
        }

        if (result.size() > 0)
        {
            return result;
        }
        else
        {
            if (inaccessibleObject != null)
            {
                throw new IFML2ParseException("Не вижу здесь " + inaccessibleObject.getName(Word.GramCaseEnum.RP) + ".");
            }
            else
            {
                throw new IFML2Exception(
                        "Системная ошибка: inaccessibleObject = null в Parser.removeInaccessibleObjects() при result.size() = 0.");
            }
            // it doesn't require word count because it's outstanding exception
        }
    }

    private ArrayList<FittedFormalElement> fitPhraseWithTemplate(ArrayList<String> phraseAsList, List<TemplateElement> template) throws IFML2Exception
    {
        // get vars into local copy
        ArrayList<String> phraseRest = new ArrayList<String>(phraseAsList);
        ArrayList<TemplateElement> templateRest = new ArrayList<TemplateElement>(template);

        // take the first element of template
        TemplateElement firstTemplateElement = templateRest.get(0);

        // try to fit template element with beginning of phrase
        TemplateElementFitResult result = fitTemplateElementWithPhrase(firstTemplateElement, phraseRest);

        ArrayList<FittedFormalElement> fittedFormalElements = new ArrayList<FittedFormalElement>();

        fittedFormalElements.add(result.fittedFormalElement);

        // cut template and phrase
        templateRest.remove(0);
        for (int i = 1; i <= result.usedWordsQty; i++)
        {
            phraseRest.remove(0);
        }

        if (templateRest.size() == 0 && phraseRest.size() == 0)
        {
            return fittedFormalElements;
        }
        else if (templateRest.size() > 0 && phraseRest.size() == 0)
        {
            throw new IFML2ParseException(makeQuestionsForTemplate(templateRest) + " (пишите ответ полностью)", result.usedWordsQty);
        }
        else if (templateRest.size() == 0 && phraseRest.size() > 0)
        {
            throw new IFML2ParsePhraseTooLong(fittedFormalElements, phraseRest, result.usedWordsQty);
        }
        else
        {
            try
            {
                ArrayList<FittedFormalElement> nextElements = fitPhraseWithTemplate(phraseRest, templateRest);
                fittedFormalElements.addAll(nextElements);
                return fittedFormalElements;
            }
            catch (IFML2ParsePhraseTooLong e)
            {
                e.getFittedFormalElements().add(0, result.fittedFormalElement);
                e.setUsedWords(e.getUsedWords() + result.usedWordsQty);
                throw e;
            }
            catch (IFML2ParseException e)
            {
                e.setUsedWords(e.getUsedWords() + result.usedWordsQty);
                throw e;
            }
        }
    }

    private TemplateElementFitResult fitTemplateElementWithPhrase(TemplateElement templateElement, ArrayList<String> phrase) throws IFML2Exception
    {
        IFML2Exception lastException = null;

        if (templateElement instanceof LiteralTemplateElement)
        {
            HashMap<String, Integer> fittedSynonyms = new HashMap<String, Integer>();
            for (String synonym : ((LiteralTemplateElement) templateElement).getSynonyms())
            {
                try
                {
                    // add fitted synonym or ...
                    fittedSynonyms.put(synonym, fitSynonymWithPhrase(synonym, phrase));
                }
                catch (IFML2ParseException e)
                {
                    // ... catch exception
                    if (lastException == null)
                    {
                        lastException = e;
                    }
                    else
                    {
                        int usedWords = ((IFML2ParseException) lastException).getUsedWords();
                        if (e.getUsedWords() > usedWords)
                        {
                            lastException = e;
                        }
                        else if (e.getUsedWords() == usedWords)
                        {
                            // take random of these exceptions :)
                            lastException = Math.round(Math.random()) == 0 ? lastException : e;
                        }
                    }
                }
            }

            // if there are fitted synonyms
            if(fittedSynonyms.size() > 0)
            {
                Map.Entry<String, Integer> maxEntry = Collections.max(fittedSynonyms.entrySet(), new Comparator<Map.Entry<String, Integer>>()
                {
                    @Override
                    public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2)
                    {
                        return o1.getValue() - o2.getValue();
                    }
                });
                return new TemplateElementFitResult(new FittedSynonym(maxEntry.getKey(), templateElement.getParameter()),
                        maxEntry.getValue());
            }

            // if there are no fitted synonyms
            if (lastException != null)
            {
                throw lastException;
            }
            else
            {
                throw new IFML2Exception(
                        "Системная ошибка: при сопоставлении ЭлементаШаблона с началом фразы проверка синонимов ни к чему не привел.");
            }
        }

        else if (templateElement instanceof ObjectTemplateElement)
        {
            Word.GramCaseEnum gramCase = ((ObjectTemplateElement) templateElement).getGramCase();

            FitObjectWithPhraseResult fitObjectWithPhraseResult = fitObjectWithPhrase(gramCase, phrase);
            ArrayList<IFMLObject> objects = fitObjectWithPhraseResult.getObjects();
            int usedWordsQty = fitObjectWithPhraseResult.getUsedWordsQty();

            return new TemplateElementFitResult(new FittedObjects(objects, gramCase, templateElement.getParameter()), usedWordsQty);
        }
        else
        {
            throw new IFML2Exception("Системная ошибка: ЭлементШаблона неизвестного типа.");
        }
    }

    private FitObjectWithPhraseResult fitObjectWithPhrase(Word.GramCaseEnum gramCase, ArrayList<String> phrase) throws IFML2Exception
    {
        List<String> restPhrase = new ArrayList<String>(phrase);
        ArrayList<Word> fittedWords = new ArrayList<Word>();
        int allUsedWords = 0;

        // Stage I

        while (true)
        {
            if (restPhrase.size() == 0)
            {
                break;
            }

            boolean wordIsFound = false;

            for (Word dictWord : story.getDictionary().values())
            {
                int usedWords = fitWordWithPhrase(dictWord, gramCase, restPhrase);

                allUsedWords += usedWords;

                if (usedWords > 0)
                {
                    // case when dict word has no links to objects
                    if (dictWord.getLinkerObjects().size() == 0)
                    {
                        throw new IFML2ParseException(
                                MessageFormat.format("Нигде не вижу {0}.", dictWord.getFormByGramCase(Word.GramCaseEnum.RP)), allUsedWords);
                    }

                    if (fittedWords.contains(dictWord))
                    {
                        String usedPhrase = "";
                        for (String word : phrase.subList(0, allUsedWords - 1))
                        {
                            usedPhrase += " " + word;
                        }

                        throw new IFML2ParseException(
                                MessageFormat.format("Я бы понял фразу, если бы вы сказали \"{0}\"", usedPhrase.trim()), allUsedWords);
                    }

                    fittedWords.add(dictWord);
                    restPhrase = restPhrase.subList(usedWords, restPhrase.size());
                    wordIsFound = true;
                    break;
                }
            }

            if (!wordIsFound)
            {
                if (fittedWords.size() > 0)
                {
                    break;
                }
                else
                {
                    throw new IFML2ParseException(MessageFormat.format("Не знаю слово \"{0}\".", restPhrase.get(0)), 1);
                }
            }
        }

        // Stage II

        ArrayList<IFMLObject> objects = new ArrayList<IFMLObject>();
        objects.addAll(fittedWords.get(0).getLinkerObjects());

        if (fittedWords.size() == 1)
        {
            return new FitObjectWithPhraseResult(objects, allUsedWords);
        }

        for (Word word : fittedWords.subList(1, fittedWords.size()))
        {
            for (Iterator<IFMLObject> iterator = objects.iterator(); iterator.hasNext(); )
            {
                IFMLObject object = iterator.next();
                if (!word.getLinkerObjects().contains(object))
                {
                    if (objects.size() > 1)
                    {
                        iterator.remove();
                    }
                    else
                    {
                        throw new IFML2ParseException(MessageFormat.format("Не знаю такого предмета – \"{0}\"", fittedWords));
                    }
                }
            }
        }

        return new FitObjectWithPhraseResult(objects, allUsedWords);
    }

    private int fitWordWithPhrase(Word word, Word.GramCaseEnum gramCase, List<String> restPhrase)
    {
        String casedDictWord = word.getFormByGramCase(gramCase);
        List<String> casedDictWordArray = Arrays.asList(casedDictWord.split("\\s+"));

        if (casedDictWordArray.size() > 0 && casedDictWordArray.size() <= restPhrase.size())
        {
            int currentWord = 0;

            for (String dictWordPart : casedDictWordArray)
            {
                String phraseWord = restPhrase.get(currentWord);
                if (!dictWordPart.equalsIgnoreCase(phraseWord))
                {
                    return 0; // if at least one word isn't fit then don't accept it
                }
                currentWord++;
            }

            return currentWord;
        }

        return 0;
    }

    private int fitSynonymWithPhrase(String synonym, ArrayList<String> phrase) throws IFML2ParseException
    {
        ArrayList<String> synonymWords = new ArrayList<String>(Arrays.asList(synonym.split("\\s+")));

        int synonymSize = synonymWords.size();

        // take length of shortest (synonym of phrase)
        int minLength = Math.min(synonymSize, phrase.size());

        int usedWordsQty = 0; // fitted words for generating the most suitable exception

        for (int wordIdx = 0; wordIdx <= minLength - 1; wordIdx++)
        {
            String phraseWord = phrase.get(wordIdx);
            String synonymWord = synonymWords.get(wordIdx);
            if (!synonymWord.equalsIgnoreCase(phraseWord))
            {
                throw new IFML2ParseException("Не знаю, что такое \"" + phraseWord + "\".", usedWordsQty);
            }
            usedWordsQty++;
        }

        // check if synonym is not fully used
        if (usedWordsQty < synonymSize)
        {
            throw new IFML2ParseException("Команду не совсем понял, прошу уточнить.", usedWordsQty);
        }

        return usedWordsQty;
    }

    private String makeQuestionsForTemplate(ArrayList<TemplateElement> templateRest) throws IFML2Exception
    {
        String result = "";

        for (TemplateElement templateElement : templateRest)
        {
            if (templateElement instanceof LiteralTemplateElement)
            {
                result += ' ' + ((LiteralTemplateElement) templateElement).getSynonyms().get(0);
            }
            else if (templateElement instanceof ObjectTemplateElement)
            {
                result += ' ' + ((ObjectTemplateElement) templateElement).getGramCase().getQuestionWord();
            }
            else
            {
                throw new IFML2Exception("Системная ошибка: ЭлементШаблона неизвестного типа.");
            }
        }

        return CommonUtils.uppercaseFirstLetter(result.trim()) + "?";
    }

    public class ParseResult
    {
        public Action action = null;
        public List<FormalElement> formalElements = null;

        public ParseResult(Action action, ArrayList<FormalElement> formalElements)
        {
            this.action = action;
            this.formalElements = formalElements;
        }

        public Action getAction()
        {
            return action;
        }

        public List<FormalElement> getFormalElements()
        {
            return formalElements;
        }
    }

    private class TemplateElementFitResult
    {
        public FittedFormalElement fittedFormalElement = null;
        public int usedWordsQty = 0;

        public TemplateElementFitResult(FittedFormalElement fittedSynonym, int usedWordsQty)
        {
            this.fittedFormalElement = fittedSynonym;
            this.usedWordsQty = usedWordsQty;
        }
    }

    private class FittedTemplate
    {
        public Action action = null;
        public ArrayList<FittedFormalElement> fittedFormalElements = new ArrayList<FittedFormalElement>();

        public FittedTemplate(Action action, ArrayList<FittedFormalElement> fittedFormalElements)
        {
            this.action = action;
            this.fittedFormalElements = fittedFormalElements;
        }

        public ArrayList<FittedFormalElement> getFittedFormalElements()
        {
            return fittedFormalElements;
        }
    }

    protected class FittedFormalElement
    {
        String parameter = "";
    }

    private class FittedSynonym extends FittedFormalElement
    {
        String synonym = null;

        public FittedSynonym(String synonym, String parameter)
        {
            this.synonym = synonym;
            this.parameter = parameter;
        }

        @Override
        public String toString()
        {
            return synonym;
        }
    }

    private class FittedObjects extends FittedFormalElement
    {
        final Word.GramCaseEnum gramCase;
        ArrayList<IFMLObject> objects = new ArrayList<IFMLObject>();

        public FittedObjects(ArrayList<IFMLObject> objects, Word.GramCaseEnum gramCase, String parameter)
        {
            this.objects = objects;
            this.gramCase = gramCase;
            this.parameter = parameter;
        }

        public ArrayList<IFMLObject> getObjects()
        {
            return objects;
        }

        @Override
        public String toString()
        {
            return objects.get(0).toString();
        }
    }

    public class FitObjectWithPhraseResult
    {
        private final ArrayList<IFMLObject> objects;
        private final int usedWordsQty;

        public FitObjectWithPhraseResult(ArrayList<IFMLObject> objects, int usedWordsQty)
        {
            this.objects = objects;
            this.usedWordsQty = usedWordsQty;
        }

        public int getUsedWordsQty()
        {
            return usedWordsQty;
        }

        public ArrayList<IFMLObject> getObjects()
        {
            return objects;
        }
    }
}
