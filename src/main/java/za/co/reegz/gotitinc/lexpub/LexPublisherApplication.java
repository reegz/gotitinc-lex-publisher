package za.co.reegz.gotitinc.lexpub;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lexmodelbuilding.AmazonLexModelBuilding;
import com.amazonaws.services.lexmodelbuilding.AmazonLexModelBuildingClientBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Base application class.
 *
 * @author Captain Rogers
 */
@Component
@NoArgsConstructor
@Slf4j
@SpringBootApplication
@SuppressWarnings("unused")
public class LexPublisherApplication {

    private SlotBuilder slotBuilder;

    private IntentBuilder intentBuilder;

    static Map<String, String> slotTypeMapper = new HashMap<String, String>() {{
        put("@sys.geo-state", "AMAZON.US_STATE");
        put("@sys.date-time", "AMAZON.DATE");
        put("@sys.number", "AMAZON.NUMBER");
    }};

    static Map<String, String> slotNameMapper = new HashMap<>();

    /**
     * Converts the custom Indie file into an AWS Lex bot using the AWS Lex Model Building API.
     *
     */
    private void convertJsonToLex(String aAccessKey, String aSecretKey, String aRegion, String aFileLocation) {
        log.debug("aAccessKey: {}, \naSecretKey: {}\naRegion: {}", aAccessKey, aSecretKey, aRegion);
        log.debug("Building bot from source file - {}", aFileLocation);
        try {
            /* Create the model builder using credentials from command line.  */
            AmazonLexModelBuilding lexModelBuilder = AmazonLexModelBuildingClientBuilder.standard()
                    .withCredentials(
                            new AWSStaticCredentialsProvider(
                                    new BasicAWSCredentials(aAccessKey, aSecretKey)))
                    .withRegion(Regions.fromName(aRegion))
                    .build();

            slotBuilder = new SlotBuilder(lexModelBuilder);
            intentBuilder = new IntentBuilder(lexModelBuilder);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new FileReader(aFileLocation));
//            log.debug(root.toPrettyString());
            JsonNode customEntities = root.get("custom_entities");
            JsonNode customIntents = root.get("intents");
            slotBuilder.buildCustomSlotTypes(customEntities);
            intentBuilder.buildIntents(customIntents);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        new LexPublisherApplication().convertJsonToLex(args[0], args[1], args[2], args[3]);
    }

}
