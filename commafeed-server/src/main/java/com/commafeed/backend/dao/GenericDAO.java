package com.commafeed.backend.dao;

import java.util.Collection;

import org.hibernate.Session;
import org.hibernate.jpa.SpecHints;

import com.commafeed.backend.model.AbstractModel;
import com.querydsl.core.types.EntityPath;
import com.querydsl.jpa.impl.JPADeleteClause;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAUpdateClause;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class GenericDAO<T extends AbstractModel> {

	private final EntityManager entityManager;
	private final Class<T> entityClass;

	protected JPAQueryFactory query() {
		return new JPAQueryFactory(entityManager);
	}

	protected JPAUpdateClause updateQuery(EntityPath<T> entityPath) {
		return new JPAUpdateClause(entityManager, entityPath);
	}

	protected JPADeleteClause deleteQuery(EntityPath<T> entityPath) {
		return new JPADeleteClause(entityManager, entityPath);
	}

	@SuppressWarnings("deprecation")
	public void saveOrUpdate(T model) {
		entityManager.unwrap(Session.class).saveOrUpdate(model);
	}

	public void saveOrUpdate(Collection<T> models) {
		models.forEach(this::saveOrUpdate);
	}

	public T findById(Long id) {
		return entityManager.find(entityClass, id);
	}

	public void delete(T object) {
		if (object != null) {
			entityManager.remove(object);
		}
	}

	public int delete(Collection<T> objects) {
		objects.forEach(this::delete);
		return objects.size();
	}

	protected void setTimeout(JPAQuery<?> query, int timeoutMs) {
		if (timeoutMs > 0) {
			query.setHint(SpecHints.HINT_SPEC_QUERY_TIMEOUT, timeoutMs);
		}
	}

}
