# java-buildpack-diagnostics-app
Java Web App for getting diagnostics information about the running application.
Supports heapdump, threaddump and memory information.
Heapdumps get uploaded to Amazon S3.
Currently this solution is designed for Tomcat applications running with the java-buildpack. You could adapt it to other environments.

There is also a separate shell script based solution for [getting heapdumps when an OOM occurs](https://github.com/lhotari/java-buildpack/commits/jbp-diagnostics-oom).

## Installing app to your java-buildpack fork

This application gets deployed in the Tomcat of your java-buildpack. It listens at /jbp-diagnostics when you follow these install instructions. 

1. build war file with ```./gradlew war```
2. copy build/libs/jbp-diagnostics.war to resources/tomcat/webapps/jbp-diagnostics.war in your forked java-buildpack. [See example](https://github.com/lhotari/java-buildpack/tree/jbp-diagnostics/resources/tomcat/webapps).
3. create new branch in your java-buildpack and push it to a new branch so that you can easily reference it in your manifest.yml file with ```https://github.com/lhotari/java-buildpack.git#jbp-diagnostics``` type of syntax (branch name after ```#``` symbol). 

## Testing java-buildpack-diagnostics-app on CloudFoundry

There is a simple Java web app to test this the diagnostics app, see https://github.com/lhotari/hello-jbp-diagnostics

## Requesting Heap dumps

example requesting heap dumps

```curl https://my-app.cfapps.io/jbp-diagnostics/heapdump\?TOKEN\=THE_VALUE_OF_JBPDIAG_TOKEN_ENV```

example response

```
Dumping...
Dumped to /home/vcap/app/.java-buildpack/tomcat/temp/heapdump-*app_name*-*app_instance_id*-2015-03-05-12-24-2970412494384667527.hprof
Dump gzipped and uploaded to S3. Download from https://my-app-dumps.s3.amazonaws.com/heapdump-*app_name*-*app_instance_id*-2015-03-05-12-24-2970412494384667527.hprof.gz?AWSAccessKeyId=xxxx&Expires=1425731184&Signature=xxxx
```

You can then download the dump from s3 with the preauthorized link. It's valid for 48 hours by default.

The dump urls get written to a file on CF, which you can access with
```cf files app-name .heapdumpservlet.dumps```

## Adjusting disk quota for your application

Dump files can be large. cf cli currently lacks support for changing disk quota. There is an [issue about it in cloudfoundry cli](https://github.com/cloudfoundry/cli/issues/102).
See http://blog.troyastle.com/2014/03/setting-disk-quota-for-your-cloud.html for details about how to change it.
The [gradle cloudfoundry plugin](https://github.com/cloudfoundry/cf-java-client/tree/master/cloudfoundry-gradle-plugin#configuring-the-plugin) has diskQuota setting and supports defining it.

CloudFoundry seems to limit disk_quota to 2048 MB . When the disk_quota is set to a higher value, the app won't start and this error message gets returned:
```The app is invalid: disk_quota too much disk requested (must be less than 2048)```
It won't be possible to get heap dumps for JVM apps with large heaps because of this hard limit in disk_quota.

When you have control over the cloud controller config, you could make the limit higher by adjusting the  [```maximum_app_disk_in_mb``` setting in the cloud controller](https://github.com/cloudfoundry/cloud_controller_ng/blob/9b5dbac6c8925b08165200b39f1a3dc9247a41b3/lib/cloud_controller/config.rb#L308). The default value is 2048 . The [```maximum_app_disk_in_mb``` setting](https://github.com/cloudfoundry/cloud_controller_ng/blob/9b5dbac6c8925b08165200b39f1a3dc9247a41b3/config/cloud_controller.yml#L37) in the default ```config/cloud_controller.yml``` file. 

## Requesting a thread dump

example requesting a thread dump

```curl https://my-app.cfapps.io/jbp-diagnostics/threaddump\?TOKEN\=THE_VALUE_OF_JBPDIAG_TOKEN_ENV```

response
```
2015-03-05 18:30:28
Full thread dump OpenJDK 64-Bit Server VM (25.31-b07 mixed mode):

"http-nio-61519-exec-10" #29 daemon prio=5 os_prio=0 tid=0x0000000002fab800 nid=0x43 waiting on condition [0x00007f4cd749b000]
   java.lang.Thread.State: WAITING (parking)
	at sun.misc.Unsafe.park(Native Method)
	- parking to wait for  <0x00000000fe0188b0> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)
	at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)
	at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2039)
	at java.util.concurrent.LinkedBlockingQueue.take(LinkedBlockingQueue.java:442)
	at org.apache.tomcat.util.threads.TaskQueue.take(TaskQueue.java:103)
	at org.apache.tomcat.util.threads.TaskQueue.take(TaskQueue.java:31)
	at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1067)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1127)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
	at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61)
	at java.lang.Thread.run(Thread.java:745)
.
.
.
continues with stack traces from all threads
```

## Getting memory info

example requesting memory info

```curl https://my-app.cfapps.io/jbp-diagnostics/meminfo\?TOKEN\=THE_VALUE_OF_JBPDIAG_TOKEN_ENV```

response
```
JVM memory usage
Heap                                used:   88M committed:  753M max:  753M
Non-Heap                            used:   40M committed:   41M max: 1399M

Memory pools
PS Eden Space                       used:   41M committed:  196M max:  196M
PS Eden Space peak                  used:  196M committed:  196M max:  196M
PS Survivor Space                   used:   32M committed:   32M max:   32M
PS Survivor Space peak              used:   32M committed:   32M max:   32M
PS Old Gen                          used:   13M committed:  524M max:  524M
PS Old Gen peak                     used:   13M committed:  524M max:  524M
Code Cache                          used:    8M committed:    8M max:  245M
Code Cache peak                     used:    8M committed:    8M max:  245M
Metaspace                           used:   28M committed:   29M max:  104M
Metaspace peak                      used:   28M committed:   29M max:  104M
Compressed Class Space              used:    3M committed:    3M max: 1048M
Compressed Class Space peak         used:    3M committed:    3M max: 1048M
```


## Amazon S3 setup

### S3 access keys

The app expects to find S3 access keys and bucket name in these environment variables:
* ```JBPDIAG_AWS_BUCKET``` - the AWS S3 bucket to use
* ```JBPDIAG_AWS_ACCESS_KEY``` - the AWS access key id that has access to the S3 bucket
* ```JBPDIAG_AWS_SECRET_KEY``` - the AWS secret key for the previous

Never use your root AWS key for this purpose. You should create a new user to be used just for this purpose.

Here is a template for manifest.yml
```
    buildpack: https://github.com/lhotari/java-buildpack.git#jbp-diagnostics
    env:
        JBPDIAG_AWS_ACCESS_KEY: AWS_ACCESS_KEY_VALUE
        JBPDIAG_AWS_SECRET_KEY: SECRET_KEY_VALUE
        JBPDIAG_AWS_BUCKET: myapp-jdbdiag-dumps
        JBPDIAG_TOKEN: some_random_token_that_gives_access_to_dumping
```

### Create user in IAM console to be used for S3 in this diagnostics app

1. go to [Amazon IAM Console](https://console.aws.amazon.com/iam/home)
2. click "Users"
3. click "Create new users"
4. enter name for user like "myapp.jdbdiag.user"
5. click "create" and "download credentials". you will get a credentials.csv file that contains the AWS access key id and secret key for the created user.
6. click "close" and "Users" again
7. open the page for the newly created user.
8. copy the "User ARN" from the user. It will be needed for adding access to S3 for this specific user. It's in format like arn:aws:iam::123412341234:user/myapp.jdbdiag.user

### Create S3 bucket and add access to the newly created user

1. go to [Amazon S3 Console](https://console.aws.amazon.com/s3)
2. click "Create Bucket" and go create a new bucket in "US Standard" region. In this example the name is myapp-jbpdiag-dumps . The bucket name has to be globally unique.
3. click "Permissions" on the new bucket
4. click "Add bucket policy"
5. copy this template and edit the "User ARN" in the AWS field and the bucket name in Resource field.
```
{
  "Id": "myapp-jdbdiag-dumps",
  "Statement": [
    {
      "Sid": "allow-putobject-for-myapp.jdbdiag.user",
      "Action": [
        "s3:PutObject"
      ],
      "Effect": "Allow",
      "Resource": "arn:aws:s3:::myapp-jdbdiag-dumps/*",
      "Principal": {
        "AWS": [
          "arn:aws:iam::123412341234:user/myapp.jdbdiag.user"
        ]
      }
    }
  ]
}
```
