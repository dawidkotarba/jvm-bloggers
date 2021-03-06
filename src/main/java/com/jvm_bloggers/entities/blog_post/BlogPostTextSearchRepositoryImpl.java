package com.jvm_bloggers.entities.blog_post;

import java.util.List;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;

@RequiredArgsConstructor
class BlogPostTextSearchRepositoryImpl implements BlogPostTextSearchRepository {

  private final EntityManager entityManager;
  private static final Sort publishedDateSort = new Sort(new SortField("publishedDate", Type.STRING, true));

  @Override
  @SuppressWarnings("unchecked")
  public List<BlogPost> findApprovedPostsByTagOrTitle(String searchPhrase, int page, int pageSize) {
    var fullTextEntityManager = Search.getFullTextEntityManager(entityManager);

    return (List<BlogPost>) fullTextEntityManager
        .createFullTextQuery(createQuery(searchPhrase, fullTextEntityManager), BlogPost.class)
        .setFirstResult(page * pageSize)
        .setMaxResults(pageSize)
        .setSort(publishedDateSort)
        .getResultList();
  }

  @Override
  public int countApprovedPostsByTagOrTitle(String searchPhrase) {
    var fullTextEntityManager = Search.getFullTextEntityManager(entityManager);

    return fullTextEntityManager
        .createFullTextQuery(createQuery(searchPhrase, fullTextEntityManager), BlogPost.class)
        .getResultSize();
  }

  private Query createQuery(String searchPhrase, FullTextEntityManager fullTextEntityManager) {
    return new BooleanQuery.Builder()
        .add(new BooleanClause(keywordQuery(searchPhrase, fullTextEntityManager), Occur.MUST))
        .add(new BooleanClause(approvedQuery(fullTextEntityManager), Occur.MUST))
        .build();
  }

  private Query approvedQuery(FullTextEntityManager fullTextEntityManager) {
    return getQueryBuilder(fullTextEntityManager)
        .keyword()
        .onField("approved")
        .matching(true)
        .createQuery();
  }

  private Query keywordQuery(String searchPhrase, FullTextEntityManager fullTextEntityManager) {
    return getQueryBuilder(fullTextEntityManager)
        .keyword()
        .onField("tags.tag").andField("title")
        .matching(searchPhrase)
        .createQuery();
  }

  private QueryBuilder getQueryBuilder(FullTextEntityManager fullTextEntityManager) {
    return fullTextEntityManager
        .getSearchFactory()
        .buildQueryBuilder()
        .forEntity(BlogPost.class)
        .get();
  }
}
