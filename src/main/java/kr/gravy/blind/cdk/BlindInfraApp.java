package kr.gravy.blind.cdk;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

/**
 * AWS CDK 진입점
 * 실행 방법: cdk deploy (cdk.json이 "./gradlew run" 실행)
 */
public class BlindInfraApp {
    private static final String REGION = "ap-northeast-2";

    public static void main(final String[] args) {
        App app = new App();

        Environment env = Environment.builder()
                .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                .region(REGION)
                .build();

        new BlindInfraStack(app, "BlindInfraStack", StackProps.builder()
                .env(env)
                .description("Blind 이미지 최적화 인프라 (CloudFront + Lambda + S3)")
                .build());

        app.synth();
    }
}
