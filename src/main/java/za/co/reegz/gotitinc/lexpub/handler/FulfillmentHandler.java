package za.co.reegz.gotitinc.lexpub.handler;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.LexEvent;
import com.amazonaws.services.lexmodelbuilding.AmazonLexModelBuilding;
import com.amazonaws.services.lexmodelbuilding.AmazonLexModelBuildingClientBuilder;
import com.amazonaws.services.lexmodelbuilding.model.GetIntentRequest;
import com.amazonaws.services.lexmodelbuilding.model.GetIntentResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FulfillmentHandler implements RequestHandler<LexEvent, String> {
//public class FulfillmentHandler implements RequestHandler<LexEvent, String> {

    static AmazonLexModelBuilding lexModelBuilder;

    static {
        lexModelBuilder = AmazonLexModelBuildingClientBuilder.standard()
                .withCredentials(
                        new AWSStaticCredentialsProvider(
                                new BasicAWSCredentials("aAccessKey", "aSecretKey")))
                .withRegion(Regions.fromName("aRegion"))
                .build();
    }

    @Override
//    public String handleRequest(Object input, Context context) {
    public String handleRequest(LexEvent input, Context context) {
        log.debug(input.toString());
        log.debug("handleRequest(String input, Context context)");
        if (input.getInvocationSource().equals("FulfillmentCodeHook")) {
            GetIntentResult intent = lexModelBuilder.getIntent(
                    new GetIntentRequest()
                            .withName(input.getCurrentIntent().getName())
                            .withVersion("$LATEST"));

            String resultTemplate = intent.getConclusionStatement().getMessages().get(0).getContent();


        }

        context.getLogger().log("Input: " + input);
        return "Hello World - " + input;
    }


}
