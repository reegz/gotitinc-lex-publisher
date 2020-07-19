package za.co.reegz.gotitinc.lexpub.service;

import com.amazonaws.services.lexmodelbuilding.AmazonLexModelBuilding;
import com.amazonaws.services.lexmodelbuilding.model.Locale;
import com.amazonaws.services.lexmodelbuilding.model.PutBotRequest;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.co.reegz.gotitinc.lexpub.LexPublisherConfiguration;

/**
 * Convenience class that handles the logic of dealing with calls to the AWS LEX sdk.
 *
 * Methods include sending text and voice to the LEX service.
 *
 * @author Axel Foley
 */
@Service
@Slf4j
@NoArgsConstructor
@SuppressWarnings("all")
public class AWSLexBuilderService {

    @Autowired
    private LexPublisherConfiguration configuration;

    @Autowired
    private AmazonLexModelBuilding builderService;

    public void buildBot() {
        PutBotRequest req = new PutBotRequest() {{
            setName("Test");
            setChildDirected(Boolean.FALSE);
            setLocale(Locale.EnUS);
        }};
        builderService.putBot(req);
    }

    public void createIntent() {}

    public void createSlot() {}

}
