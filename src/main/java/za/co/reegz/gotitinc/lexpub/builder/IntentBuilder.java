package za.co.reegz.gotitinc.lexpub.builder;

import com.amazonaws.services.lexmodelbuilding.AmazonLexModelBuilding;
import com.amazonaws.services.lexmodelbuilding.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This class focuses on building up Lex intents with their associated data.
 *
 */
@Slf4j
public class IntentBuilder extends AbstractBuilder {

    public IntentBuilder(AmazonLexModelBuilding aLexBuilder) {
        super(aLexBuilder);
    }

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
                        getSlots(aJsonNodeIntent.get("parameters"),
                                aJsonNodeIntent.get("followup_utterances"))
                )
                .withSampleUtterances(
                        getUtterances(aJsonNodeIntent.get("utterances"))
                )
                .withFulfillmentActivity(
                        new FulfillmentActivity()
                                .withType(FulfillmentActivityType.CodeHook)
                                .withCodeHook(
                                        new CodeHook().withMessageVersion("1.0")
                                                .withUri("arn:aws:lambda:us-east-1:687787444107:function:test"))
                )
                .withConclusionStatement(
                        new Statement()
                        .withMessages(
                                new Message()
                                        .withContent(aJsonNodeIntent.get("response_template").asText())
                                        .withContentType("PlainText")
                        )
                )
                .withCreateVersion(false); //Set to false so that we don't have to go fishing for the latest version.
        request.setChecksum(getCheckSum(request.getName()));
        PutIntentResult result = lexBuilder.putIntent(request);
        builtIntents.add((new Intent())
                .withIntentName(result.getName())
                .withIntentVersion(result.getVersion()));
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
                                    .concat(
                                            AbstractBuilder.slotNameMapper.get(
                                                    arrayNode.get(i).get("alias").textValue()
                                            ))
                                    .concat("}");
                        } else {
                            str = str.concat(
                                    removePunctuation(arrayNode.get(i).get("text").asText())
                            );
                        }
                    }
                    sampleUtterances.add(str);
                }
            }
        }
        return sampleUtterances;
    }

    /**
     *
     *
     * @param aParametersNode
     * @param aFollowUpNode
     * @return
     */
    private List<Slot> getSlots(JsonNode aParametersNode, JsonNode aFollowUpNode) {
        List<Slot> parameterSlots = new ArrayList<>();
        if (aParametersNode.isArray()) {
            Iterator<JsonNode> iter = aParametersNode.elements();
            int order = 0;
            while (iter.hasNext()) {
                JsonNode currParam = iter.next();
                log.debug(currParam.get("entity_type").asText());

                AbstractBuilder.slotNameMapper.put(
                        currParam.get("id").asText(),
                        cleanObjectName(currParam.get("friendly_name").asText()));

                Slot tmp = new Slot()
                        .withDescription(
                                cleanObjectName(currParam.get("id").asText()))
                        .withName(
                                cleanObjectName(currParam.get("friendly_name").asText()))
                        .withSlotType(
                                AbstractBuilder.slotTypeMapper.get(currParam.get("entity_type").asText()))
                        .withSampleUtterances(
                                getSampleUtterances(cleanObjectName(currParam.get("friendly_name").asText()), aFollowUpNode))
                        .withValueElicitationPrompt(
                                new Prompt().withMessages(
                                        Collections.singletonList(
                                                new Message().withContent("Dummy").withContentType(ContentType.PlainText)))
                                .withMaxAttempts(3)
                        )
                        .withSlotConstraint(currParam.get("mandatory").asBoolean() ? "Required" : "Optional")
                        .withPriority(++order);
                // Don't set a version for built-in slots.
                if (!tmp.getSlotType().startsWith("AMAZON")) {
                    tmp.setSlotTypeVersion("$LATEST");
//                    tmp.setSlotTypeVersion(getLatestSlotVersion(tmp.getSlotType()));
                }
                parameterSlots.add(tmp);
            }
        }
        return parameterSlots;
    }

    /**
     * Supposedly gets the latest numeric version of the slot identified by the given name.
     *
     * @param aSlotTypeName
     * @return
     */
    private String getLatestSlotVersion(String aSlotTypeName) {
        GetSlotTypeVersionsRequest request = new GetSlotTypeVersionsRequest()
                .withName(aSlotTypeName)
                .withMaxResults(20);
        GetSlotTypeVersionsResult result = lexBuilder.getSlotTypeVersions(request);
        return String.valueOf(
                result.getSlotTypes().stream()
                    .mapToInt(x ->
                            x.getVersion().equals("$LATEST") ? 0 : Integer.valueOf(x.getVersion()))
                    .max()
                    .orElse(1));
    }


    private List<String> getSampleUtterances(String aSlotTypeName, JsonNode aJsonNodeSampleUtterances) {
        if (aJsonNodeSampleUtterances.isArray()) {
            ArrayNode sampleUtterArr = (ArrayNode) aJsonNodeSampleUtterances;
            if (sampleUtterArr.size() > 0) {
                Iterator<JsonNode> iter = sampleUtterArr.elements();
                while (iter.hasNext()) {
                    JsonNode currPart = iter.next();
                    log.debug(currPart.toPrettyString());
                }
            }
        }
        return Collections.emptyList();
    }
}
