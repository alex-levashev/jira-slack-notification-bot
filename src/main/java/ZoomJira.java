import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

public class ZoomJira {
    private String jiraUrl;
    private String jiraUser;
    private String jiraPassword;
    private String issueKey;

    public ZoomJira(String jiraUrl, String jiraUser, String jiraPassword) throws MalformedURLException {
        this.jiraUrl = jiraUrl;
        this.jiraUser = jiraUser;
        this.jiraPassword = jiraPassword;
    }

    private JSONArray getJsonArrayFromURL(String requestUrl) throws IOException {
        URL url = new URL(this.jiraUrl + requestUrl);
        String user = this.jiraUser;
        String password = this.jiraPassword;
        URLConnection uc = url.openConnection();

        uc.setRequestProperty("X-Requested-With", "Curl");

        String userpass = user + ":" + password;
        String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
        uc.setRequestProperty("Authorization", basicAuth);

        InputStreamReader inputStreamReader = new InputStreamReader(uc.getInputStream());
        Scanner s = new Scanner(inputStreamReader).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";
        JSONArray resultJSON = new JSONArray(result);
        return resultJSON;
    }

    private JSONObject getJsonFromURL(String requestUrl) throws IOException {
        URL url = new URL(this.jiraUrl + requestUrl);
        String user = this.jiraUser;
        String password = this.jiraPassword;
        URLConnection uc = url.openConnection();

        uc.setRequestProperty("X-Requested-With", "Curl");

        String userpass = user + ":" + password;
        String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
        uc.setRequestProperty("Authorization", basicAuth);

        InputStreamReader inputStreamReader = new InputStreamReader(uc.getInputStream());
        Scanner s = new Scanner(inputStreamReader).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";
        JSONObject resultJSON = new JSONObject(result);
        return resultJSON;
    }

    private String getProjectId(String projectKey) throws IOException {
        String user = this.jiraUser;
        String password = this.jiraPassword;
        String requestUrl = "/rest/api/2/project";

        String resultOut = "Project not found!";

        JSONArray resultJSON = this.getJsonArrayFromURL(requestUrl);

        for (int i = 0; i < resultJSON.length(); ++i) {
            JSONObject prg = resultJSON.getJSONObject(i);
            if(prg.getString("key").equals(projectKey)) {
                resultOut = prg.getString("id");
            }
        }
        return resultOut;
    }

    public int getFilterCount(String filter) throws IOException {
        String encodedQuery = URLEncoder.encode(filter, StandardCharsets.UTF_8.toString());
        String requestUrl = "/rest/api/2/search?jql=" + encodedQuery + "&maxResults=1";
        return Integer.parseInt(this.getJsonFromURL(requestUrl).get("total").toString());
    }

    public JSONArray getFilterList(String filter) throws IOException {
        String encodedQuery = URLEncoder.encode(filter, StandardCharsets.UTF_8.toString());
        String requestUrl = "/rest/api/2/search?jql=" + encodedQuery + "&maxResults=5000";
        JSONArray resultArray = (this.getJsonFromURL(requestUrl)).getJSONArray("issues");
        return resultArray;
    }

    public JSONObject getIssueInfo(String issueKey) throws IOException {
        String requestUrl = "/rest/api/2/issue/" + issueKey;
        JSONObject resultJson = this.getJsonFromURL(requestUrl);
        return resultJson;
    }
}
