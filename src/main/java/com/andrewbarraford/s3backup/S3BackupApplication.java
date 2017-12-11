package com.andrewbarraford.s3backup;

import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import org.apache.hadoop.fs.s3a.BasicAWSCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableScheduling
@SpringBootApplication
@EnableAutoConfiguration
public class S3BackupApplication {

	public static void main(String[] args) {
		SpringApplication.run(S3BackupApplication.class, args);
	}

	@Bean
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);
		executor.setMaxPoolSize(10);
		executor.setQueueCapacity(25);
		return executor;
	}

	@Bean(name = "transferManager")
	public TransferManager transferManagerInit(
			@Value("${cloud.aws.credentials.accessKey}") final String key,
			@Value("${cloud.aws.credentials.secretKey}") final String secret
	){
		return TransferManagerBuilder.standard().withS3Client(
				AmazonS3ClientBuilder.standard()
						.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(key, secret)))
						.build())
				.build();
	}

}
