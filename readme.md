# Slack Notification Bot
Notifies you in a Slack channel if any issue added/removed from filter with a message.

Required parameters are:
1. Jira base URL - should be changed in the code before compilation if needed
2. Jira Filter number
3. Username
4. Password
5. Slack webhook URL
6. Check timeout in ms
7. Filter name to display in Slack message

Started as jar (for example):

java -jar SlackNotificationBot.jar 12345 User Password "http://webhook.slack.com/...." 1000 "Test Name" 

Could be deployed as a systemd service (taken from https://stackoverflow.com/questions/12102270/run-java-jar-file-on-a-server-as-background-process).

1. Find your user defined services mine was at /usr/lib/systemd/system/
2. Create a text file with your favorite text editor name it whatever_you_want.service
3. Put following Template to the file whatever_you_want.service

```bash
[Unit]
Description=webserver Daemon
[Service]
ExecStart=/usr/bin/java -jar /web/server.jar
User=user
[Install]
WantedBy=multi-user.target
```

4. Run your service as super user

````
$ systemctl start whatever_you_want.service # starts the service
$ systemctl enable whatever_you_want.service # auto starts the service
$ systemctl disable whatever_you_want.service # stops autostart
$ systemctl stop whatever_you_want.service # stops the service
$ systemctl restart whatever_you_want.service # restarts the service