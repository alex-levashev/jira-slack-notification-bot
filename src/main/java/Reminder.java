import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;


public class Reminder {
    private static Map<String, String> map;
    public static ServerSocket serverSocketAvailable;

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
                !map.containsKey("feedback") ||
                !map.containsKey("filterName") ||
                !map.containsKey("flagWebhookUrl") ||
                !map.containsKey("jiraUrl")) {
            System.out.println("Error! Some parameters are missing!");
            System.out.println("Script parameters should be:");
            System.out.println("Eg:java -jar <jarName> " +
                    "-user=StrongUsername (Jira username)" +
                    "-password=StrongPassword (Jira password)" +
                    "-webhookUrl=\"https://hooks.slack.com/....\" (Slack webhook URL)" +
                    "-timeout=10000 (Timeout in milliseconds)" +
                    "-feedback=Yes (Inform if the issues is out of the filter)" +
                    "-filterNumber=66666 (Jira filter number)" +
                    "-flagWebhookUrl=\"https://hooks.slack.com/....\" (Webhook for the bot status (Started/Stopped/Unavailable))" +
                    "-jiraUrl=\"https://jira.jira.com\" (Jira URL)" +
                    "-teamLabel=Yes (Optional: If you need a team label in the Slack message)");
            System.exit(0);
        }
        String filterNumber = map.get("filterNumber");
        String user = map.get("user");
        String pass = map.get("password");
        String webhookUrl = map.get("webhookUrl");
        int timeout = Integer.parseInt(map.get("timeout"));
        String feedback = map.get("feedback");
        String filterName = map.get("filterName");
        String flagWebhookUrl = map.get("flagWebhookUrl");
        String jiraUrl = map.get("jiraUrl");
        String teamLabel = map.get("teamLabel");

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
        boolean jiraAvailabilityFlag = true;
        serverSocketAvailable = null;
        while (true) {
            try {
                JSONArray jsonIssuesList = connection.getFilterList(request);
                tmp1.clear();
                tmp2.clear();
                addedIssuesList.clear();
                removedIssuesList.clear();
                previousIssuesList.clear();
                previousIssuesList.addAll(actualIssuesList);
                actualIssuesList.clear();
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
                        String reporter;
                        try {
                            priority = json_item.getJSONObject("fields").getJSONObject("priority").getString("name");
                        } catch (org.json.JSONException exception) {
                            priority = "None";
                        }
                        try {
                            reporter = json_item.getJSONObject("fields").getJSONObject("reporter").getString("displayName");
                        } catch (org.json.JSONException exception) {
                            reporter = "None";
                        }

                        String summary = json_item.getJSONObject("fields").getString("summary");

                        String jiraTeamLabel = "Missing team label";
                        try {
                            for (Object label : json_item.getJSONObject("fields").getJSONArray("labels")) {
                                if (label.toString().toUpperCase().contains("TEAM:")) {
                                    jiraTeamLabel = label.toString().toUpperCase().replace("TEAM:", "");
                                }
                            }
                        } catch (org.json.JSONException exception) {
                            jiraTeamLabel = "Missing team label";
                        }


                        String message_text = ":heavy_plus_sign: New issue in *<" + jiraUrl +
                                "/issues/?filter=" + filterNumber + "|" + filterName + ">*\n" +
                                "*:black_small_square:Issue: * <" + jiraUrl + "/browse/" + item + "|" + item + ">\n" +
                                "*:black_small_square:Priority: * " + priority + "\n";
                                if(teamLabel.equals("Yes")) {
                                    message_text += "*:black_small_square:Team: * " + jiraTeamLabel + "\n";
                    }
                                message_text += "*:black_small_square:Reporter: * " + reporter + "\n" +
                                "*:black_small_square:Summary: * " + summary + "\n" +
                                "*:black_small_square:Total in filter: * " + connection.getFilterCount(request);

                        ZoomSlack.sendMessage(message_text, webhookUrl);
                    }
                }
                if (!removedIssuesList.isEmpty()) {
                    for (String item : removedIssuesList) {
                        JSONObject json_item = connection.getIssueInfo(item);
                        String priority;
                        String resolution;
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
                        if(feedback.equals("Yes")) {
                            ZoomSlack.sendMessage(message_text, webhookUrl);
                        }

                    }
                }
                if(!jiraAvailabilityFlag && serverSocketAvailable != null) {
                    ZoomSlack.sendMessage(":exclamation: Jira server becomes available", flagWebhookUrl);
                    serverSocketAvailable = null;
                }
                jiraAvailabilityFlag = true;
            } catch (Exception e) {
                if(jiraAvailabilityFlag) {
                    jiraAvailabilityFlag = false;
                    try {
                        serverSocketAvailable = new ServerSocket(1044);
                        ZoomSlack.sendMessage(":exclamation: Jira server is unavailable", flagWebhookUrl);
                    } catch (IOException ee) {
                        System.err.println("Another instance reported that Jira is down already!");
                    }
                } else {
                    jiraAvailabilityFlag = false;
                }
            }
            Thread.sleep(timeout);
        }
    }
}


