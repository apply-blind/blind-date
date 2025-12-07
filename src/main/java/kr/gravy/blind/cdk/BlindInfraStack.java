package kr.gravy.blind.cdk;

import kr.gravy.blind.cdk.constructs.ImageOptimizationConstruct;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;

/**
 * Blind 인프라 스택
 * - CloudFront Distribution (CDN)
 * - Lambda Function (이미지 리사이징)
 * - S3 Bucket (원본 이미지 저장소)
 */
public class BlindInfraStack extends Stack {
    public BlindInfraStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // 이미지 최적화 Construct 생성
        ImageOptimizationConstruct imageOptimization =
                new ImageOptimizationConstruct(this, "ImageOptimization");

        // CloudFront 도메인을 Stack Output으로 출력 (application-local.yml에 수동 입력용)
        CfnOutput.Builder.create(this, "CloudFrontDomain")
                .value(imageOptimization.getCloudFrontDomain())
                .description("CloudFront Distribution Domain for Image Optimization")
                .exportName("BlindCloudFrontDomain")
                .build();

        // S3 버킷 이름 출력
        CfnOutput.Builder.create(this, "SourceBucketName")
                .value(imageOptimization.getSourceBucketName())
                .description("S3 Source Bucket for Original Images")
                .exportName("BlindSourceBucket")
                .build();
    }
}
