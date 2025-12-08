package kr.gravy.blind.board.repository;

import kr.gravy.blind.board.entity.PostDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;


public interface PostSearchRepository extends ElasticsearchRepository<PostDocument, String> {
}
