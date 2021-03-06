# Backup ZoneMinder recordings to Amazon S3.

This spring boot application was created to provide a backup service to S3, of ZoneMinder recordings. It has been 
built to run with a single instance, ZoneMinder setup, connecting and modifying the DB used by ZoneMinder. 

`Compatible with ZM V 1.32.`

This app works by pulling events from the ZoneMinder Events table, creating 2 new fields in that table, backedUp and 
backUpCompleted the first keeps track of events put into the backup process, while the latter tracks when the 
specific event has been fully uploaded to S3. 

We use the following flow in our system: 

1. Every 5 minutes pull list of events that have not started the backup process.
2. Move zip up those events and place in a temp dir. 
3. Add a message to our Queue that a zip is ready for upload to s3.
4. When a message is pulled off the Queue, we start the upload process to S3, as well as place the message on a 
processing Queue.
5. When upload completes we delete the temp zip package and update the db to completed.
6. We have a process that short polls the processing queue to: 
    a. Leave on processing Queue. 
    b. If timeout expires, move back to message queue to restart upload process as we assumed it failed.
    c. Upload has completed successfully - drop from processing queue.
7. Lastly we have a process that checks for completed events older than 30 days, and deletes those from our local ZM,
 using ZM API. 

I hacked this together on my day off, but feel free to use this in any way you like. Open an Issue if you find a bug.
 Feel free to create a PR if you want to contribute.

### Required Dependencies

* Running-single node ZoneMinder setup. (I run ZM on a Centos7 machine)
* ZoneMinder DB credentials. 
* Redis DB installed on the instance and ready for connections.
* S3 Bucket with IAM credentials.
* Java 8 installed.
* create a temp directory in your base path.

### Required JVM Params to Run

* `-Dzm.username=backup`
* `-Dzm.password=PASSWORD`
* `-Dzm.url=https://hostname.com/zm/`
* `-Dcloud.aws.credentials.accessKey=ACCESS_KEY`
* `-Dcloud.aws.credentials.secretKey=SECRET_KEY`
* `-Dcloud.aws.s3.bucket=BUCKET_NAME`
* `-Dspring.datasource.username=ZM_USERNAME`
* `-Dspring.datasource.password=ZM_PASSWORD`

### Packaging and Running 

*  `mvn package` to package up your jar into the target folder
*  `sudo nohup java
    -Dzm.username=backup
    -Dzm.password=PASSWORD
    -Dzm.url=https://hostname.com/zm/
    -Dcloud.aws.credentials.accessKey=ACCESS_KEY
    -Dcloud.aws.credentials.secretKey=SECRET_KEY
    -Dcloud.aws.s3.bucket=BUCKET_NAME
    -Dspring.datasource.username=ZM_USERNAME
    -Dspring.datasource.password=ZM_PASSWORD
    -Dserver.port=8081
    -Devents.path=/home/zoneminder/
    -jar s3-backup-0.0.1-SNAPSHOT.jar > /dev/null 2>&1&`
