# java-buildpack-diagnostics-app
Java Web App for doing a heapdump and uploading the file to Amazon S3

## Installing app to your java-buildpack fork

1. build war file with ```./gradlew war```
2. copy build/libs/jbp-diagnostics.war to resources/tomcat/webapps/jbp-diagnostics.war in your forked java-buildpack. [See example|https://github.com/lhotari/java-buildpack/tree/jbp-diagnostics/resources/tomcat/webapps].
3. create new branch in your java-buildpack and push it to a new branch so that you can easily reference it in your manifest.yml file with https://github.com/lhotari/java-buildpack.git#jbp-diagnostics type of syntax. 

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
