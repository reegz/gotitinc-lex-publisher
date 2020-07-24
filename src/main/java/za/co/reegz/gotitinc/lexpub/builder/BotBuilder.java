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
        log.debug("Building bot.");
        PutBotRequest request = new PutBotRequest()
                .withName(
                        cleanObjectName(aJsonNode.get("name").asText()))
                .withDescription(
                        aJsonNode.get("description").asText())
                .withCreateVersion(false)
                .withChildDirected(false)
                .withLocale(Locale.EnUS)
                .withIntents(builtIntents)
                .withChecksum(
                        getCheckSum(cleanObjectName(aJsonNode.get("name").asText())))
                .withAbortStatement(
                        new Statement().withMessages(
                                Collections.singletonList(
                                        new Message()
                                                .withContentType(ContentType.PlainText)
                                                .withContent(aJsonNode.get("abort_statement").asText())
                )));
        lexBuilder.putBot(request);
    }

    /**
     * Get the checksum of the slot, if it exists.
     *
     * @param aBotName
     * @return
     */
    private String getCheckSum(String aBotName) {
        log.debug("Getting Bot checksum.");
        try {
            GetBotRequest request = new GetBotRequest();
            request.setName(aBotName);
            request.setVersionOrAlias(LATEST_VERSION);
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
