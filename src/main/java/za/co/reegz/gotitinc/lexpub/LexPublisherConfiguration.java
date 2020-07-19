package za.co.reegz.gotitinc.lexpub;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Primary configuration class
 * @author Captain Rogers
 */
@Configuration
@ConfigurationProperties(prefix = "application")
@PropertySource("classpath:application.yml")
@Data
public class LexPublisherConfiguration {

    public static final String DIALOG_STATE_ELICIT_INTENT = "ElicitIntent";

    public static final String DIALOG_STATE_FAILED = "Failed,";

    public static final String DIALOG_STATE_FULFILLED = "Fulfilled";

    public static final String DIALOG_STATE_CONFIRM_INTENT = "ConfirmIntent";

    private Lex lex;

    @Data
    @NoArgsConstructor
    public static class Lex {

        private String basePrefix;
    }

}
