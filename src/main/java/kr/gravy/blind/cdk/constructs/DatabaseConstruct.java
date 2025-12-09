package kr.gravy.blind.cdk.constructs;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.elasticache.CfnCacheCluster;
import software.amazon.awscdk.services.elasticache.CfnSubnetGroup;
import software.amazon.awscdk.services.rds.*;
import software.constructs.Construct;

import java.util.List;

/**
 * Database Construct: RDS MySQL + ElastiCache Redis
 */
public class DatabaseConstruct extends Construct {
    private final IVpc vpc;
    private final ISecurityGroup rdsSecurityGroup;
    private final ISecurityGroup redisSecurityGroup;
    private final ISecurityGroup openSearchSecurityGroup;
    private final ISecurityGroup bastionSecurityGroup;
    private final DatabaseInstance rdsInstance;
    private final CfnCacheCluster redisCluster;

    public DatabaseConstruct(final Construct scope, final String id) {
        super(scope, id);

        // 1. VPC 생성
        this.vpc = Vpc.Builder.create(this, "BlindVpc")
                .maxAzs(1)
                .natGateways(0)
                .subnetConfiguration(List.of(
                        SubnetConfiguration.builder()
                                .name("Public")
                                .subnetType(SubnetType.PUBLIC)
                                .cidrMask(24)
                                .build(),
                        SubnetConfiguration.builder()
                                .name("Private")
                                .subnetType(SubnetType.PRIVATE_ISOLATED)
                                .cidrMask(24)
                                .build()
                ))
                .build();

        // VPC Gateway Endpoint: S3
        vpc.addGatewayEndpoint("S3Endpoint", GatewayVpcEndpointOptions.builder()
                .service(GatewayVpcEndpointAwsService.S3)
                .build());

        // 2. Security Groups
        // Bastion Host Security Group (SSH)
        this.bastionSecurityGroup = SecurityGroup.Builder.create(this, "BastionSecurityGroup")
                .vpc(vpc)
                .description("Bastion Host Security Group")
                .allowAllOutbound(true)
                .build();

        bastionSecurityGroup.addIngressRule(
                Peer.anyIpv4(),
                Port.tcp(22),
                "SSH from anywhere"
        );

        // RDS Security Group (MySQL 3306)
        this.rdsSecurityGroup = SecurityGroup.Builder.create(this, "RdsSecurityGroup")
                .vpc(vpc)
                .description("RDS MySQL Security Group")
                .allowAllOutbound(true)
                .build();

        rdsSecurityGroup.addIngressRule(
                bastionSecurityGroup,
                Port.tcp(3306),
                "MySQL from Bastion"
        );

        // Redis Security Group (Redis 6379)
        this.redisSecurityGroup = SecurityGroup.Builder.create(this, "RedisSecurityGroup")
                .vpc(vpc)
                .description("ElasticCache Redis Security Group")
                .allowAllOutbound(true)
                .build();

        redisSecurityGroup.addIngressRule(
                bastionSecurityGroup,
                Port.tcp(6379),
                "Redis from Bastion"
        );

        // OpenSearch Security Group (HTTPS 443)
        this.openSearchSecurityGroup = SecurityGroup.Builder.create(this, "OpenSearchSecurityGroup")
                .vpc(vpc)
                .description("OpenSearch Security Group")
                .allowAllOutbound(true)
                .build();

        openSearchSecurityGroup.addIngressRule(
                bastionSecurityGroup,
                Port.tcp(443),
                "HTTPS from Bastion"
        );

        // 3. RDS MySQL 생성 (db.t4g.micro, Graviton2)
        this.rdsInstance = DatabaseInstance.Builder.create(this, "BlindRds")
                .engine(DatabaseInstanceEngine.mysql(MySqlInstanceEngineProps.builder()
                        .version(MysqlEngineVersion.VER_8_0)
                        .build()))
                .instanceType(software.amazon.awscdk.services.ec2.InstanceType.of(
                        InstanceClass.BURSTABLE4_GRAVITON, InstanceSize.MICRO))
                .vpc(vpc)
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PRIVATE_ISOLATED)
                        .build())
                .securityGroups(List.of(rdsSecurityGroup))
                .databaseName("blind")
                .credentials(Credentials.fromGeneratedSecret("admin"))
                .allocatedStorage(20)
                .maxAllocatedStorage(100)
                .multiAz(false)
                .publiclyAccessible(false)
                .deletionProtection(false)
                .removalPolicy(RemovalPolicy.DESTROY)
                .backupRetention(Duration.days(7))
                .build();

        // 4. ElasticCache Redis 생성 (cache.t3.micro, L1 Construct)
        CfnSubnetGroup redisSubnetGroup = CfnSubnetGroup.Builder.create(this, "RedisSubnetGroup")
                .description("ElasticCache Redis Subnet Group")
                .subnetIds(vpc.selectSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PRIVATE_ISOLATED)
                        .build()).getSubnetIds())
                .cacheSubnetGroupName("blind-redis-subnet-group")
                .build();

        this.redisCluster = CfnCacheCluster.Builder.create(this, "BlindRedis")
                .cacheNodeType("cache.t3.micro")
                .engine("redis")
                .numCacheNodes(1)
                .vpcSecurityGroupIds(List.of(redisSecurityGroup.getSecurityGroupId()))
                .cacheSubnetGroupName(redisSubnetGroup.getCacheSubnetGroupName())
                .build();

        redisCluster.addDependency(redisSubnetGroup);
    }

    public IVpc getVpc() {
        return vpc;
    }

    public ISecurityGroup getRdsSecurityGroup() {
        return rdsSecurityGroup;
    }

    public ISecurityGroup getRedisSecurityGroup() {
        return redisSecurityGroup;
    }

    public ISecurityGroup getOpenSearchSecurityGroup() {
        return openSearchSecurityGroup;
    }

    public ISecurityGroup getBastionSecurityGroup() {
        return bastionSecurityGroup;
    }

    public DatabaseInstance getRdsInstance() {
        return rdsInstance;
    }

    public CfnCacheCluster getRedisCluster() {
        return redisCluster;
    }
}
