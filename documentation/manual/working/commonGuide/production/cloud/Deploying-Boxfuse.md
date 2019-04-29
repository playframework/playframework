<!--- Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com> -->
# Deploying to Boxfuse and AWS

Boxfuse lets you deploy your Play applications on AWS. It is based on 3 core principles: Immutable Infrastructure, Minimal Images and Blue/Green deployments.

Boxfuse comes with native Play application support and works by turning your Play dist zip into a minimal VM image that can be deployed unchanged either on VirtualBox or on AWS. This image is generated on-the-fly in seconds and is about 100x smaller than a regular Linux system. It literally only contains your Play application, a JRE and the Linux kernel, reducing the security attack surface to the minimum possible.

Boxfuse works with your AWS account and automatically provisions all the necessary AWS resources your application requires including AMIs, Elastic IPs, Elastic Load Balancers, Security Groups, Auto-Scaling Groups and EC2 instances.

## Prerequisites

Sign up for a free [Boxfuse](https://boxfuse.com) account as well as a free [AWS](https://aws.amazon.com/free) account and [install the Boxfuse command line client](https://boxfuse.com/getstarted/download).

As Boxfuse works with your AWS account, it first needs the necessary permissions to do so. So if you haven't already done so, go to the Boxfuse Console and [connect your AWS account](https://console.boxfuse.com/#/awsAccount) now.

## Build your Application

Package your app using the `sbt dist` command in your project directory.

## Deploy your Application

Every new Boxfuse account comes with 3 environments: `dev`, `test` and `prod`. `dev` is for fast roundtrips locally on VirtualBox environment and `test` and `prod` are on AWS.

So let's deploy the new zip file of your application to the `prod` environment on AWS:

```bash
myapp$ boxfuse run -env=prod

Fusing Image for myapp-1.0.zip ...
Image fused in 00:09.817s (75949 K) -> myuser/myapp:1.0
Pushing myuser/myapp:1.0 ...
Verifying myuser/myapp:1.0 ...
Waiting for AWS to create an AMI for myuser/myapp:1.0 in eu-central-1 (this may take up to 50 seconds) ...
AMI created in 00:34.152s in eu-central-1 -> ami-8b988be7
Creating security group boxsg-myuser-prod-myapp-1.0 ...
Launching t2.micro instance of myuser/myapp:1.0 (ami-8b988be7) in prod (eu-central-1) ...
Instance launched in 00:35.372s -> i-ebea4857
Waiting for AWS to boot Instance i-ebea4857 and Payload to start at http://52.29.129.239/ ...
Payload started in 00:50.316s -> http://52.29.129.239/
Remapping Elastic IP 52.28.107.167 to i-ebea4857 ...
Waiting 15s for AWS to complete Elastic IP Zero Downtime transition ...
Deployment completed successfully. myuser/myapp:1.0 is up and running at http://myapp-myuser.boxfuse.io/
```

You can now visit your app deployed on AWS by running:

```bash
myapp$ boxfuse open -env=prod
```

## Further learning resources

* [Get Started with Boxfuse & Play](https://boxfuse.com/getstarted/play)
* [Deploy Play Framework Scala Apps effortlessly to AWS](https://boxfuse.com/blog/playframework-aws)
* [Boxfuse Play integration reference documentation](https://boxfuse.com/docs/payloads/play)
