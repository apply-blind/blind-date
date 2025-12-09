package kr.gravy.blind.cdk.constructs;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.s3.assets.Asset;
import software.constructs.Construct;

import java.util.List;

/**
 * Kafka Construct: EC2 Bastion + Kafka (Docker)
 */
public class KafkaConstruct extends Construct {
    private final Instance kafkaInstance;

    private static final class Config {
        static final String INSTANCE_TYPE = "t3a.medium";
        static final int KAFKA_PORT = 9092;
        static final int ZOOKEEPER_PORT = 2181;
    }

    public KafkaConstruct(final Construct scope, final String id,
                          final IVpc vpc, final ISecurityGroup bastionSecurityGroup) {
        super(scope, id);

        // Kafka Security Group
        ISecurityGroup kafkaSecurityGroup = SecurityGroup.Builder.create(this, "KafkaSecurityGroup")
                .vpc(vpc)
                .description("Kafka Security Group")
                .allowAllOutbound(true)
                .build();

        kafkaSecurityGroup.addIngressRule(
                Peer.ipv4(vpc.getVpcCidrBlock()),
                Port.tcp(Config.KAFKA_PORT),
                "Kafka from VPC"
        );

        // IAM Role (S3 Asset 읽기 권한)
        Role instanceRole = Role.Builder.create(this, "KafkaInstanceRole")
                .assumedBy(new ServicePrincipal("ec2.amazonaws.com"))
                .managedPolicies(List.of(
                        ManagedPolicy.fromAwsManagedPolicyName("AmazonSSMManagedInstanceCore")
                ))
                .build();

        // Kafka 설치 스크립트 Asset
        Asset setupScript = Asset.Builder.create(this, "KafkaSetupScript")
                .path("./cdk/scripts/install-kafka.sh")
                .build();

        // EC2 Instance
        this.kafkaInstance = Instance.Builder.create(this, "KafkaBastionInstance")
                .instanceType(new InstanceType(Config.INSTANCE_TYPE))
                .machineImage(MachineImage.latestAmazonLinux2023())
                .vpc(vpc)
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PUBLIC)
                        .build())
                .securityGroup(bastionSecurityGroup)
                .role(instanceRole)
                .keyName("blind-keypair")  // Phase 3에서 수동 생성 필요
                .userData(UserData.forLinux())
                .build();

        kafkaInstance.addSecurityGroup(kafkaSecurityGroup);

        // UserData 설정
        String localPath = kafkaInstance.getUserData().addS3DownloadCommand(
                S3DownloadOptions.builder()
                        .bucket(setupScript.getBucket())
                        .bucketKey(setupScript.getS3ObjectKey())
                        .build()
        );

        kafkaInstance.getUserData().addExecuteFileCommand(
                ExecuteFileOptions.builder()
                        .filePath(localPath)
                        .arguments(Config.KAFKA_PORT + " " + Config.ZOOKEEPER_PORT)
                        .build()
        );

        setupScript.grantRead(instanceRole);
        kafkaInstance.applyRemovalPolicy(RemovalPolicy.DESTROY);
    }

    public Instance getKafkaInstance() {
        return kafkaInstance;
    }
}
