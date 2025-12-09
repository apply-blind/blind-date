package kr.gravy.blind.cdk.constructs;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.opensearchservice.*;
import software.constructs.Construct;

import java.util.List;

/**
 * Search Construct: OpenSearch Service (Nori 한글 형태소 분석기)
 */
public class SearchConstruct extends Construct {
    private final Domain openSearchDomain;

    public SearchConstruct(final Construct scope, final String id,
                           final IVpc vpc, final ISecurityGroup securityGroup) {
        super(scope, id);

        // OpenSearch Domain 생성 (t3.small.search, Single-AZ)
        this.openSearchDomain = Domain.Builder.create(this, "BlindOpenSearch")
                .version(EngineVersion.OPENSEARCH_2_11)
                .capacity(CapacityConfig.builder()
                        .dataNodeInstanceType("t3.small.search")
                        .dataNodes(1)
                        .build())
                .ebs(EbsOptions.builder()
                        .volumeSize(10)
                        .volumeType(EbsDeviceVolumeType.GP3)
                        .build())
                .zoneAwareness(ZoneAwarenessConfig.builder()
                        .enabled(false)  // Single-AZ (프리티어)
                        .build())
                .vpc(vpc)
                .vpcSubnets(List.of(SubnetSelection.builder()
                        .subnetType(SubnetType.PRIVATE_ISOLATED)
                        .build()))
                .securityGroups(List.of(securityGroup))
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }

    public Domain getOpenSearchDomain() {
        return openSearchDomain;
    }
}
