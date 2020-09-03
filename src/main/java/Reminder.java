import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;


public class Reminder {
    private static Map<String, String> map;

    private static void makeMap(String[] args) {
        map = new HashMap<>();
        for (String arg : args) {
            if (arg.contains("-")) {
                if (arg.contains("=")) {
                    String tmp_key = arg.substring(1, arg.indexOf('='));
                    String tmp_val = arg.substring(arg.indexOf('=') + 1);
                    map.put(tmp_key, tmp_val);
                }
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        makeMap(args);


        if (!map.containsKey("filterNumber") ||
                !map.containsKey("user") ||
                !map.containsKey("password") ||
                !map.containsKey("webhookUrl") ||
                !map.containsKey("timeout") ||
                !map.containsKey("filterName") ||
                !map.containsKey("flagWebhookUrl") ||
                !map.containsKey("jiraUrl")) {
            System.out.println("Error! Some parameters are missing!");
            System.out.println("Script parameters should be:");
            System.out.println("Eg:java -jar <jarName> " +
                    "-jiraUrl=\"https://jira.jira.com\"" +
                    "-filterNumber=66666 -user=User.Userovich " +
                    "-password=StrongPassword -webhookUrl=\"https://hooks.slack.com/....\" " +
                    "-timeout=10000 -filterName=\"MT Filter\" " +
                    "-flagWebhookUrl=\"https://hooks.slack.com/....\"");
            System.exit(0);
        }
        String filterNumber = map.get("filterNumber");
        String user = map.get("user");
        String pass = map.get("password");
        String webhookUrl = map.get("webhookUrl");
        int timeout = Integer.parseInt(map.get("timeout"));
        String filterName = map.get("filterName");
        String flagWebhookUrl = map.get("flagWebhookUrl");
        String jiraUrl = map.get("jiraUrl");

        Set<String> actualIssuesList = new LinkedHashSet<>();
        Set<String> previousIssuesList = new LinkedHashSet<>();
        Set<String> addedIssuesList = new LinkedHashSet<>();
        Set<String> removedIssuesList = new LinkedHashSet<>();
        Set<String> tmp1 = new LinkedHashSet<>();
        Set<String> tmp2 = new LinkedHashSet<>();
        ZoomJira connection = new ZoomJira(jiraUrl, user, pass);
        String request = "filter=" + filterNumber;
        JSONArray jsonIssuesListStarted = connection.getFilterList(request);
        for (int i = 0; i < jsonIssuesListStarted.length(); i++) {
            String issue = jsonIssuesListStarted.getJSONObject(i).getString("key");
            actualIssuesList.add(issue);
        }
        ZoomSlack.sendMessage(":white_check_mark: Monitor for filter " + filterName +
                " started successfully, there are " + actualIssuesList.size() +
                " issues in the filter now!", flagWebhookUrl);
        while (true) {
            tmp1.clear();
            tmp2.clear();
            addedIssuesList.clear();
            removedIssuesList.clear();
            previousIssuesList.clear();
            previousIssuesList.addAll(actualIssuesList);
            actualIssuesList.clear();
            JSONArray jsonIssuesList = connection.getFilterList(request);
            for (int i = 0; i < jsonIssuesList.length(); i++) {
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

            if (!addedIssuesList.isEmpty()) {
                for (String item : addedIssuesList) {
                    JSONObject json_item = connection.getIssueInfo(item);
                    String priority = "None";
                    try {
                        priority = json_item.getJSONObject("fields").getJSONObject("priority").getString("name");
                    } catch (org.json.JSONException exception) {
                        priority = "None";
                    }

                    String summary = json_item.getJSONObject("fields").getString("summary");

                    String message_text = ":heavy_plus_sign: New issue in *<" + jiraUrl +
                            "/issues/?filter=" + filterNumber + "|" + filterName + ">*\n" +
                            "*:black_small_square:Issue: * <" + jiraUrl + "/browse/" + item + "|" + item + ">\n" +
                            "*:black_small_square:Priority: * " + priority + "\n" +
                            "*:black_small_square:Summary: * " + summary + "\n" +
                            "*:black_small_square:Total in filter: * " + connection.getFilterCount(request);
                    ZoomSlack.sendMessage(message_text, webhookUrl);
                }
            }

            if (!removedIssuesList.isEmpty()) {
                for (String item : removedIssuesList) {
                    JSONObject json_item = connection.getIssueInfo(item);
                    String priority = "None";
                    String resolution = "None";
                    try {
                        priority = json_item.getJSONObject("fields").getJSONObject("priority").getString("name");
                    } catch (org.json.JSONException exception) {
                        priority = "None";
                    }
                    String summary = json_item.getJSONObject("fields").getString("summary");
                    try {
                        resolution = json_item.getJSONObject("fields").getJSONObject("resolution").getString("name");
                    } catch (org.json.JSONException exception) {
                        resolution = "None";
                    }

                    String message_text = ":heavy_minus_sign: Issue removed from *<" + jiraUrl + "/issues/?filter=" +
                            filterNumber + "|" + filterName + ">*\n" +
                            "*:black_small_square:Issue: * <" + jiraUrl + "/browse/" + item + "|" + item + ">\n" +
                            "*:black_small_square:Priority: * " + priority + "\n" +
                            "*:black_small_square:Summary: * " + summary + "\n" +
                            "*:black_small_square:Resolution: * " + resolution + "\n" +
                            "*:black_small_square:Total in filter: * " + connection.getFilterCount(request);
                    ZoomSlack.sendMessage(message_text, webhookUrl);
                }
            }

            Thread.sleep(timeout);
        }
    }
}


