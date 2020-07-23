package za.co.reegz.gotitinc.lexpub.builder;

import com.amazonaws.services.lexmodelbuilding.AmazonLexModelBuilding;
import com.amazonaws.services.lexmodelbuilding.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
public class BotBuilder extends AbstractBuilder {

    public BotBuilder(AmazonLexModelBuilding aLexBuilder) {
        super(aLexBuilder);
    }

    /**
     *
     *
     * @param aJsonNode
     */
    public void buildBot(JsonNode aJsonNode) {
        PutBotRequest request = new PutBotRequest()
                .withName("Test")
                .withDescription(aJsonNode.get("description").asText())
                .withCreateVersion(false)
                .withChildDirected(false)
                .withLocale(Locale.EnUS)
                .withIntents(builtIntents)
                .withChecksum(getCheckSum("Test"))
                .withAbortStatement(new Statement().withMessages(
                        Collections.singletonList(
                                new Message().withContentType(ContentType.PlainText).withContent("Uh oh! Looks like I don't understand that..."))
                ));
        PutBotResult result = lexBuilder.putBot(request);
    }

    /**
     * Get the checksum of the slot, if it exists.
     *
     * @param aBotName
     * @return
     */
    private String getCheckSum(String aBotName) {
        try {
            GetBotRequest request = new GetBotRequest();
            request.setName(aBotName);
            request.setVersionOrAlias("$LATEST");
            GetBotResult response = lexBuilder.getBot(request);
            if (response != null && response.getName() != null) {
                return response.getChecksum();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
