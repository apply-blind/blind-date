package kr.gravy.blind.infrastructure.kafka;

public final class KafkaConstants {

    private KafkaConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    public static final String POST_INDEXING_TOPIC = "post-indexing";

    public static final String POST_INDEXING_DLT = POST_INDEXING_TOPIC + "-dlt";
}
