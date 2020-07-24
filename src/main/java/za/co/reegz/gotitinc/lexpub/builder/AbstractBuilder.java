package za.co.reegz.gotitinc.lexpub.builder;

import com.amazonaws.services.lexmodelbuilding.AmazonLexModelBuilding;
import com.amazonaws.services.lexmodelbuilding.model.Intent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractBuilder {

    static final String LATEST_VERSION = "$LATEST";

    static Map<String, String> slotTypeMapper = new HashMap<String, String>() {{
        put("@sys.geo-state", "AMAZON.US_STATE");
        put("@sys.date-time", "AMAZON.DATE");
        put("@sys.number", "AMAZON.NUMBER");
    }};

    static Map<String, String> slotNameMapper = new HashMap<>();

    static List<Intent> builtIntents = new ArrayList<>();

    public AmazonLexModelBuilding lexBuilder;

    /**
     * Default constructor.
     * 
     * @param aLexBuilder
     */
    public AbstractBuilder(AmazonLexModelBuilding aLexBuilder) {
        this.lexBuilder = aLexBuilder;
    }


    /**
     * Clean out the name to match Lex's naming standards.
     *
     * @param aOriginalName
     * @return
     */
     String cleanObjectName(String aOriginalName) {
        return aOriginalName.replace("@", "")
                .replace("-", "_")
                .replace(" ", "")
                .replaceAll("\\d","");
    }

    /**
     * Remove all punctuation from the given <code>String</code> object.
     *
     * @param aString
     * @return
     */
    String removePunctuation(String aString) {
         return aString.replaceAll("\\p{Punct}","");
    }

}
