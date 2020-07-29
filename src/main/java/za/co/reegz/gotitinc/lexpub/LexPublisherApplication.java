package za.co.reegz.gotitinc.lexpub;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.AddPermissionRequest;
import com.amazonaws.services.lambda.model.RemovePermissionRequest;
import com.amazonaws.services.lexmodelbuilding.AmazonLexModelBuilding;
import com.amazonaws.services.lexmodelbuilding.AmazonLexModelBuildingClientBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import za.co.reegz.gotitinc.lexpub.builder.BotBuilder;
import za.co.reegz.gotitinc.lexpub.builder.IntentBuilder;
import za.co.reegz.gotitinc.lexpub.builder.SlotBuilder;

import java.io.FileReader;

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

    private BotBuilder botBuilder;

    /**
     * Converts the custom Indie file into an AWS Lex bot using the AWS Lex Model Building API.
     *
     */
    private void convertJsonToLex(String aAccessKey, String aSecretKey, String aRegion,
                                  String aFileLocation, String aFulfillmentLambdaARN) {
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

            AWSLambda awsLambda = AWSLambdaClientBuilder.standard().withCredentials(
                            new AWSStaticCredentialsProvider(
                                    new BasicAWSCredentials(aAccessKey, aSecretKey)))
                    .withRegion(Regions.fromName(aRegion))
                    .build();

            slotBuilder = new SlotBuilder(lexModelBuilder);
            intentBuilder = new IntentBuilder(lexModelBuilder);
            botBuilder = new BotBuilder(lexModelBuilder);

            log.debug("Fetching JSON input source.");
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new FileReader(aFileLocation));
            setWebhookPermissions(aFulfillmentLambdaARN, awsLambda);
            JsonNode customEntities = root.get("custom_entities");
            JsonNode customIntents = root.get("intents");
            slotBuilder.buildCustomSlotTypes(customEntities);
            intentBuilder.buildIntents(customIntents, aFulfillmentLambdaARN);
            botBuilder.buildBot(root);
            log.debug("Process completed.");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


    /**
     * Ensure the correct permissions are set on the Lambda.
     *
     * @param aFunctionName
     * @param awsLambda
     */
    private void setWebhookPermissions(String aFunctionName, AWSLambda awsLambda) {
        log.debug("Setting permissions for fulfillment web hook.");
        try {
            RemovePermissionRequest remove = new RemovePermissionRequest()
                    .withFunctionName(aFunctionName)
                    .withStatementId("chatbot-fulfillment");
            awsLambda.removePermission(remove);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        try {
            AddPermissionRequest request = new AddPermissionRequest()
                    .withAction("lambda:InvokeFunction")
                    .withFunctionName(aFunctionName)
                    .withStatementId("chatbot-fulfillment")
                    .withPrincipal("lex.amazonaws.com");
            awsLambda.addPermission(request);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Ye good old main method!
     *
     * @param args
     */
    public static void main(String[] args) {
        String accesskey = args[0];
        String secretKey = args[1];
        String awsRegion = args[2];
        String fileLocation = args[3];
        String fulfillmentLambdaARN = args[4];
        new LexPublisherApplication().convertJsonToLex(accesskey, secretKey, awsRegion, fileLocation, fulfillmentLambdaARN);
    }

}
