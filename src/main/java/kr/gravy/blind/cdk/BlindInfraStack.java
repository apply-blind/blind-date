package kr.gravy.blind.cdk;

import kr.gravy.blind.cdk.constructs.DatabaseConstruct;
import kr.gravy.blind.cdk.constructs.ImageOptimizationConstruct;
import kr.gravy.blind.cdk.constructs.KafkaConstruct;
import kr.gravy.blind.cdk.constructs.SearchConstruct;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;

/**
 * Blind 인프라 스택
 */
public class BlindInfraStack extends Stack {
    public BlindInfraStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // 1. Database Construct (VPC, RDS, Redis)
        DatabaseConstruct database = new DatabaseConstruct(this, "Database");

        // 2. OpenSearch Construct
        SearchConstruct search = new SearchConstruct(this, "Search",
                database.getVpc(),
                database.getOpenSearchSecurityGroup());

        // 3. Kafka Construct (EC2 Bastion + Kafka)
        KafkaConstruct kafka = new KafkaConstruct(this, "Kafka",
                database.getVpc(),
                database.getBastionSecurityGroup());

        // 4. 이미지 최적화 Construct (기존)
        ImageOptimizationConstruct imageOptimization =
                new ImageOptimizationConstruct(this, "ImageOptimization");

        // Stack Outputs
        CfnOutput.Builder.create(this, "VpcId")
                .value(database.getVpc().getVpcId())
                .description("VPC ID")
                .exportName("BlindVpcId")
                .build();

        CfnOutput.Builder.create(this, "RdsEndpoint")
                .value(database.getRdsInstance().getDbInstanceEndpointAddress())
                .description("RDS MySQL Endpoint")
                .exportName("BlindRdsEndpoint")
                .build();

        CfnOutput.Builder.create(this, "RdsSecretArn")
                .value(database.getRdsInstance().getSecret().getSecretArn())
                .description("RDS Credentials Secret ARN")
                .exportName("BlindRdsSecretArn")
                .build();

        CfnOutput.Builder.create(this, "RedisEndpoint")
                .value(database.getRedisCluster().getAttrRedisEndpointAddress())
                .description("ElastiCache Redis Endpoint")
                .exportName("BlindRedisEndpoint")
                .build();

        CfnOutput.Builder.create(this, "RedisPort")
                .value(database.getRedisCluster().getAttrRedisEndpointPort())
                .description("ElastiCache Redis Port")
                .exportName("BlindRedisPort")
                .build();

        CfnOutput.Builder.create(this, "OpenSearchEndpoint")
                .value(search.getOpenSearchDomain().getDomainEndpoint())
                .description("OpenSearch Domain Endpoint")
                .exportName("BlindOpenSearchEndpoint")
                .build();

        CfnOutput.Builder.create(this, "KafkaBastionPublicIp")
                .value(kafka.getKafkaInstance().getInstancePublicIp())
                .description("Kafka Bastion Public IP")
                .exportName("BlindKafkaBastionIp")
                .build();

        CfnOutput.Builder.create(this, "KafkaPrivateIp")
                .value(kafka.getKafkaInstance().getInstancePrivateIp())
                .description("Kafka Private IP")
                .exportName("BlindKafkaPrivateIp")
                .build();

        CfnOutput.Builder.create(this, "ImageCloudFrontDomain")
                .value(imageOptimization.getCloudFrontDomain())
                .description("CloudFront Distribution Domain for Image Optimization")
                .exportName("BlindImageCloudFrontDomain")
                .build();

        CfnOutput.Builder.create(this, "SourceBucketName")
                .value(imageOptimization.getSourceBucketName())
                .description("S3 Source Bucket for Original Images")
                .exportName("BlindSourceBucket")
                .build();
    }
}
