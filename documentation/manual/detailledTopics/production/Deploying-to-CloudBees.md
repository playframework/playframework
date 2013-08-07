# Deploying to Cloudbees

CloudBees support Play dists natively - with Jenkins and continuous deployment - you can read more about this [here](https://developer.cloudbees.com/bin/view/RUN/Playframework) - yes, you can run it for free.

How to use: 

1. [Signup for CloudBees](https://www.cloudbees.com/signup) (if you don't already have an account) 
2. [Run the ClickStart to setup a working Play app, with repo and Jenkins build service](https://grandcentral.cloudbees.com/?CB_clickstart=https://raw.github.com/CloudBees-community/play2-clickstart/master/clickstart.json)
3. Clone the repo created above - push your changes and then glory is yours !

Alternatively, you may want to just deploy from your Play command shell directly (using SBT), the plugin for this can be found [here](https://github.com/CloudBees-community/sbt-cloudbees-play-plugin).
