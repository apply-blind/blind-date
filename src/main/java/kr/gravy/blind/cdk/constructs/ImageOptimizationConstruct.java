package kr.gravy.blind.cdk.constructs;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Fn;
import software.amazon.awscdk.services.cloudfront.BehaviorOptions;
import software.amazon.awscdk.services.cloudfront.Distribution;
import software.amazon.awscdk.services.cloudfront.OriginRequestPolicy;
import software.amazon.awscdk.services.cloudfront.ViewerProtocolPolicy;
import software.amazon.awscdk.services.cloudfront.origins.HttpOrigin;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.IBucket;
import software.constructs.Construct;

import java.util.Map;

/**
 * 1. S3 Bucket: 원본 이미지 저장
 * 2. Lambda Function: 동적 이미지 리사이징 (Sharp 라이브러리)
 * 3. CloudFront Distribution: CDN 캐싱 + Lambda 호출
 */
public class ImageOptimizationConstruct extends Construct {
    private final IBucket sourceBucket;
    private final Function imageResizerFunction;
    private final Distribution distribution;

    public ImageOptimizationConstruct(final Construct scope, final String id) {
        super(scope, id);

        // 1. 기존 S3 Bucket 참조 (blind-dev)
        this.sourceBucket = Bucket.fromBucketName(this, "SourceBucket", "blind-dev");

        // 2. Lambda Function 생성 (이미지 리사이징 - Node.js + Sharp)
        this.imageResizerFunction = Function.Builder.create(this, "ImageResizerFunction")
                .runtime(Runtime.NODEJS_20_X)
                .handler("index.handler")
                .code(Code.fromAsset("lambda/image-resizer"))
                .memorySize(1536)
                .timeout(Duration.seconds(30))
                .environment(Map.of(
                        "SOURCE_BUCKET", sourceBucket.getBucketName(),
                        "ALLOWED_WIDTHS", "200,800,1920"
                ))
                .build();

        sourceBucket.grantRead(imageResizerFunction);

        // Lambda Function URL 생성 (CloudFront Origin용)
        FunctionUrl functionUrl = imageResizerFunction.addFunctionUrl(FunctionUrlOptions.builder()
                .authType(FunctionUrlAuthType.NONE)
                .build());

        // 3. CloudFront Distribution 생성 (CDN + Lambda Origin)
        String functionDomain = Fn.select(2, Fn.split("/", functionUrl.getUrl()));

        this.distribution = Distribution.Builder.create(this, "ImageDistribution")
                .defaultBehavior(BehaviorOptions.builder()
                        .origin(HttpOrigin.Builder.create(functionDomain)
                                .protocolPolicy(software.amazon.awscdk.services.cloudfront.OriginProtocolPolicy.HTTPS_ONLY)
                                .build())
                        .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                        .originRequestPolicy(OriginRequestPolicy.ALL_VIEWER_EXCEPT_HOST_HEADER)
                        .build())
                .comment("Blind Image Optimization CDN (CloudFront + Lambda)")
                .build();
    }


    /**
     * CloudFront 도메인 반환 (예: d111111abcdef8.cloudfront.net)
     */
    public String getCloudFrontDomain() {
        return distribution.getDistributionDomainName();
    }

    /**
     * S3 버킷 이름 반환
     */
    public String getSourceBucketName() {
        return sourceBucket.getBucketName();
    }
}
