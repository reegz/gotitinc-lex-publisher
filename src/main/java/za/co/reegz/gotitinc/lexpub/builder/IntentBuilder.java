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
import java.util.stream.Collectors;

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
    public void buildIntents(JsonNode aJsonNode, String aFulfillmentLambdaARN) {
        aJsonNode.iterator().forEachRemaining(
                x -> mapSlotNames(x)
        );
        aJsonNode.iterator().forEachRemaining(
                x -> createIntent(x, aFulfillmentLambdaARN)
        );
    }

    /**
     * Create the static map of slot names for use in later processing.
     *
     * @param jsonNode
     */
    private void mapSlotNames(JsonNode jsonNode) {
        log.debug("Mapping slot names.");
        if (jsonNode.get("parameters").isArray()) {
            Iterator<JsonNode> iter = jsonNode.get("parameters").elements();
            while (iter.hasNext()) {
                JsonNode currParam = iter.next();
                log.debug(currParam.get("entity_type").asText());

                AbstractBuilder.slotNameMapper.put(
                        currParam.get("id").asText(),
                        cleanObjectName(currParam.get("friendly_name").asText()));
            }
        }
    }

    /**
     * Converts a single intent record into an AWS Lex Intent.
     *
     * @param aJsonNodeIntent
     */
    private void createIntent(JsonNode aJsonNodeIntent, String aFulfillmentLambdaARN) {
        log.debug("Creating intents.");
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
                                                .withUri(aFulfillmentLambdaARN)))
                .withConclusionStatement(
                        new Statement()
                        .withMessages(
                                new Message()
                                        .withContent(aJsonNodeIntent.get("response_template").asText())
                                        .withContentType("PlainText")
                        )
                )
                .withCreateVersion(false); //Set to false so that we don't have to go fishing for the latest version.
//                .withCreateVersion(true);
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
            request.setVersion(LATEST_VERSION);
//            request.setVersion(getLatestSlotVersion(aSlotName));
            GetIntentResult response = lexBuilder.getIntent(request);
            if (response != null && response.getName() != null) {
                return response.getChecksum();
            }
        } catch (Exception e) {
//            log.error(e.getMessage(), e);
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
        log.debug("Building utterances.");
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
        log.debug("Building slots.");
        List<Slot> parameterSlots = new ArrayList<>();
        if (aParametersNode.isArray()) {
            Iterator<JsonNode> iter = aParametersNode.elements();
            int order = 0;
            while (iter.hasNext()) {
                JsonNode currParam = iter.next();
                log.debug(currParam.get("entity_type").asText());

                /*AbstractBuilder.slotNameMapper.put(
                        currParam.get("id").asText(),
                        cleanObjectName(currParam.get("friendly_name").asText()));*/

                Slot tmp = new Slot()
                        .withDescription(
                                cleanObjectName(currParam.get("id").asText()))
                        .withName(
                                cleanObjectName(currParam.get("friendly_name").asText()))
                        .withSlotType(
                                AbstractBuilder.slotTypeMapper.get(currParam.get("entity_type").asText()))
                        .withSampleUtterances(
                                getSlotSampleUtterances(currParam.get("id").asText(), aFollowUpNode))
                        .withValueElicitationPrompt(
                                new Prompt().withMessages(
                                        Collections.singletonList(
                                                // This value should come from the file at a later stage.
                                                new Message().withContent("Dummy").withContentType(ContentType.PlainText)))
                                .withMaxAttempts(3)
                        )
                        .withSlotConstraint(currParam.get("mandatory").asBoolean() ? "Required" : "Optional")
                        .withPriority(++order);
                // Don't set a version for built-in slots.
                if (!tmp.getSlotType().startsWith("AMAZON")) {
                    tmp.setSlotTypeVersion(LATEST_VERSION);
//                    tmp.setSlotTypeVersion(getLatestSlotVersion(cleanObjectName(currParam.get("friendly_name").asText())));
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
        log.debug("Getting latest slot version for slot with name {}", aSlotTypeName);
        GetSlotTypeVersionsRequest request = new GetSlotTypeVersionsRequest()
                .withName(aSlotTypeName)
                .withMaxResults(50);
        GetSlotTypeVersionsResult result = lexBuilder.getSlotTypeVersions(request);
        return String.valueOf(
                result.getSlotTypes().stream()
                    .mapToInt(x ->
                            x.getVersion().equals(LATEST_VERSION) ? 0 : Integer.valueOf(x.getVersion()))
                    .max()
                    .orElse(1));
    }


    /**
     * Build up a <code>List</code> of sample utterances with which to invoke the intent.
     *
     * @param aJsonNodeSampleUtterances
     * @return
     */
    private List<String> getSlotSampleUtterances(String aParamName, JsonNode aJsonNodeSampleUtterances) {
        log.debug("Looking for slot prompts for slot with name {}", aParamName);
        List<String> sampleUtterances = new ArrayList<>();
        if (aJsonNodeSampleUtterances.isArray()) {
            ArrayNode sampleUtterArr = (ArrayNode) aJsonNodeSampleUtterances;
            if (sampleUtterArr.size() > 0) {
                Iterator<JsonNode> iter = sampleUtterArr.elements();
                while (iter.hasNext()) {
                    JsonNode currPart = iter.next();
                    if (currPart.get("parts").isArray()) {
                        ArrayNode currPartArr = (ArrayNode) currPart.get("parts");
                        String sampleSlotUtterance = "";
                        boolean shouldAdd = false;
                        Iterator<JsonNode> iter2 = currPartArr.elements();
                        while (iter2.hasNext()) {
                            JsonNode currStr = iter2.next();
                            if (currStr.has("alias")) {
                                sampleSlotUtterance = sampleSlotUtterance.concat("{")
                                        .concat(
                                                AbstractBuilder.slotNameMapper.get(
                                                        currStr.get("alias").textValue()
                                                ))
                                        .concat("}");
                            } else {
                                sampleSlotUtterance = sampleSlotUtterance.concat(currStr.get("text").asText());
                            }

                            if (currStr.has("alias")
                                    && currStr.get("alias").asText().equals(aParamName) ) {
                                shouldAdd = true;
                            }
                        }
                        if (shouldAdd) {
                            log.debug("Adding slot utterance for slot {}", aParamName);
                            sampleUtterances.add(sampleSlotUtterance);
                        }
                    }
                }
            }
        }
        return sampleUtterances.stream().distinct().collect(Collectors.toList());
    }
}
