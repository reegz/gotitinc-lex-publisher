package za.co.reegz.gotitinc.lexpub;

import com.amazonaws.services.lexmodelbuilding.AmazonLexModelBuilding;
import com.amazonaws.services.lexmodelbuilding.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class focuses on building up Lex intents with their associated data.
 *
 */
@Slf4j
public class IntentBuilder {

    public IntentBuilder(AmazonLexModelBuilding aLexBuilder) {
        this.lexBuilder = aLexBuilder;
    }

    AmazonLexModelBuilding lexBuilder;

    /**
     * A <code>JsonNode</code> object containing the intents that need to be built in Lex.
     *
     * @param aJsonNode
     */
    public void buildIntents(JsonNode aJsonNode) {
        aJsonNode.iterator().forEachRemaining(
                x -> createIntent(x)
        );
    }

    /**
     * Converts a single intent record into an AWS Lex Intent.
     *
     * @param aJsonNodeIntent
     */
    private void createIntent(JsonNode aJsonNodeIntent) {
        PutIntentRequest request = new PutIntentRequest()
                .withName(cleanObjectName(aJsonNodeIntent.get("id").asText()))
                .withSlots(
                        getSlots(aJsonNodeIntent.get("parameters"))
                )
                .withSampleUtterances(
                        getUtterances(aJsonNodeIntent.get("utterances"))
                )
                // Call Webhook here
                .withFulfillmentActivity(
                        new FulfillmentActivity()
                                .withType("ReturnIntent")
//                        .withCodeHook()
                )
                .withConclusionStatement(
                        new Statement()
                        .withMessages(
                                new Message()
                                        .withContent(aJsonNodeIntent.get("response_template").asText())
                                        .withContentType("PlainText")
                        )
                )
                .withCreateVersion(true);
        request.setChecksum(getCheckSum(request.getName()));
        lexBuilder.putIntent(request);
    }

    /**
     * Get the checksum of the slot, if it exists.
     *
     * @param aSlotName
     * @return
     */
    private String getCheckSum(String aSlotName) {
        try {
            GetIntentRequest request = new GetIntentRequest();
            request.setName(aSlotName);
            request.setVersion("$LATEST");
            GetIntentResult response = lexBuilder.getIntent(request);
            if (response != null && response.getName() != null) {
                return response.getChecksum();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Clean out the name to match Lex's naming standards.
     *
     * @param aOriginalName
     * @return
     */
    private String cleanObjectName(String aOriginalName) {
        return aOriginalName.replace("@", "")
                .replace("-", "_")
                .replace(" ", "")
                .replaceAll("\\d","");
    }

    /**
     * Convert the Json Array into a list of <code>String</code>'s which represent the sample utterances for the intent.
     *
     * @param aJsonNodeUtterance
     * @return
     */
    private List<String> getUtterances(JsonNode aJsonNodeUtterance) {
        List<String> sampleUtterances = new ArrayList<>();
        if (aJsonNodeUtterance.isArray()) {
            Iterator<JsonNode> iter = aJsonNodeUtterance.elements();
            while (iter.hasNext()) {
                JsonNode currPart = iter.next();
                if (currPart.get("parts").isArray()) {
                    String str = "";
                    ArrayNode arrayNode = (ArrayNode) currPart.get("parts");
                    for (int i=0; i<arrayNode.size(); i++) {
                        if (arrayNode.get(i).get("alias") != null
                            && arrayNode.get(i).get("entity_type") != null) {
                            str = str.concat("{")
//                                    .concat(arrayNode.get(i).get("alias").textValue())

                                    .concat(
                                            LexPublisherApplication.slotNameMapper.get(
                                                    arrayNode.get(i).get("alias").textValue()
                                            ))

//                                    .concat(LexPublisherApplication.slotTypeMapper.get(
//                                            arrayNode.get(i).get("entity_type").textValue()))
                                    .concat("}");
                        } else {
                            str = str.concat(arrayNode.get(i).get("text").asText());
                        }
                    }
                    sampleUtterances.add(str);
                }
            }
        }
        return sampleUtterances;
    }

    private List<Slot> getSlots(JsonNode aParametersNode) {
        List<Slot> parameterSlots = new ArrayList<>();
        if (aParametersNode.isArray()) {
            Iterator<JsonNode> iter = aParametersNode.elements();
            int order = 0;
            while (iter.hasNext()) {
                JsonNode currParam = iter.next();
                log.debug(currParam.get("entity_type").asText());

                LexPublisherApplication.slotNameMapper.put(
                        currParam.get("id").asText(),
                        cleanObjectName(currParam.get("friendly_name").asText()));

                Slot tmp = new Slot()
                        .withDescription(cleanObjectName(currParam.get("id").asText()))
                        .withName(cleanObjectName(currParam.get("friendly_name").asText()))
                        .withSlotType(
                                LexPublisherApplication.slotTypeMapper.get(
                                        currParam.get("entity_type").asText()
                                )
                        )
                        .withSlotConstraint(
                                currParam.get("mandatory").asBoolean() ? "Required" : "Optional")
                        .withPriority(++order);
                // Don't set a version for built-in slots.
                if (!tmp.getSlotType().startsWith("AMAZON")) {
//                    tmp.setSlotTypeVersion("$LATEST");
                    tmp.setSlotTypeVersion(getLatestSlotVersion(tmp.getSlotType()));
                }
                parameterSlots.add(tmp);
            }
        }
        return parameterSlots;
    }

    private String getLatestSlotVersion(String aSlotType) {
        GetSlotTypeVersionsRequest request = new GetSlotTypeVersionsRequest()
                .withName(aSlotType)
                .withMaxResults(20);
        GetSlotTypeVersionsResult result = lexBuilder.getSlotTypeVersions(request);
        return String.valueOf(
                result.getSlotTypes().stream()
                    .mapToInt(x ->
                            x.getVersion().equals("$LATEST") ? 0 : Integer.valueOf(x.getVersion()))
                    .max()
                    .orElse(1));
    }
}
