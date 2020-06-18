import com.slack.api.webhook.WebhookResponse;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import java.io.IOException;

public class ZoomSlack {
    public static void sendMessage (String text, String webhookUrl) throws IOException {
        BasicConfigurator.configure();
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
        com.slack.api.Slack slack = com.slack.api.Slack.getInstance();

        String payload = "{\"text\":\"" + text + "\"}";

        WebhookResponse response = slack.send(webhookUrl, payload);
    }

}
