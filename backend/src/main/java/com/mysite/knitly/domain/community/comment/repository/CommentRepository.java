package com.mysite.knitly.domain.community.comment.repository;

import com.mysite.knitly.domain.community.comment.entity.Comment;
import com.mysite.knitly.domain.community.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPost(Post post);
}
