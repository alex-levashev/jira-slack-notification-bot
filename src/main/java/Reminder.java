import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;


public class Reminder {
    public static void main(String[] args) throws IOException, InterruptedException {
        if(args.length < 0) {
            System.out.println("Error! Some parameters are missing!");
            System.out.println("Script parameters should be:");
            System.out.println("Jira filter number, Jira username, Jira password, Slack webhook URL devided by space");
            System.out.println("Eg: 66666 User.Userovich StrongPassword https://hooks.slack.com/.... 10000 \"MT Filter\"");
            System.exit(0);
        }
        String filterNumber = args[0];
        String user = args[1];
        String pass = args[2];
        String webhookUrl = args[3];
        int timeout = Integer.parseInt(args[4]);
        String filterName = args[5];
        String jiraUrl = "https://jira.zoomint.com";

        Set<String> actualIssuesList = new LinkedHashSet<>();
        Set<String> previousIssuesList = new LinkedHashSet<>();
        Set<String> addedIssuesList = new LinkedHashSet<>();
        Set<String> removedIssuesList = new LinkedHashSet<>();
        Set<String> tmp1 = new LinkedHashSet<>();
        Set<String> tmp2 = new LinkedHashSet<>();
        ZoomJira connection = new ZoomJira(jiraUrl, user, pass);
        String request = "filter=" + filterNumber;
        while(true) {
            tmp1.clear();
            tmp2.clear();
            addedIssuesList.clear();
            removedIssuesList.clear();
            previousIssuesList.clear();
            previousIssuesList.addAll(actualIssuesList);
            actualIssuesList.clear();
            JSONArray jsonIssuesList = connection.getFilterList(request);
            for(int i = 0; i< jsonIssuesList.length(); i++) {
                String issue = jsonIssuesList.getJSONObject(i).getString("key");
                    actualIssuesList.add(issue);
            }

            // GETTING REMOVED ISSUES
            // TODO FIX THIS PART OF THE CODE
            tmp1.addAll(actualIssuesList);
            tmp2.addAll(previousIssuesList);

            previousIssuesList.removeAll(actualIssuesList);
            removedIssuesList.addAll(previousIssuesList);

            actualIssuesList.clear();
            previousIssuesList.clear();
            actualIssuesList.addAll(tmp1);
            previousIssuesList.addAll(tmp2);
            tmp1.clear();
            tmp2.clear();

            //GETTING ADDED ISSUES
            // TODO FIX THIS PART OF THE CODE
            tmp1.addAll(actualIssuesList);
            tmp2.addAll(previousIssuesList);

            actualIssuesList.removeAll(previousIssuesList);
            addedIssuesList.addAll(actualIssuesList);

            actualIssuesList.clear();
            previousIssuesList.clear();
            actualIssuesList.addAll(tmp1);
            previousIssuesList.addAll(tmp2);
            tmp1.clear();
            tmp2.clear();

            if(!addedIssuesList.isEmpty()) {
                for(String item : addedIssuesList) {
                    JSONObject json_item = connection.getIssueInfo(item);
                    String priority = json_item.getJSONObject("fields").getJSONObject("priority").getString("name");
                    String summary = json_item.getJSONObject("fields").getString("summary");

                    String message_text = ":heavy_plus_sign: New issue in *<" + jiraUrl + "/issues/?filter=" + filterNumber + "|" + filterName + ">*\n" +
                            "*:black_small_square:Issue: * <" + jiraUrl + "/browse/" + item + "|" + item + ">\n" +
                            "*:black_small_square:Priority: * " + priority + "\n" +
                            "*:black_small_square:Summary: * " + summary + "\n" +
                            "*:black_small_square:Total in filter: * " + connection.getFilterCount(request);
                    ZoomSlack.sendMessage(message_text, webhookUrl);
                }
            }

            if(!removedIssuesList.isEmpty()) {
                for(String item : removedIssuesList) {
                    JSONObject json_item = connection.getIssueInfo(item);
                    String priority = json_item.getJSONObject("fields").getJSONObject("priority").getString("name");
                    String summary = json_item.getJSONObject("fields").getString("summary");

                    String message_text = ":heavy_minus_sign: Issue removed from *<" + jiraUrl + "/issues/?filter=" + filterNumber + "|" + filterName + ">*\n" +
                            "*:black_small_square:Issue: * <" + jiraUrl + "/browse/" + item + "|" + item + ">\n" +
                            "*:black_small_square:Priority: * " + priority + "\n" +
                            "*:black_small_square:Summary: * " + summary + "\n" +
                            "*:black_small_square:Total in filter: * " + connection.getFilterCount(request);
                    ZoomSlack.sendMessage(message_text, webhookUrl);
                }
            }

            Thread.sleep(timeout);
        }
    }
}


