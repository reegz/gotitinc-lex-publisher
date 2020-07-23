package za.co.reegz.gotitinc.lexpub.builder;

import com.amazonaws.services.lexmodelbuilding.AmazonLexModelBuilding;
import com.amazonaws.services.lexmodelbuilding.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * This class focuses on building up Lex slots with their associated data.
 *
 */
@Slf4j
public class SlotBuilder extends AbstractBuilder {

    public SlotBuilder(AmazonLexModelBuilding aLexBuilder) {
        super(aLexBuilder);
    }

    /**
     * A <code>JsonNode</code> object containing the custom entities that need to be converted into custom Lex slot types.
     *
     * @param aJsonNode
     */
    public void buildCustomSlotTypes(JsonNode aJsonNode) {
        aJsonNode.iterator().forEachRemaining(
                x -> createSlotType(x)
        );
    }

    /**
     * Converts a single custom entity record into an AWS Lex Slot.
     *
     * @param aJsonNodeSlot
     */
    private void createSlotType(JsonNode aJsonNodeSlot) {
        log.debug(aJsonNodeSlot.toPrettyString());
        PutSlotTypeRequest request = new PutSlotTypeRequest()
                .withName(cleanObjectName(
                        aJsonNodeSlot.get("table").asText()
                                .concat("_")
                                .concat(aJsonNodeSlot.get("column").asText())
                        )
                )
                /* Lex doesn't allow numbers in the slot name so swapping around. */
                .withDescription(cleanObjectName(aJsonNodeSlot.get("entity_type").asText()))
                .withEnumerationValues(getEnumerationValues(aJsonNodeSlot.get("dictionary")))
                .withValueSelectionStrategy(SlotValueSelectionStrategy.TOP_RESOLUTION)
                .withCreateVersion(false); //Set to false so that we don't have to go fishing for the latest version.
        AbstractBuilder.slotTypeMapper.put(
                aJsonNodeSlot.get("entity_type").asText(),
                request.getName());
        request.setChecksum(getCheckSum(request.getName()));
        lexBuilder.putSlotType(request);
    }

    /**
     * Get the checksum of the slot, if it exists.
     *
     * @param aSlotName
     * @return
     */
    private String getCheckSum(String aSlotName) {
        try {
            GetSlotTypeRequest request = new GetSlotTypeRequest();
            request.setName(aSlotName);
            request.setVersion("$LATEST");
            GetSlotTypeResult response = lexBuilder.getSlotType(request);
            if (response != null && response.getName() != null) {
                return response.getChecksum();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Convert the Json Array into a list of <code>EnumerationValue</code> objects to send through to Lex for this slot.
     *
     * @param aJsonNodeEnum
     * @return
     */
    private List<EnumerationValue> getEnumerationValues(JsonNode aJsonNodeEnum) {
        List<EnumerationValue> enumValues = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> iter = aJsonNodeEnum.fields();
        while (iter.hasNext()) {
            Map.Entry<String, JsonNode> currNode = iter.next();
            EnumerationValue tmp = new EnumerationValue();
            tmp.setValue(currNode.getKey());
            tmp.setSynonyms(Collections.emptyList());
            if (currNode.getValue().isArray()) {
                ArrayNode arrayNode = (ArrayNode) currNode.getValue();
                for (int i=0; i<arrayNode.size(); i++) {
                    tmp.getSynonyms().add(arrayNode.get(i).textValue());
                }
            }
            enumValues.add(tmp);
        }
        return enumValues;
    }
}
